package com.latamautos.kernel.scala.application

import akka.event.{ActorEventBus, LookupClassification}
import com.latamautos.kernel.scala.domain.Aggregate.{Event => AggregateEvent}

object ApplicationEventBus extends ActorEventBus with LookupClassification {

    type Event = AggregateEvent
    type Classifier = String // Todo: Verificar la mejor estrategia

    protected def mapSize(): Int = 10 // Todo: Verificar mapSize

    protected def classify(event: Event): Classifier = {
      event.getClass.getSimpleName
    }

    protected def publish(event: Event, subscriber: Subscriber): Unit = {
      subscriber ! event
    }
}



