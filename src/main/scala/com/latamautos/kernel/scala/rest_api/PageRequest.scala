package com.latamautos.kernel.scala.rest_api

import scala.collection.JavaConversions._
import org.springframework.data.domain.Sort.{Direction, Order}
import org.springframework.data.domain.{PageRequest => ParentPageRequest, Pageable, AbstractPageRequest, Sort}

class PageRequest(var site: String, var namespace: String, var appId: String, var pageRequest: ParentPageRequest)
  extends AbstractPageRequest(pageRequest.getPageNumber, pageRequest.getPageSize) {
  override def next(): Pageable = pageRequest.next()

  override def previous(): Pageable = pageRequest.previous()

  override def first(): Pageable = pageRequest.first()

  override def getSort: Sort = pageRequest.getSort

  val size: Int = pageRequest.getPageSize

  val page: Int = pageRequest.getPageNumber

  override def toString: String = "Page request [site: %s,namespace: %s,appid: %s, number: %d, size %d, sort: %s]".format(site, namespace, appId, getPageNumber,
    getPageSize, if (Option(getSort).isEmpty) "empty" else getSort.toString)
}

object PageRequest {
  def apply(site: String, namespace: String, appid: String, page: Int, size: Int): PageRequest = {
    new PageRequest(site, namespace, appid, new ParentPageRequest(page, size))
  }

  def apply(site: String, namespace: String, appid: String, page: Int, size: Int, sort: Iterable[String]): PageRequest =
    sort match {
      case Nil => this (site, namespace, appid, page, size)
      case sortList => new PageRequest(site, namespace, appid, new ParentPageRequest(page, size, new Sort(getOrdersFromQueryString(sortList))))
    }

  def getOrdersFromQueryString(sortListParam: Iterable[String]): List[Order] = {
    sortListParam.filter(x =>
      x.split(",").toList match {
        case Nil => false
        case i :: Nil =>
          !i.isEmpty && (i.equals(x.trim) || (i + ",").equals(x.trim))
        case i :: xs =>
          !i.isEmpty && xs.size == 1
      }).map(x => {
      val propertyDirection = x.split(",")
      val (property: String, direction: Option[String]) =
        (propertyDirection(0), if (propertyDirection.length > 1) Some(propertyDirection(1)) else None)
      new Order(Direction.fromStringOrNull(direction.orNull), property)
    }).toList
  }
}
