package com.latamautos.kernel.scala.rest_api

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, StandardRoute, _}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.CorsSettings
import com.latamautos.kernel.scala.util.NotificationManager.GetNotifications
import com.latamautos.kernel.scala.util.TimeoutParameters.Implicit._
import com.latamautos.kernel.scala.util._
import com.typesafe.scalalogging.Logger
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import spray.json.DefaultJsonProtocol

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.Try


abstract class BaseRestController(implicit system: ActorSystem, materializer: ActorMaterializer)
  extends Config with SprayJsonSupport with DefaultJsonProtocol {
  var streamPath: String = ""
  var pathPrefixName: String = ""
  final val EMPTY = ""
  var siteConfigUtil: SiteConfigUtil = new SiteConfigUtil

  final val NOTIFICATION_ACTOR_PREFIX = "notification-"

  implicit def str2Int(string: String): Int = Integer.valueOf(string).intValue

  implicit val formats = Serialization.formats(NoTypeHints)

  def getNotification: ActorRef = system.actorOf(NotificationManager.props, NOTIFICATION_ACTOR_PREFIX + UUID.randomUUID().toString)

  protected def serviceName: String = this.getClass.getSimpleName

  protected def log: LoggingAdapter = Logging(system, serviceName)

  protected lazy val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  def responseComplete[T](data: T): StandardRoute = StandardRoute(_.complete(""))

  def completePaged(getRouteFromPageRequest: (PageRequest, ActorRef, Query) => Any): Route = {
    val routeParameters: Route = parameters('page ?, 'size ?, 'sort.*, 'namespace ?, 'appid ?) {
      (pageParam, sizeParam, sortListParam, namespaceParam, appidParam) =>
        ctx: RequestContext => {
          logger.debug(":::::::::::::::::Http Request:::::::::::::::::" + ctx.request.toString())
          //TODO performance issue
          val namespaceAlias: String = getParameterValue(namespaceParam, "")
          val namespace: String = if (siteConfigUtil.getSite(namespaceAlias).isDefined) siteConfigUtil.getSite(namespaceAlias).get else EMPTY

          val pageRequest = PageRequest(ctx.request.getUri().host().address(), namespace,
            getParameterValue(appidParam, ""), getParameterValue(pageParam, page),
            getParameterValue(sizeParam, size), sortListParam)
          try {
            val notification = getNotification
            val data = getRouteFromPageRequest(pageRequest, notification, ctx.request.uri.query())
            StandardRoute(
              _.complete(createResponse(data, pageRequest, notification, namespace))
            ).apply(ctx)
          } catch {
            case CodeException(messageCode: MessageCode) =>
              StandardRoute(
                _.complete( generateHttpResponseWithContentType(messageCode.code ,
                            write(new BaseResponse(List(), pageRequest.getPageNumber, pageRequest.getPageSize,
                            messageCode.code, 0, 0, Notification(messageCode.message)))) )
              ).apply(ctx)
            case e: Throwable =>
              logger.error(s"::::::::::::::::::::::::::::::::::: INTERNAL ERROR = $e :::::::::::::::::::::::::::::::::::")
              println(e.printStackTrace())
              StandardRoute(
                _.complete(generateHttpResponseWithContentType(MessageCodeConst.SERVER_ERROR,
                           write(new BaseResponse(List(), pageRequest.getPageNumber, pageRequest.getPageSize,
                           MessageCodeConst.SERVER_ERROR, 0, 0, Notification("Internal error")))) )
              ).apply(ctx)
          }
        }
    }
    routeParameters
  }

  def createResponse(data: Any, pageRequest: PageRequest, notification: ActorRef, namespace: String): Future[HttpResponse] = {
    val POINT_CHARACTER = "."
    val UNDER_SCORE_SEPARATOR = "_"
    val namespaceReplaced = namespace.replace(POINT_CHARACTER, UNDER_SCORE_SEPARATOR)
    data match {
      case data: Future[Any] =>
        data.flatMap(value => {
          val futureNotification: Future[Any] = notification ? GetNotifications
          futureNotification.asInstanceOf[Future[Notification]].map((notificationValues) => {
            var responseCode = MessageCodeConst.SUCCESS
            if (notificationValues.hasError) responseCode = notificationValues.getFirstCode
            val paths: Seq[PathResponse] = value match {
              case Some(identityResponse: Identity) =>
                val path = pathPrefixName + "/" + streamPath + "/" + identityResponse.id
                val realTimePath = namespaceReplaced + "/" + path
                Seq(PathResponse(path, realTimePath))
              case response: Seq[IdentityResponse] =>
                response.map(iR => {
                  val path = pathPrefixName + "/" + streamPath + "/" + iR.id
                  val realTimePath = namespaceReplaced + "/" + path
                  PathResponse(path, realTimePath)
                })
              case _ => Seq.empty[PathResponse]
            }
            generateHttpResponseWithContentType(responseCode, write(new BaseResponse(value, pageRequest.getPageNumber,
              pageRequest.getPageSize, responseCode, 0, 0, notificationValues, paths)) )
          })
        })
      case data: Page[_] =>
        Future {
          generateHttpResponseWithContentType(MessageCodeConst.SUCCESS,
            write(new BaseResponse(data.getContent, pageRequest.getPageNumber, pageRequest.getPageSize,
              MessageCodeConst.SUCCESS, data.getTotalElements.toInt, data.getTotalPages, Notification())))
        }
      case data: Iterable[_] =>
        Future {
           generateHttpResponseWithContentType(MessageCodeConst.SUCCESS,
             write(new BaseResponse(data, 0, 0, MessageCodeConst.SUCCESS, data.size, 0, Notification())))
        }

      case data =>
        Future {
          generateHttpResponseWithContentType(MessageCodeConst.SUCCESS, write(new BaseResponse(data, pageRequest.getPageNumber,
            pageRequest.getPageSize, MessageCodeConst.SUCCESS, 0, 0, Notification())))
        }
    }
  }

  def generateHttpResponseWithContentType(responseCode:Int, responseJsonString: String, contentType: ContentType.NonBinary = ContentTypes.`application/json`): HttpResponse ={
    HttpResponse(status = responseCode, entity = HttpEntity(contentType, responseJsonString))
  }

  def getParameterValue[T](givenValue: Option[String], defaultValue: T)(implicit converter: String => T): T = {
    givenValue match {
      case Some(value: String) => Try {
        val a: T = value
        a
      } getOrElse {
        defaultValue
      }
      case None => defaultValue
    }
  }

  val settings = CorsSettings.defaultSettings.copy(allowedMethods = Seq(GET, POST, HEAD, DELETE, PUT, OPTIONS))

}


