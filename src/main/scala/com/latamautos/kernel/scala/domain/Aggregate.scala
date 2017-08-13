package com.latamautos.kernel.scala.domain

import akka.actor.ActorRef
import com.latamautos.kernel.scala.domain.Aggregate._

import scala.concurrent.duration.FiniteDuration

object Aggregate {

  trait Event

  trait IdentifiedEvent extends Event {
    val id: String
    val namespace: Option[String]
  }

  trait IdentifiedEventWithParent extends IdentifiedEvent {
    val parentId: Option[String]
  }

  trait EntityStreamUpdatedEvent extends  IdentifiedEvent

  trait EntityCreatedEvent extends IdentifiedEventWithParent

  trait EntityDeletedEvent extends IdentifiedEventWithParent

  trait EntityUpdatedEvent extends IdentifiedEvent

  trait EventWithSaveSnapshot extends Event

  trait EventNotUpdatedState extends Event

  trait Command

  trait CreateCommand extends Command {
    implicit val notification: ActorRef
  }

  trait IdentifiedCommand extends Command {
    val aggregateId: String
    implicit val notification: ActorRef
  }

  trait DeleteCommand extends IdentifiedCommand

  trait CreateWithIdCommand extends CreateCommand {
    val aggregateId: String
    implicit val notification: ActorRef
  }

  trait IdentifiedChildCommand extends IdentifiedCommand {
    override val aggregateId: String
    var childId: String
  }

  case class PersistCommand(any: Any) extends Command

  case class PersistEventCommand(event: Event) extends Command

  case class SetTimeoutCommand(time: FiniteDuration) extends Command

  case class PersistEventsCommand(events: List[Event]) extends Command

  trait CreateChildCommand extends IdentifiedChildCommand

  case object Remove extends Command

  case object IsInitialized extends Command

  case object IsDeleted extends Command

  case object GetState extends Command

  case object KillAggregate extends Command

  trait QueryCommand extends Command {
    val aggregateId: String
    implicit val notification: ActorRef
  }

}

abstract class Aggregate[T <: CreateCommand] extends java.io.Serializable{
  @transient
  var self: ActorRef = _

  def initialize(cmd: T, notification: ActorRef): (Option[Event], Any)

  def validateCmd(cmd: Command, notification: ActorRef): (Option[Event], Any)

  def updateState(event: Event): Unit

  def queryCmd(cmd: QueryCommand, notification: ActorRef): Any = None

  def postRecovery(): Unit = {}

  var isDeleted: Boolean = false
}