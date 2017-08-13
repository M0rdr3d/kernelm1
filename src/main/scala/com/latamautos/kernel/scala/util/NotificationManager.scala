/**
  * @author Livan Frometa lfrometa@latamautos.com
  * @author Harold Portocarrero hportocarrero@latamautos.com
  * @version 1.0.0
  */

package com.latamautos.kernel.scala.util

import akka.actor._

object NotificationManager {

  trait Command

  case class AddError(messageCode: MessageCode) extends Command

  case object HasError extends Command

  case object HasNotification extends Command

  case object GetError extends Command

  case class AddWarning(messageCode: MessageCode) extends Command

  case class AddSuccess(messageCode: MessageCode) extends Command

  case class AddLinked(messageCode: MessageCode, reference: String) extends Command

  case object GetNotifications extends Command

  case object HasNotifications extends Command

  def props: Props = Props(new NotificationManager)
}

class NotificationManager extends PassivateActor with ActorLogging {

  import NotificationManager._

  private val notification: Notification = Notification()

  override def receive: Receive = {
    case AddError(messageCode) =>
      notification.addCode(messageCode.code)
      notification.addError(messageCode.message)
    case AddWarning(messageCode) =>
      notification.addWarning(messageCode.message)
    case AddSuccess(messageCode) =>
      notification.addSuccess(messageCode.message)
    case AddLinked(messageCode, reference: String) =>
      notification.addLinked(messageCode.message, reference)
    case HasError => sender() ! notification.hasError
    case HasNotification => sender() ! notification.hasNotification
    case GetNotifications => sender() ! notification
    case ReceiveTimeout => context stop self
    case _ =>
  }
}
