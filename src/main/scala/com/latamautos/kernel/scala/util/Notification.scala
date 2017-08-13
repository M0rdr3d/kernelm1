package com.latamautos.kernel.scala.util

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by xavier on 5/10/16.
  */
class Notification(var errorMessages: ListBuffer[String], var warningMessagess: ListBuffer[String],
                   var successMessages: ListBuffer[String], var linkedMessages: ListBuffer[Map[String, String]]) {

  private val codeList: ListBuffer[Int] = ListBuffer.empty[Int]

  def addError(message: String): Unit = errorMessages += message

  def getError: ListBuffer[String] = errorMessages

  def hasError: Boolean = errorMessages.nonEmpty

  def hasNotification: Boolean = errorMessages.nonEmpty || warningMessagess.nonEmpty || successMessages.nonEmpty || linkedMessages.nonEmpty

  def addWarning(message: String): Unit = warningMessagess += message

  def getWarning: ListBuffer[String] = warningMessagess

  def addSuccess(message: String): Unit = successMessages += message

  def getSuccess: ListBuffer[String] = successMessages

  def addLinked(message: String, reference: String): Unit = {
    val mapRef: Map[String, String] = Map(reference -> message)
    linkedMessages += mapRef
  }

  def getLinked: ListBuffer[Map[String, String]] = linkedMessages

  def addCode(code: Int): Unit = {
    codeList += code
  }

  def getFirstCode: Int = codeList.head

  def getCodes: ListBuffer[Int] = codeList

}

object Notification {
  def apply(): Notification = new Notification(ListBuffer(), ListBuffer(), ListBuffer(), ListBuffer())

  def apply(error: String): Notification = {
    val notification = new Notification(ListBuffer(), ListBuffer(), ListBuffer(), ListBuffer())
    notification.addError(error)
    notification
  }
}
