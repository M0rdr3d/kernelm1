package com.latamautos.kernel.scala.application

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotSuccess}
import com.firebase.client.Firebase
import com.google.inject.Injector
import com.latamautos.kernel.scala.domain.Aggregate._
import com.latamautos.kernel.scala.rest_api.Config

trait EventHandler {
  val eventPatternMatching: PartialFunction[IdentifiedEvent, Option[IdentifiedEvent]]
}

trait DomainTriggerHandler extends EventHandler

object GenericEventHandlerActor {
  var injector: Injector = null
}

class GenericEventHandlerActor(x: Class[_ <: EventHandler]) extends PersistentActor with ActorLogging {

  final var EVENT_HANDLER_PERSISTENCE_ID = x.getSimpleName

  override def persistenceId = EVENT_HANDLER_PERSISTENCE_ID

  val handlerInfo = GenericEventHandlerActor.injector.getInstance(x)

  val eventPatternMatching: PartialFunction[IdentifiedEvent, Option[IdentifiedEvent]] = handlerInfo.eventPatternMatching.orElse {
    case event => log.error("There is no pattern matching for " + event)
      None
  }

  //  val eventPatternMatching: PartialFunction[Any, Unit]

  def saveSnapshot[T <: IdentifiedEvent](snapshot: T): Unit = {
    super.saveSnapshot(snapshot)
  }

  val receiveCommand: Receive = {

    case event: IdentifiedEvent => {
      log.debug("[GenericEventHandlerActor] Receive Command event: " + event)
      persist(event)(eventPatternMatching.andThen(saveSnapshotClosure(event)))
    }

    case SaveSnapshotSuccess(metadata) => {
      deleteSnapshot(metadata.sequenceNr - 1)
    }
  }

  def saveSnapshotClosure[U <: IdentifiedEvent](event: U): PartialFunction[Any, Unit] = {
    case evt: IdentifiedEvent => saveSnapshot(evt)
    case _ => saveSnapshot(event)
  }

  val receiveRecover: Receive = {
    case event: IdentifiedEvent => log.debug("[GenericEventHandlerActor] Receive Recover event: " + event) // TODO: Se elimina hasta definir nueva estrategia de recuperación
    //Seq(event).foreach(eventPatternMatching.andThen(saveSnapshotClosure(event.asInstanceOf[IdentifiedEvent])))
    //case RecoveryCompleted => log.debug(s"[EventHandler][receiveRecover] receive >>receiveRecover.RecoveryCompleted<< now will continue in receiveCommand")
  }

}

class RealTimeEventHandlerActor(path: String, parentPath: Option[String], x: Class[_ <: EventHandler], microservicePrefixPath: String)
  extends GenericEventHandlerActor(x) with Config {

  final val TIMESTAMP_PREFIX = "timestamp"
  final val EMPTY = ""
  final val POINT_CHARACTER = "."
  final val UNDER_SCORE_SEPARATOR = "_"

  val firebaseURLFunc: (String, String) => String = (id: String, namespace: String) => {
    val branchFirebaseUrl: String = s"$microservicePrefixPath/$path/$id/$TIMESTAMP_PREFIX"
    if (namespace.nonEmpty)
      s"$firebaseURL/$namespace/$branchFirebaseUrl"
    else
      s"$firebaseURL/$branchFirebaseUrl"
  }

  val firebaseURLParentFunc: (String, String, String) => String = (parentPathString: String, parentId: String, namespace: String) => {
    val branchFirebaseUrl: String = s"$microservicePrefixPath/$parentPathString/$parentId/$path/$TIMESTAMP_PREFIX"
    if (namespace.nonEmpty)
      s"$firebaseURL/$namespace/$branchFirebaseUrl"
    else
      s"$firebaseURL/$branchFirebaseUrl"
  }

  override def saveSnapshot[T <: IdentifiedEvent](snapshot: T): Unit = {
    log.debug("[RealTimeEventHandlerActor] Persisiting on firebase event: " + snapshot)

    val namespace: String = if (snapshot.namespace.isDefined) snapshot.namespace.get.replace(POINT_CHARACTER, UNDER_SCORE_SEPARATOR) else EMPTY
    snapshot match {
      case s: EntityStreamUpdatedEvent =>
        updateStreamParent(s, namespace)
        super.saveSnapshot(s)
      case s: EntityCreatedEvent =>
        new Firebase(firebaseURLFunc(s.id, namespace)).setValue(System.nanoTime())
        updateStreamTimestampOnParent(s, namespace)
        super.saveSnapshot(s)
      case s: EntityDeletedEvent =>
        new Firebase(firebaseURLFunc(s.id, namespace)).removeValue()
        super.saveSnapshot(s)
      case s: EntityUpdatedEvent =>
        new Firebase(firebaseURLFunc(s.id, namespace)).setValue(System.nanoTime())
        super.saveSnapshot(s)
      case s => super.saveSnapshot(s)
    }
    super.saveSnapshot(snapshot)
  }

  def updateStreamTimestampOnParent[T <: IdentifiedEventWithParent](s: T, namespace: String): Unit = {
    parentPath match {
      case Some(parentPathString) => s.parentId match {
        case Some(parentId) =>
          new Firebase(firebaseURLParentFunc(parentPathString, parentId, namespace)).setValue(System.nanoTime())
        case None =>
      }
      case None =>
    }
  }

  def updateStreamParent[T <: IdentifiedEvent](s: T, namespace: String): Unit = {
    parentPath match {
      case Some(parentPathString) =>
        new Firebase(firebaseURLParentFunc(parentPathString, s.id, namespace)).setValue(System.nanoTime())
      case None =>
    }
  }
}

class RecoveryEventHandlerActor(x: Class[_ <: EventHandler]) extends GenericEventHandlerActor(x) with Config {
  override val receiveCommand: Receive = {
    case event => log.error("There is no pattern matching for " + event)
      None
  }

  override val receiveRecover: Receive = {
    case event: IdentifiedEvent =>
      log.debug("[RecoveryEventHandlerActor] Receive Recover event: " + event)
      Seq(event).foreach(eventPatternMatching.andThen(saveSnapshotClosure(event.asInstanceOf[IdentifiedEvent])))
    case RecoveryCompleted => log.debug(s"[EventHandler][receiveRecover] receive >>receiveRecover.RecoveryCompleted<< now will continue in receiveCommand")

  }

}


