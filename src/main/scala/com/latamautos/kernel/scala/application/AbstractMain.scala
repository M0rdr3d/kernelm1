package com.latamautos.kernel.scala.application

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import com.github.swagger.akka.{HasActorSystem, SwaggerHttpService}
import com.google.inject.Injector
import com.latamautos.kernel.scala.domain.Aggregate.IdentifiedEvent
import com.latamautos.kernel.scala.domain.{AggregateDeletedLifecycleEvent, AggregateInitializedLifecycleEvent, ApplicationScheduler}
import com.latamautos.kernel.scala.rest_api._
import com.typesafe.scalalogging.Logger
import io.swagger.models.Scheme
import org.slf4j.LoggerFactory

import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.runtime.universe.Type

/**
  * Created by xavier on 5/30/16.
  */
trait AbstractMain extends Config {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  import system.dispatcher


  val microservicePrefixPath: String
  val useJournal: Boolean = true
  var routeMap: MutableMap[String, Option[String]] = MutableMap()
  var connectionContext: ConnectionContext = _
  var mutableApiTypes = Seq.empty[Type]
  protected lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName))

  def httpInterface: String

  def httpPort: Int

  def enableHttps: Boolean


  val commandHandlers: Seq[Class[_ <: ApplicationService[_]]]
  val scheduledServices: Seq[ApplicationScheduler] = Seq()
  val injector: Injector
  val paths: Seq[Path]

  val eventHandlers: Map[Class[_ <: EventHandler], Seq[Class[_ <: IdentifiedEvent]]]
  val domainTriggerHandlers: Map[Class[_ <: EventHandler], Seq[Class[_ <: IdentifiedEvent]]]

  val swaggerService = (actorMaterializer: ActorMaterializer, mutableApiTypes: Seq[Type]) => new SwaggerHttpService with HasActorSystem {
    implicit val actorSystem = system
    implicit val materializer: ActorMaterializer = actorMaterializer

    implicit def actorRefFactory: ActorRefFactory = system

    override val apiTypes = mutableApiTypes
    override val host = swaggerHost
    override val scheme: Scheme = if (swaggerSchema) Scheme.HTTPS else Scheme.HTTP
  }

  private def routes: Option[Route] = {
    val controllerTypes = paths.filter(path => {
      if (!path.hasController) {
        logger.warn("**************************")
        logger.warn("There is no controller for path: " + path.path)
      }
      path.hasController
    })
    controllerTypes.isEmpty match {
      case true => None
      case false =>
        Some(controllerTypes
          .map(convertPathToTuple)
          .map(convertToControllerWithChildren)
          .map(_.getRoutes).reduce((c1: Route, c2: Route) => {
          c1 ~ c2
        }) ~ path(microservicePrefixPath / "api-doc") {
          getFromResource("swagger/index.html")
        } ~ swaggerService(materializer, mutableApiTypes).routes)
    }
  }

  private def startScheduledServices(): Unit = {
    scheduledServices.foreach(domainScheduler => {
      system.scheduler.schedule(domainScheduler.delay, domainScheduler.interval) {
        domainScheduler.handle()
      }
    })
  }

  private def convertToControllerWithChildren(tuple: (GenericController[_], Path)): GenericController[_] = {
    tuple._1.withChildren {
      notifyForNotSetStreamPath(tuple)
      paths.filter(childPath => tuple._2.streams.contains(childPath.path)
      ).map(convertPathToTuple).map(tuple => tuple._1)
    }
    tuple._1
  }

  def notifyForNotSetStreamPath(tuple: (GenericController[_], Path)): Unit = {
    tuple._2.streams.foreach(stream => paths.map(_.path).contains(stream) match {
      case false =>
        logger.warn("**************************")
        logger.warn("There is no set Path for stream: " + stream + " for path: " + tuple._2.path)
      case true =>
    })
  }

  private def convertPathToTuple(path: Path): (GenericController[_], Path) = {
    path.controller match {
      case Some(controllerType) =>
        val instancedController = injector.getInstance(controllerType)

        validateControllerTypes(path, controllerType, instancedController)
        instancedController.setPathPrefix(microservicePrefixPath)
        mutableApiTypes = mutableApiTypes :+ instancedController.apiControllerType
        instancedController.setStreamPath(path.path)
        (instancedController, path)
    }
  }

  def validateControllerTypes(path: Path, controllerType: Class[_ <: GenericController[_]], instancedController: Any): Unit = {
    routeMap.get(path.path).flatten match {
      case None => instancedController match {
        case _: ResourceController[_] =>
        case _: ResourceWithoutParameterController =>
        case _ =>
          logger.warn("**************************")
          logger.warn("Controller '" + controllerType.getSimpleName + "' for route \"" + path.path + "\" should be of type '"
            + classOf[ResourceController[_]].getSimpleName + "'")
      }
      case Some(_) => instancedController match {
        case _: ResourceController[_] =>
          logger.warn("**************************")
          logger.warn("Controller '" + controllerType.getSimpleName + "' for route \"" +
            path.path + "\" should be of type '" + classOf[ResourceWithParentController[_]].getSimpleName +
            "' or '" + classOf[StreamController] + "'")
        case _: ResourceWithoutParameterController =>
          logger.warn("**************************")
          logger.warn("Controller '" + controllerType.getSimpleName + "' for route \"" +
            path.path + "\" should be of type '" + classOf[ResourceWithParentController[_]].getSimpleName +
            "' or '" + classOf[StreamController].getSimpleName + "'")
        case _ =>

      }
    }
  }


  //new HttpsConnectionContext(sslContext, sslConfig, enabledCipherSuites, enabledProtocols, clientAuth, sslParameters)

  def getHttpsConnectionContext(): HttpsConnectionContext = {

    val password: Array[Char] = "stormbringer".toCharArray
    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("KeyStore.p12")

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, SecureRandom.getInstanceStrong)
    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

    logger.info(":::::::::::::::::HTTPS ENABLED:::::::::::::::::" + https)
    https
  }

  def main(args: Array[String]): Unit = {
    GenericEventHandlerActor.injector = injector
    CommandBus.injector = injector
    commandHandlers.foreach(CommandBus.addHandler)
    generateRouteMap()
    startScheduledServices()

    val isRecoverActor = if (args.isEmpty) false else args(0).toBoolean
    subscribeEventHandlers(isRecoverActor)
    routes match {
      case Some(routes) => if (enableHttps) connectionContext = getHttpsConnectionContext() else connectionContext = Http().defaultServerHttpContext
        Http().bindAndHandle(routes, httpInterface, httpPort, connectionContext)
      case None =>
    }
  }

  def subscribeEventHandlers(isRecoverActor: Boolean): Unit = {
    paths.flatMap(path => path.handlers).foreach(handlerSetting => {
      SubscriberEventHandler.subscribe(isRecoverActor,
        system,
        handlerSetting.events,
        handlerSetting.handler,
        Some(handlerSetting.path.path),
        routeMap.get(handlerSetting.path.path).flatten,
        microservicePrefixPath: String
      )
    })
    val handlers = eventHandlers.toSeq ++ domainTriggerHandlers.toSeq
    var eventHandlersWithAggregateEventsHandler = handlers.toMap
    if (useJournal) {
      eventHandlersWithAggregateEventsHandler = eventHandlersWithAggregateEventsHandler + (classOf[AggregateLifecycleEventHandler] -> Seq(classOf[AggregateDeletedLifecycleEvent], classOf[AggregateInitializedLifecycleEvent]))
    }
    eventHandlersWithAggregateEventsHandler.foreach(eventHandler => {
      SubscriberEventHandler.subscribe(isRecoverActor, system, eventHandler._2, eventHandler._1, None, None, microservicePrefixPath)
    })
  }

  def generateRouteMap(): Unit = {
    paths.map { path => {
      routeMap.put(path.path, None)
      path
    }
    }.foreach {
      path => path.streams.foreach(stream => routeMap.put(stream, Some(path.path)))
    }
  }
}

