package com.latamautos.kernel.scala.domain

import scala.concurrent.duration.FiniteDuration

abstract class ApplicationScheduler {
    val delay: FiniteDuration
    val interval : FiniteDuration
    def handle()
  }
