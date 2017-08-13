package com.latamautos.kernel.scala.application

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.routing.{RoundRobinPool, DefaultResizer, FromConfig}
import com.latamautos.kernel.scala.domain.Aggregate.IdentifiedEvent
import com.latamautos.kernel.scala.rest_api.Config

import scala.concurrent.duration._

object SubscriberEventHandler extends Config {

  final val MINBACKOFF = 2.seconds
  final val MAXBACKOFF = 6.seconds
  final val SUPERVISORRANDON_FACTOR = 0.1
  final val SUPERVISOR_SUFFIX = "Supervisor"
  final val UNDEFINED_EVENT_MESSAGE: String = "================================================================================= ERROR =======================================================================================\n" +
    "===============================================================================================================================================================================\n" +
    "===============================================================================================================================================================================\n" +
    "THIS EVENT: [%s] \n" +
    "Is not defined at HANDLER: [%s] \n" +
    "===============================================================================================================================================================================\n" +
    "===============================================================================================================================================================================\n" +
    "===============================================================================================================================================================================\n"
  final val EVENT_BUS_SUBSCRIPTION_MESSAGE: String = "APPLICATION EVENT BUS - SUBSCRIBING EventHandler: [%s] to channel: [%s]"
  final val FUTURE_FAILURE_VALIDATE_EVENT: String = "FAILURE ON FUTURE - WHEN EVENT WAS VALIDATED. Event: [%s] / Details: [%s]" //t.getMessage

  def getPrimitiveValueFromStringType(typeString: String, parametrizedTypeString: String = null): Object = {
    val t = typeString match {
      case "Option" => Some(getPrimitiveValueFromStringType(parametrizedTypeString))
      case "Some" => Some(getPrimitiveValueFromStringType(parametrizedTypeString))
      case "boolean" => java.lang.Boolean.FALSE
      case "byte" => java.lang.Byte.MIN_VALUE
      case "short" => java.lang.Short.MIN_VALUE
      case "int" => java.lang.Integer.MIN_VALUE
      case "long" => java.lang.Long.MIN_VALUE
      case "char" => java.lang.Character.MIN_VALUE
      case "float" => java.lang.Float.MIN_VALUE
      case "double" => java.lang.Double.MIN_VALUE
      case _ => null
    }
    t.asInstanceOf[Object]
  }

  def subscribe(isRecoverActor: Boolean,
                system: ActorSystem,
                eventSeq: Seq[Class[_ <: IdentifiedEvent]],
                eventHandlerInfo: Class[_ <: EventHandler],
                path: Option[String] = None,
                parentPath: Option[String] = None,
                microservicePrefixPath: String): Unit = {
    val eventHandlerRef = createSupervisorByEventHandler(isRecoverActor, system, eventHandlerInfo, path, parentPath,microservicePrefixPath)
    eventSeq.foreach(event => {

      eventIsDefinedAt(eventHandlerInfo, event) match {
        case true => subscribeEventHandlerToChannel(event, eventHandlerInfo, eventHandlerRef)
        case false => exitForUndefinedEvent(event, eventHandlerInfo)
      }

    })
  }

  def eventIsDefinedAt[T <: IdentifiedEvent](eventHandlerInfo: Class[_ <: EventHandler], event: Class[T]): Boolean = { true
//    eventHandlerInfo.newInstance().eventPatternMatching.isDefinedAt(event.getConstructors()(0).
//      newInstance(event.getConstructors()(0).getParameters.toSeq.
//        map(x => getPrimitiveValueFromStringType(x.getType.getSimpleName, x.getParameterizedType.getTypeName)): _*).asInstanceOf[T])
  }

  def subscribeEventHandlerToChannel(event: Class[_ <: IdentifiedEvent], eventHandlerInfo: Class[_ <: EventHandler], eventHandlerRef: ActorRef): Boolean = {
    println(EVENT_BUS_SUBSCRIPTION_MESSAGE.format(eventHandlerInfo.getSimpleName, event.getSimpleName))
    ApplicationEventBus.subscribe(eventHandlerRef, event.getSimpleName)
  }

  def createSupervisorByEventHandler(isRecoverActor: Boolean,
                                      system: ActorSystem,
                                     eventHandlerInfo: Class[_ <: EventHandler],
                                     path: Option[String] = None,
                                     parentPath: Option[String] = None,
                                     microservicePrefixPath: String): ActorRef = {
    val className = eventHandlerInfo.getSimpleName
    val childProps = path match {
      case None => Props(new GenericEventHandlerActor(eventHandlerInfo))
      case Some(pathString) => Props(new RealTimeEventHandlerActor(pathString, parentPath, eventHandlerInfo,microservicePrefixPath))
    }
    val eventHandlerSupervisorDefinition = BackoffSupervisor.props(
      Backoff.onStop(childProps, childName = className, minBackoff = MINBACKOFF, maxBackoff = MAXBACKOFF,
        randomFactor = SUPERVISORRANDON_FACTOR)
    )

    var eventHandlerSupervisor: ActorRef = null
    if(isRecoverActor){
      println(s">>>>>>>>> eventHandlerSupervisor WITHOUT RoundRobinPool $isRecoverActor")
      eventHandlerSupervisor = system.actorOf(eventHandlerSupervisorDefinition)
    }else{
      println(s">>>>>>>>> eventHandlerSupervisor WITH RoundRobinPool $isRecoverActor")
      val resizer = DefaultResizer(lowerBound = lowerBound, upperBound = upperBound, backoffRate = backoffRate,
        backoffThreshold = backoffThreshold, messagesPerResize = messagesPerResize)
      eventHandlerSupervisor = system.actorOf(RoundRobinPool(1, Some(resizer)).props(eventHandlerSupervisorDefinition),className)
    }
    eventHandlerSupervisor
  }


  def exitForUndefinedEvent(event: Class[_ <: IdentifiedEvent], eventHandlerInfo: Class[_ <: EventHandler]): Unit = {
    println(UNDEFINED_EVENT_MESSAGE.format(event, eventHandlerInfo))
    System.exit(0)
  }

}