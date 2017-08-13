package com.latamautos.kernel.scala.util

/**
  * Created by Harold on 8/6/16.
  */

trait Identity {
  val id: String
}

case class IdentityResponse(override val id: String, tempId: Option[String] = None) extends Identity

case class MessageResponse(message: Any)



