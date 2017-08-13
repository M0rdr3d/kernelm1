package com.latamautos.kernel.scala.rest_api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.CorsDirectives._
import spray.json.RootJsonFormat

import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.{universe => runtimeReflection}

/**
  * Created by xavier on 5/26/16.
  */
abstract class BaseController[T](implicit system: ActorSystem, materializer: ActorMaterializer) extends BaseRestController with GenericController [T]{

  object ControllerCategoryEnum extends Enumeration {
    type ControllerCategoryEnum = Value
    val NORMAL, CUSTOM = Value
  }

  implicit val unMarshaller: RootJsonFormat[T]

  implicit val mirror = runtimeReflection.runtimeMirror(getClass.getClassLoader)

  val receiverBodyParameter: Boolean = true

  def getAllResources(pageRequest: PageRequest, queryString: Query): Any

  def getAllResourcesByParentId(parentId: String, pageRequest: PageRequest, queryString: Query): Any

  def getOptionResource: Any = ""

  def postResource(data: T, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any

  def postResourceByParentId(id: String, data: T, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any

  def updateResourceById(id: String, data: T, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any

  def postResource(notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any

  def postResourceByParentId(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any

  def updateResourceById(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any

  def deleteResourceById(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any

  def getResourceById(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any

  override def setPathPrefix(pathPrefixName: String) = this.pathPrefixName = pathPrefixName

  override def setStreamPath(streamPath: String) = this.streamPath = streamPath

  override def apiControllerType: Type = getBaseClassType(this.getClass)

  def getBaseClassType[T](baseClass: Class[T]): runtimeReflection.Type = {
    val runtimeMirror = runtimeReflection.runtimeMirror(baseClass.getClassLoader)
    runtimeMirror.classSymbol(baseClass).toType
  }

  def +:(): Seq[BaseController[_]] = {
    Seq(this)
  }

  def +:(controller: BaseController[_]): Seq[BaseController[_]] = {
    Seq(this, controller)
  }

  override def withChildren(controller: GenericController[_]): GenericController[_] = {
    controller.parent = Some(this)
    children = Seq(controller)
    this
  }

  override def withChildren(controllers: Seq[GenericController[_]]): GenericController[_] = {
    controllers.foreach(controller => controller.parent = Some(this))
    children = controllers
    this
  }

  def customRoutes: Option[Route]

  def getAllResourcesRoute: Route = {

    parent match {
      case Some(controller: BaseController[_]) =>

        var postRouteWithChildBaseController = path(controller.streamPath / Segment / streamPath) {
          parentId: String => (post & entity(as[T])) {
            itemParameter => completePaged {
              (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => postResourceByParentId(parentId, itemParameter, notification, pageRequest, queryString)
            }
          }
        }
        if (!receiverBodyParameter) {
          postRouteWithChildBaseController = path(controller.streamPath / Segment / streamPath) {
            parentId: String => post {
              completePaged {
                (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => postResourceByParentId(parentId, notification, pageRequest, queryString)
              }
            }
          }
        }

        pathPrefix(controller.pathPrefixName) {
          path(controller.streamPath / Segment / streamPath) {
            parentId: String => get {
              completePaged {
                (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => getAllResourcesByParentId(parentId, pageRequest, queryString)
              }
            }
          } ~ postRouteWithChildBaseController ~ path(controller.streamPath / Segment / streamPath) {
            parentId: String => options {
              completePaged {
                (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => getOptionResource
              }
            }
          }
        }

      case None =>
        var postRoute = path(streamPath) {
          (post & entity(as[T])) {
            itemParameter =>
              completePaged {
                (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => {
                  postResource(itemParameter, notification, pageRequest, queryString)
                }
              }
          }
        }

        if (!receiverBodyParameter) {
          postRoute = path(streamPath) {
            post {
              completePaged {
                (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => {
                  postResource(notification, pageRequest, queryString)
                }
              }
            }
          }
        }
        pathPrefix(pathPrefixName) {
          path(streamPath) {
            get {
              completePaged { (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => getAllResources(pageRequest, queryString) }
            }
          } ~ postRoute ~ path(streamPath) {
            options {
              completePaged {
                (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => getOptionResource
              }
            }
          }
        }
    }
  }

  override def getRoutesNormal: Route ={
    var putRoute = path(streamPath / Segment) {
      id: String => (put & entity(as[T])) {
        itemParameter =>
          completePaged { (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => updateResourceById(id, itemParameter, notification, pageRequest, queryString)
          }
      }
    }
    if (!receiverBodyParameter) {
      putRoute = path(streamPath / Segment) {
        id: String => put {
          completePaged {
            (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => updateResourceById(id, notification, pageRequest, queryString)
          }
        }
      }
    }

    val routes =
      pathPrefix(pathPrefixName) {
        customRoutes.getOrElse(getAllResourcesRoute)
      } ~ getAllResourcesRoute ~
        pathPrefix(pathPrefixName) {
          putRoute ~ path(streamPath / Segment) {
            id: String => delete {
              completePaged { (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => deleteResourceById(id, notification, pageRequest, queryString) }
            }
          } ~
            path(streamPath / Segment) {
              id: String => get {
                completePaged {
                  (pageRequest: PageRequest, notification: ActorRef, queryString: Query) => getResourceById(id, notification, pageRequest, queryString)
                }
              }

            }
        }


    if (children.isEmpty) {
      routes
    }
    else {
      routes ~ children.map(_.getRoutesNormal).reduce((r1: Route, r2: Route) => {
        r1 ~ r2
      })
    }
  }


  override def getRoutes: Route = {
      if(enableCors){
        cors(settings){
          getRoutesNormal
        }
      }else{
        getRoutesNormal
      }
  }

}