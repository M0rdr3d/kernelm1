package com.latamautos.kernel.scala.util

import akka.actor.Actor
import scala.concurrent.duration._

trait PassivateActor extends Actor {
  override def preStart(): Unit = {
    context.setReceiveTimeout(1.minutes)
    super.preStart()
  }
}

trait PassivateAggregateActor extends Actor {
  override def preStart(): Unit = {
    context.setReceiveTimeout(1.minutes)
    super.preStart()
  }
}