package com.latamautos.kernel.scala.util

/**
  * Created by xavier on 8/26/16.
  */
object StringHelper {
  def decapitalize(string: String): String = {

    string match {
      case s if s.length() == 0 => s
      case _ =>
        val c = string.toCharArray
        c(0) = Character.toLowerCase(c(0))
        new String(c)
    }

  }
}
