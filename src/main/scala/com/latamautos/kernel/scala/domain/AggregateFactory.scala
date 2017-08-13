package com.latamautos.kernel.scala.domain

import akka.actor.{Props, ActorSystem, ActorRef}

/**
  * Created by xavier on 3/30/16.
  */
abstract class AggregateFactory[T](implicit as: ActorSystem) {

  val actorSystem: ActorSystem = as

  def findOrCreate(id: String): ActorRef = {
    actorSystem.actorOf(props(id), id)
  }

  def props(id: String): Props
}
