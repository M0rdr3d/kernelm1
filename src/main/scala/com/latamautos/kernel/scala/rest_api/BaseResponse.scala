package com.latamautos.kernel.scala.rest_api

import com.latamautos.kernel.scala.util.Notification

case class PathResponse(var path: String, var realTimePath: String)

class BaseResponse[T](val data: T, val page: Int, val size: Int, val status: Int,
                      val total_items: Int, val total_pages: Int, val notifications: Notification,
                      val paths: Seq[PathResponse] = Seq.empty[PathResponse])
