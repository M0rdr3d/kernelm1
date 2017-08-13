package com.latamautos.kernel.scala.application

import com.google.inject.Inject
import com.latamautos.kernel.scala.domain.Aggregate.IdentifiedEvent
import com.latamautos.kernel.scala.domain.{AggregateDeletedLifecycleEvent, AggregateInitializedLifecycleEvent}
import com.latamautos.kernel.scala.infrastructure.dao.AggregateLifecycleEventDAO
import com.latamautos.kernel.scala.infrastructure.transfer_object.AggregateLifecycleEvent

class AggregateLifecycleEventHandler @Inject()(aggregateEventDAO: AggregateLifecycleEventDAO) extends EventHandler {


  override val eventPatternMatching: PartialFunction[IdentifiedEvent, Option[IdentifiedEvent]] = {
    case AggregateInitializedLifecycleEvent(id, aggregateName, _) =>
      aggregateEventDAO.index(new AggregateLifecycleEvent(id, aggregateName, AggregateLifecycleEvent.Status.INITIALIZED))
      None
    case AggregateDeletedLifecycleEvent(id, aggregateName, _) =>
      aggregateEventDAO.index(new AggregateLifecycleEvent(id, aggregateName, AggregateLifecycleEvent.Status.DELETED))
      None

  }

}
