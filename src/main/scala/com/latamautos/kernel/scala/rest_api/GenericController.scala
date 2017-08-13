package com.latamautos.kernel.scala.rest_api

import akka.http.scaladsl.server._

import scala.reflect.runtime.universe.Type

/**
  * Created by jorgerodriguez on 18/10/16.
  */
trait GenericController[T] {

  var parent: Option[GenericController[_]] = None

  var children: Seq[GenericController[_]] = Seq()

  def withChildren(controller: GenericController[_]): GenericController[_] = ???

  def withChildren(controllers: Seq[GenericController[_]]): GenericController[_] = ???

  def getRoutesNormal: Route = ???

  def apiControllerType: Type = ???

  def setStreamPath(streamPath: String): Unit = ???

  def setPathPrefix(pathPrefixName: String): Unit = ???

  def getRoutes: Route = ???

}
