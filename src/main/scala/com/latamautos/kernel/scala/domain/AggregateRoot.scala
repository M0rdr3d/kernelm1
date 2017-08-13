package com.latamautos.kernel.scala.domain

import akka.actor._
import akka.cluster.Cluster
import akka.persistence._
import com.latamautos.kernel.scala.application.ApplicationEventBus
import com.latamautos.kernel.scala.domain.Aggregate._
import com.latamautos.kernel.scala.util._

import scala.concurrent.duration._

object AggregateRoot {
  val eventsPerSnapshot = 10
}

case object RemovedAggregate


case class AggregateDeletedLifecycleEvent(override val id: String, aggregateName: String, override val namespace: Option[String])
  extends IdentifiedEvent

case class AggregateInitializedLifecycleEvent(override val id: String, aggregateName: String, override val namespace: Option[String])
  extends IdentifiedEvent


class AggregateRoot[T <: Aggregate[CreateCommand]](id: String, var state: T) extends PersistentActor with ActorLogging with PassivateAggregateActor {

  import AggregateRoot._


  final val UNINITIALIZED: Boolean = false
  final val DELETED: Boolean = true
  final val INITIALIZED: Boolean = true
  final val UNDELETED: Boolean = false
  final val PREFIX_PROXY: String = "proxy"
  var senderRefToKill: Option[ActorRef] = None

  override def persistenceId: String = id


  val cluster = Cluster(context.system)

  private var eventsSinceLastSnapshot = 0

  protected def afterEventPersisted(evt: Event): Unit = {
    eventsSinceLastSnapshot += 1
    if (eventsSinceLastSnapshot >= eventsPerSnapshot) {
      saveSnapshot(state)
      eventsSinceLastSnapshot = 0
    }

    state.updateState(evt)
    this.state = state
    context become created

    evt match {
      case e: EventWithSaveSnapshot => saveSnapshot(state)
      case _ =>
    }

    publish(evt)

  }

  private def publish(event: Event) = ApplicationEventBus.publish(event)

  override val receiveRecover: Receive = {
    case event: EventNotUpdatedState =>
    case event: Event =>
      eventsSinceLastSnapshot += 1
      state.updateState(event)
      this.state = state
      context become created
    case SnapshotOffer(metadata, state: T) =>
      context become created
      this.state = state
      state.self = self
      log.debug("recovering aggregate from snapshot")
    case RecoveryCompleted =>
      state.self = self
      state.postRecovery()
  }

  val created: Receive = {

    case cmd: CreateCommand =>
      state.self = self
      state.initialize(cmd, cmd.notification) match {
        case (Some(event), identity: Identity) =>

          persist(event)(afterEventPersisted)
          publish(AggregateInitializedLifecycleEvent(id, state.getClass.getSimpleName, None))
          senderSuccess(identity)
        case (None, None) => senderFailure()
      }
    case cmd: DeleteCommand =>


      state.self = self
      state.validateCmd(cmd, cmd.notification) match {
        case (Some(event), identity: Identity) => {
          publish(AggregateDeletedLifecycleEvent(id, state.getClass.getSimpleName, None))
          persist(event)(afterEventPersisted)
          senderSuccess(identity)
        }
        case (None, None) => senderFailure()
      }
    case IsDeleted => sender() ! UNDELETED
    case cmd: IdentifiedCommand =>
      state.self = self
      state.validateCmd(cmd, cmd.notification) match {
        case (Some(event), identity: Identity) => {
          persist(event)(afterEventPersisted)
          senderSuccess(identity)
        }
        case (None, None) => senderFailure()
      }

    case cmd: QueryCommand =>
      state.self = self
      state.queryCmd(cmd, cmd.notification) match {
        case MessageResponse(message) =>
          sender() ! MessageResponse(message)
      }
    case PersistEventCommand(event) =>
      println("-------------->")
      println(event)
      persist(event)(afterEventPersisted)
    case SetTimeoutCommand(time: FiniteDuration) => context.setReceiveTimeout(time)
    case ReceiveTimeout =>
      val actorSystem: ActorSystem = context.system
      actorSystem.actorSelection(actorSystem.child(id)) ! Kill
      actorSystem.actorSelection(actorSystem.child("proxy" + id)) ! Kill
      context stop self
    case KillAggregate =>
      val actorSystem: ActorSystem = context.system
      actorSystem.actorSelection(actorSystem.child(id)) ! Kill
      actorSystem.actorSelection(actorSystem.child("proxy" + id)) ! Kill
      context stop self
  }


  override def receiveCommand: Receive = created


  def senderSuccess(identity: Identity): Unit = sender() ! identity

  def senderFailure(): Unit = sender() ! None
}