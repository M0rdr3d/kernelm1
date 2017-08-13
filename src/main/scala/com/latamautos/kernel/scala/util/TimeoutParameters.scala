package com.latamautos.kernel.scala.util

import scala.concurrent.ExecutionContext
import akka.util.Timeout
import com.latamautos.kernel.scala.rest_api.Config

import scala.concurrent.duration._

/**
  * Created by Harold on 14/6/16.
  */
object TimeoutParameters extends Config {

  object Implicit {
    implicit val ec = ExecutionContext.Implicits.global
    implicit lazy val timeout = Timeout(time seconds)
  }

}
