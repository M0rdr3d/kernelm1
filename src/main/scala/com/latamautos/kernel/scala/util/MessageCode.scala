package com.latamautos.kernel.scala.util

class MessageCode(var code: Int, var message: String)

case class Message100(msg: String) extends MessageCode(MessageCodeConst.CONTINUE, msg)

case class Message101(msg: String) extends MessageCode(MessageCodeConst.SWITCHING_PROTOCOL, msg)

case class Message102(msg: String) extends MessageCode(MessageCodeConst.PROCESSING, msg)

case class Message200(msg: String) extends MessageCode(MessageCodeConst.SUCCESS, msg)

case class Message201(msg: String) extends MessageCode(MessageCodeConst.CREATED, msg)

case class Message202(msg: String) extends MessageCode(MessageCodeConst.ACCEPTED, msg)

case class Message203(msg: String) extends MessageCode(MessageCodeConst.NON_AUTHORIZATION_INFORMATION, msg)

case class Message204(msg: String) extends MessageCode(MessageCodeConst.NO_CONTENT, msg)

case class Message205(msg: String) extends MessageCode(MessageCodeConst.RESET_CONTENT, msg)

case class Message206(msg: String) extends MessageCode(MessageCodeConst.PARTIAL_CONTENT, msg)

case class Message207(msg: String) extends MessageCode(MessageCodeConst.MULTI_STATUS, msg)

case class Message208(msg: String) extends MessageCode(MessageCodeConst.ALREADY_REPORTED, msg)

case class Message300(msg: String) extends MessageCode(MessageCodeConst.FOUND, msg)

case class Message301(msg: String) extends MessageCode(MessageCodeConst.MOVED_PERMANENTLY, msg)

case class Message303(msg: String) extends MessageCode(MessageCodeConst.SEE_OTHER, msg)

case class Message304(msg: String) extends MessageCode(MessageCodeConst.NOT_MODIFIED, msg)

case class Message305(msg: String) extends MessageCode(MessageCodeConst.USE_PROXY, msg)

case class Message307(msg: String) extends MessageCode(MessageCodeConst.TEMPORARY_REDIRECT, msg)

case class Message308(msg: String) extends MessageCode(MessageCodeConst.PERMANENT_REDIRECT, msg)

case class Message400(msg: String) extends MessageCode(MessageCodeConst.BAD_REQUEST, msg)

case class Message401(msg: String) extends MessageCode(MessageCodeConst.NOT_AUTHENTICATE, msg)

case class Message402(msg: String) extends MessageCode(MessageCodeConst.PAYMENT_REQUIRED, msg)

case class Message403(msg: String) extends MessageCode(MessageCodeConst.FORBIDDEN, msg)

case class Message404(msg: String) extends MessageCode(MessageCodeConst.NOT_FOUND, msg)

case class Message405(msg: String) extends MessageCode(MessageCodeConst.METHOD_NOT_ALLOWED, msg)

case class Message406(msg: String) extends MessageCode(MessageCodeConst.NOT_ACCEPTABLE, msg)

case class Message407(msg: String) extends MessageCode(MessageCodeConst.PROXY_AUTHENTICATE_REQUIRED, msg)

case class Message408(msg: String) extends MessageCode(MessageCodeConst.REQUEST_TIMEOUT, msg)

case class Message409(msg: String) extends MessageCode(MessageCodeConst.CONFLICT, msg)

case class Message500(msg: String) extends MessageCode(MessageCodeConst.SERVER_ERROR, msg)

case class Message501(msg: String) extends MessageCode(MessageCodeConst.NOT_IMPLEMENTED, msg)

case class Message502(msg: String) extends MessageCode(MessageCodeConst.BAD_GATEWAY, msg)

case class MessageWarning(msg: String) extends MessageCode(MessageCodeConst.WARNING, msg)

object MessageCodeConst {
  val WARNING: Int = 0

  val CONTINUE: Int = 100
  val SWITCHING_PROTOCOL: Int = 101
  val PROCESSING: Int = 102

  val SUCCESS: Int = 200
  val CREATED: Int = 201
  val ACCEPTED: Int = 202
  val NON_AUTHORIZATION_INFORMATION: Int = 203
  val NO_CONTENT: Int = 204
  val RESET_CONTENT: Int = 205
  val PARTIAL_CONTENT: Int = 206
  val MULTI_STATUS: Int = 207
  val ALREADY_REPORTED: Int = 208


  val FOUND: Int = 300
  val MOVED_PERMANENTLY: Int = 301
  val SEE_OTHER: Int = 303
  val NOT_MODIFIED: Int = 304
  val USE_PROXY: Int = 305
  val TEMPORARY_REDIRECT: Int = 307
  val PERMANENT_REDIRECT: Int = 308

  val BAD_REQUEST: Int = 400
  val NOT_AUTHENTICATE: Int = 401
  val PAYMENT_REQUIRED: Int = 402
  val FORBIDDEN: Int = 403
  val NOT_FOUND: Int = 404
  val METHOD_NOT_ALLOWED: Int = 405
  val NOT_ACCEPTABLE: Int = 406
  val PROXY_AUTHENTICATE_REQUIRED: Int = 407
  val REQUEST_TIMEOUT: Int = 408
  val CONFLICT: Int = 409

  val SERVER_ERROR: Int = 500
  val NOT_IMPLEMENTED: Int = 501
  val BAD_GATEWAY: Int = 502

}
