package com.latamautos.kernel.scala.rest_api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.Uri.Query
import akka.stream.ActorMaterializer

abstract class ResourceWithParentController[T](implicit system: ActorSystem, materializer: ActorMaterializer)
  extends BaseController[T] {

  override def getAllResources(pageRequest: PageRequest, queryString: Query): Any = ???

  override def postResource(data: T, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def postResource(notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def postResourceByParentId(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def updateResourceById(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???
}