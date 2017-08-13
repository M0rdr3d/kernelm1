package com.latamautos.kernel.scala.application

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.Injector
import com.latamautos.kernel.scala.util.NotificationManager
import net.codingwell.scalaguice.InjectorExtensions._

import scala.concurrent.Future
import scala.reflect.ClassTag

object ApplicationService {
  var injector: Injector = null
  val system: ActorSystem = injector.instance[ActorSystem]
  val notificationDefault = system.actorOf(NotificationManager.props)
}

abstract class ApplicationService[T <: ApplicationServiceCommand : ClassTag] {
  def getCommandClassSimpleName: String = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  def handle(command: T)(implicit notification: ActorRef = ApplicationService.notificationDefault): Future[T#R]
}


