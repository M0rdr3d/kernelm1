package com.latamautos.kernel.scala.rest_api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import spray.json.RootJsonFormat

/**
  * Created by xavier on 5/31/16.
  */

case class ForConvenience(id: Int)

abstract class StreamController(implicit system: ActorSystem, materializer: ActorMaterializer)
  extends BaseController[ForConvenience] {
  override def postResource(data: ForConvenience, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def postResourceByParentId(id: String, data: ForConvenience, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def deleteResourceById(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def updateResourceById(id: String, data: ForConvenience, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def getResourceById(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def getAllResources(pageRequest: PageRequest, queryString: Query): Any = ???

  override def updateResourceById(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def postResource(notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def postResourceByParentId(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override implicit val unMarshaller: RootJsonFormat[ForConvenience] = jsonFormat1(ForConvenience)

  override def customRoutes: Option[Route] = None
}
