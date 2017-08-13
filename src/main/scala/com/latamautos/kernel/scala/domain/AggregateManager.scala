package com.latamautos.kernel.scala.domain

import java.util.UUID

import akka.actor._
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import com.google.inject.{Guice, Injector}
import com.latamautos.kernel.scala.KernelInfrastructureModule
import com.latamautos.kernel.scala.domain.Aggregate._
import com.latamautos.kernel.scala.infrastructure.dao.AggregateLifecycleEventDAO
import com.latamautos.kernel.scala.infrastructure.transfer_object.AggregateLifecycleEvent
import com.latamautos.kernel.scala.util.Message500
import com.latamautos.kernel.scala.util.NotificationManager.AddError
import com.latamautos.kernel.scala.util.TimeoutParameters.Implicit._
import net.codingwell.scalaguice.InjectorExtensions._

import scala.collection.immutable.{Nil, Seq, Set}
import scala.concurrent.Future
import scala.reflect.ClassTag


object AggregateManagerFactory {
  type F = ActorRef => ActorRef
  type NM = ActorRef
}

object AggregateManager {
  val maxChildren = 40
  val childrenToKillAtOnce = 20

  case class PendingCommand(sender: ActorRef, aggregateId: String, command: Command)

  val injector: Injector = Guice.createInjector(new KernelInfrastructureModule)
  val aggregateLifecycleEventDAO: AggregateLifecycleEventDAO = injector.instance[AggregateLifecycleEventDAO]

  def isAggregateAvailable(aggregateId: String): Boolean = {
    aggregateLifecycleEventDAO.findOne(aggregateId) match {
      case null => false
      case _ => aggregateLifecycleEventDAO.findOne(aggregateId).status == AggregateLifecycleEvent.Status.INITIALIZED

    }
  }

}


abstract class AggregateManager[U <: Aggregate[_ <: CreateCommand] : ClassTag](implicit val notification: ActorRef) extends Actor with ActorLogging {

  import AggregateManager._


  final val PROXY_NAME_PREFIX = "proxy"
  val actorSystem: ActorSystem = context.system
  implicit val mat = ActorMaterializer()(actorSystem)

  var aggregateClassTag = implicitly[reflect.ClassTag[U]]

  private var childrenBeingTerminated: Set[ActorRef] = Set.empty
  private var pendingCommands: Seq[PendingCommand] = Nil

  def processCommand: Receive = defaultProcessCommand

  override def receive: Receive = processCommand orElse defaultProcessCommand

  protected def aggregateProps(id: String): Props = Props(classOf[AggregateRoot[U]], id, aggregateClassTag.runtimeClass.getConstructors()(0).newInstance(id))

  protected def defaultProcessCommand: Receive = {
    case cmd: CreateWithIdCommand =>
      val senderRef = sender
      create(cmd).map(res => senderRef ! res)
    case cmd: CreateCommand =>
      val senderRef = sender
      create(cmd).map(res => senderRef ! res)
    case cmd: CreateChildCommand =>
      cmd.childId = generateId
      val senderRef = sender
      forwardCommand(cmd, cmd.aggregateId).map(res => senderRef ! res)
    case cmd: IdentifiedCommand =>
      val senderRef = sender
      forwardCommand(cmd, cmd.aggregateId).map(res => senderRef ! res)
    case cmd: QueryCommand =>
      val senderRef = sender
      forwardCommand(cmd, cmd.aggregateId).map(res => {
        senderRef ! res
      })
    case ReceiveTimeout => context.stop(self)
    case _ =>
  }

  def create(cmd: CreateCommand): Future[Any] = {
    val actorRef = createSingletonActor(generateId)
    actorRef ? cmd
  }

  def create(cmd: CreateWithIdCommand): Future[Any] = {
    val actorRef = createSingletonActor(cmd.aggregateId)
    actorRef ? cmd
  }

  def findById(aggregateId: String): Future[Option[ActorRef]] = {
    isAggregateAvailable(aggregateId) match {
      case false => Future {
        None
      }
      case x =>
        val actorSelection: ActorSelection = actorSystem.actorSelection(actorSystem.child(PROXY_NAME_PREFIX + aggregateId))
        actorSelection.resolveOne().map(actorRef => Some(actorRef)).recoverWith {
          case _ => Future(Some(createSingletonActor(aggregateId)))
        }
    }
  }

  def forwardCommand(cmd: Command, aggregateId: String): Future[Any] = {
    findById(aggregateId).flatMap {
      case Some(actorRef) =>
        actorRef ? cmd
      case None =>
        notification ! AddError(Message500("Not make forward the command because not found Aggregate Root"))
        log.info("Not make forward the command because not found Aggregate Root")
        Future {
          None
        }
    }
  }

  def generateId: String = {
    UUID.randomUUID().toString
  }

  def createSingletonActor(aggregateId: String): ActorRef = {
    actorSystem.actorOf(ClusterSingletonManager.props(
      singletonProps = aggregateProps(aggregateId),
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(actorSystem)),
      name = aggregateId)

    actorSystem.actorOf(ClusterSingletonProxy.props(
      singletonManagerPath = "/user/" + aggregateId,
      settings = ClusterSingletonProxySettings(actorSystem)),
      name = PROXY_NAME_PREFIX + aggregateId)
  }

}
