package com.latamautos.kernel.scala.util

/**
  * Created by Harold on 7/6/16.
  */
object FutureFlatten {

  import scala.concurrent.{Future, ExecutionContext}

  implicit class FutureFlatten[T](f: Future[Future[T]]) {
    def flatten(implicit ec: ExecutionContext): Future[T] = f flatMap identity
  }

}
