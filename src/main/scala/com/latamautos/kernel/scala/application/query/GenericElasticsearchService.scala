package com.latamautos.kernel.scala.application.query

import com.latamautos.kernel.scala.rest_api.PageRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

import scala.collection.JavaConversions._

/**
  * Created by mordred on 6/22/16.
  */
class GenericElasticsearchService[T, U <: ElasticsearchRepository[T,String]] {

  @Autowired
  var dao : U = _

  def findAll(pageRequest: PageRequest): Page[T] = dao.findAll(pageRequest)
  def findAll(): Iterable[T] = dao.findAll().toList
  def find(id: String): T = dao.findOne(id)
  def save(entity : T): Unit = dao.save(entity)
  def delete(id: String): Unit = dao.delete(id)
}

trait IGenericService[T]{
  def findAll(): Iterable[T]
  def findAll(pageRequest: PageRequest): Page[T]
  def find(id: String): T
  def save(entity : T): Unit
  def delete(id : String): Unit
}



