package com.latamautos.kernel.scala.rest_api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.Uri.Query
import akka.stream.ActorMaterializer
import spray.json.RootJsonFormat

case class ResourceDefaultParameter(id: String)

abstract class ResourceWithoutParameterController(implicit system: ActorSystem, materializer: ActorMaterializer) extends BaseController[ResourceDefaultParameter] {

  override val receiverBodyParameter: Boolean = false
  override implicit val unMarshaller: RootJsonFormat[ResourceDefaultParameter] = jsonFormat1(ResourceDefaultParameter)

  override def getAllResourcesByParentId(parentId: String, pageRequest: PageRequest, queryString: Query): Any = ???

  override def postResourceByParentId(id: String, data: ResourceDefaultParameter, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def postResourceByParentId(id: String, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def postResource(data: ResourceDefaultParameter, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???

  override def updateResourceById(id: String, data: ResourceDefaultParameter, notification: ActorRef, pageRequest: PageRequest, queryString: Query): Any = ???
}
