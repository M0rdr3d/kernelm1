package com.latamautos.kernel.scala.application.query

import javax.annotation.Resource

import com.latamautos.kernel.scala.infrastructure.dao.CustomElasticsearchDao
import org.springframework.data.domain.{Page, Pageable}
import org.springframework.stereotype.Component

@Component
class CustomElasticsearchService {

  @Resource
  private var customElasticsearchComponent : CustomElasticsearchDao = _

  def getCustomElasticsearchComponent(): CustomElasticsearchDao = customElasticsearchComponent

  def findAllWithIndexAndType[T](indexName: String, indexType: String, pageable: Pageable, x : Class[T]) : Page[T]={
    customElasticsearchComponent.findAllWithIndexAndType(indexName, indexType, pageable, x)
  }

  def findByIdWithIndexAndType[T  >: Null](indexName: String, indexType: String, id: String, x: Class[T]) : T ={
    customElasticsearchComponent.findByIdWithIndexAndType(indexName, indexType, id, x)
  }

  def findAllByPropertyWithIndexAndType[T](indexName: String, indexType: String, propertyName: String, propertyValue: String, pageable: Pageable, x : Class[T]) : Page[T] = {
    customElasticsearchComponent.findAllByPropertyWithIndexAndType(indexName, indexType, propertyName, propertyValue, pageable, x)
  }

  def saveWithIndexAndType[T](indexName: String, indexType: String, x: T, id: String = null) : String ={
    customElasticsearchComponent.saveWithIndexAndType(indexName, indexType, x, id)
  }

  def deleteWithIndexAndType[T](indexName: String, indexType: String, id: String) : String ={
    customElasticsearchComponent.deleteWithIndexAndType(indexName, indexType, id)
  }

}
