package com.latamautos.kernel.scala.application

import com.latamautos.kernel.scala.domain.Aggregate.IdentifiedEvent
import com.latamautos.kernel.scala.rest_api.{BaseController, GenericController}


/**
  * Created by xavier on 6/1/16.
  */
case class HandlerSetting(path: Path, handler: Class[_ <: EventHandler]) {
  var events: Seq[Class[_ <: IdentifiedEvent]] = Seq()

  def events(evt: Class[_ <: IdentifiedEvent]): Path = {
    events = events.+:(evt)
    path
  }

  def events(evts: Seq[Class[_ <: IdentifiedEvent]]): Path = {
    events = events.++:(evts)
    path
  }
}

class SimpleHandlerSetting(handler: Class[_ <: EventHandler]) {
  var events: Seq[Class[_ <: IdentifiedEvent]] = Seq()

  def events(evt: Class[_ <: IdentifiedEvent]): Unit = {
    events = events.+:(evt)
  }

  def events(evts: Seq[Class[_ <: IdentifiedEvent]]): Unit = {
    events = events.++:(evts)
  }
}


case class Path(path: String) {
  var controller: Option[Class[_ <: GenericController[_]]] = None
  var handlers: Seq[HandlerSetting] = Seq()
  var streams: Seq[String] = Seq()


  def eventHandler(handler: Class[_ <: EventHandler]): HandlerSetting = {
    val handlerSetting: HandlerSetting = HandlerSetting(this, handler)
    handlers = Seq(handlerSetting)
    handlerSetting
  }

  def controller(controller: Class[_ <: GenericController[_]]): Path = {
    this.controller = Some(controller)
    this
  }

  def hasController: Boolean = {
    controller match {
      case Some(controllerType) => true
      case None =>

        false
    }
  }


  def /(child: String): Path = {
    streams = streams.+:(child)
    this
  }

}

