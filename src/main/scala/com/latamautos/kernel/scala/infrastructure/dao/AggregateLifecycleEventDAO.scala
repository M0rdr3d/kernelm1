package com.latamautos.kernel.scala.infrastructure.dao

import com.latamautos.kernel.scala.infrastructure.transfer_object.AggregateLifecycleEvent
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

/**
  * Created by xavier on 8/4/16.
  */
trait AggregateLifecycleEventDAO extends ElasticsearchRepository[AggregateLifecycleEvent, String] {

}
