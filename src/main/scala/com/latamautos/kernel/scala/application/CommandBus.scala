package com.latamautos.kernel.scala.application

import akka.actor._

import scala.collection.mutable
import scala.concurrent.Future
import akka.pattern.ask
import com.latamautos.kernel.scala.util.{NotificationManager, PassivateActor}
import com.latamautos.kernel.scala.application.enums.EnumCommandBus
import net.codingwell.scalaguice.InjectorExtensions._
import akka.util.Timeout
import com.google.inject.Injector

import scala.util.{Failure, Success}
import com.latamautos.kernel.scala.util.TimeoutParameters.Implicit._

class CommandBus

object CommandBus {
  var handlerClassMap: mutable.Map[String, Class[_]] = mutable.Map()
  var injector: Injector = null

  class CommandBusActor[T <: ApplicationServiceCommand, V] extends PassivateActor with ActorLogging  {
    override def receive: Receive = {
      case CommandBusActorMsg(command, notificationActorRef) =>
        val commandName = command.getClass.getSimpleName
        val x = handlerClassMap.get(commandName)
        if (x.isDefined) {
          val handler = injector.getInstance(x.get).asInstanceOf[ApplicationService[T]]
          val s = sender
          handler.handle(command.asInstanceOf[T])(notificationActorRef).asInstanceOf[V].asInstanceOf[Future[Any]].map(res => {
            s ! res
          })
          log.info(EnumCommandBus.LOG_INFO_COMMANDBUS_ACTOR_MSG)
        } else {
          throw new Exception(EnumCommandBus.EXCEPTION_NO_HANDLER_MESSAGE.format(commandName))
        }
      case ReceiveTimeout => context stop self
    }
  }

  case class Retry[U, T <: ApplicationServiceCommand](message: U ⇒ T, notificationActorRef: ActorRef)

  case class Response(result: Any)

  case class CommandBusActorMsg[C](command: Any, notificationActorRef: ActorRef)

  class CommandBusSupervisor extends PassivateActor with ActorLogging  {

    var originalSender: ActorRef = null
    var maxRetries: Int = EnumCommandBus.MAXIMUM_RETRIES

    def supervise[U, T <: ApplicationServiceCommand](f: U ⇒ T, notificationActorRef: ActorRef) {
      implicit val timeout = Timeout(EnumCommandBus.SUPERVISED_TIMEOUT)
      val system: ActorSystem = injector.instance[ActorSystem]
      val actorRef = system.actorOf(Props(new CommandBusActor[T, U]))
      val future = actorRef ? CommandBusActorMsg(f(null.asInstanceOf[U]), notificationActorRef)
      future.onComplete {
        case Success(result) =>
          log.debug(EnumCommandBus.LOG_DEBUG_SUPERVISOR_FUTURE_SUCCESS.format(result));
          self ! Response(result)
        case Failure(ex) => {
          log.debug(EnumCommandBus.LOG_DEBUG_SUPERVISOR_FUTURE_FAILURE.format(ex))
          self ! Retry(f, notificationActorRef)
        }
      }
    }

    override def receive: Receive = {
      case Response(result) => {
        log.debug(EnumCommandBus.LOG_DEBUG_RECEIVE_RESPONSE.format(result, originalSender))
        originalSender ! result
      }
      case ReceiveTimeout => context stop self
      case Retry(message, notificationActorRef) => {
        maxRetries -= 1
        log.debug(EnumCommandBus.LOG_DEBUG_TRYING_MESSAGE.format(message))
        if (originalSender == null) originalSender = sender()
        if (maxRetries > EnumCommandBus.MAXIMUM_RETRIES_VALIDATION) {
          supervise(message, notificationActorRef)
        } else {
          log.debug(EnumCommandBus.LOG_DEBUG_MAX_RETRIES_ATTEMPTS_REACHED_MESSAGE.format(EnumCommandBus.MAXIMUM_NUMBER_ATTEMPTS_REACHED, message))
          throw new Exception(EnumCommandBus.MAXIMUM_NUMBER_ATTEMPTS_REACHED)
        }
      }
    }
  }

  implicit class ImplicitHandler(ref: CommandBus) {
    val system: ActorSystem = injector.instance[ActorSystem]

    def handle[U, T <: ApplicationServiceCommand](f: U ⇒ T, notificationActorRef: ActorRef = system.actorOf(NotificationManager.props))
                                                 (implicit system: ActorSystem): Future[U] = {
      val handler = system.actorOf(Props(new CommandBusSupervisor))
      val future = handler ? Retry(f, notificationActorRef)
      future.asInstanceOf[Future[U]]
    }
  }

  def addHandler(clazz: Class[_ <: ApplicationService[_]]): Unit = {
    handlerClassMap.put(injector.getInstance(clazz).getCommandClassSimpleName, clazz)
  }

}

