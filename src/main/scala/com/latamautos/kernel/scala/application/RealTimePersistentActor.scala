package com.latamautos.kernel.scala.application

import akka.persistence.{SnapshotMetadata, PersistentActor}

/**
  * Created by xavier on 6/2/16.
  */
trait RealTimePersistentActor extends PersistentActor {
  override def saveSnapshot(snapshot: Any): Unit = {
    super.saveSnapshot(snapshot)
  }
}
