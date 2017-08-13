package com.latamautos.kernel.scala.infrastructure.dao

import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.{Page, Pageable}
import org.springframework.data.elasticsearch.core.query._
import org.springframework.data.elasticsearch.core.{ElasticsearchOperations, ElasticsearchTemplate}
import org.springframework.stereotype.Component

import scala.collection.JavaConversions._

@Component
class CustomElasticsearchDao {

  final val ID = "_id"

  @Autowired
  val elasticSearchTemplate: ElasticsearchTemplate = null

  @Autowired
  val elasticSearchOperations: ElasticsearchOperations = null

  private def searchQueryMethodWithIndexAndType[T](indexName: String, indexType: String, queryBuilder: QueryBuilder, pageable: Pageable, x: Class[T]): Page[T] = {
    val searchQuery: SearchQuery = new NativeSearchQueryBuilder()
      .withQuery(queryBuilder)
      .withIndices(indexName)
      .withTypes(indexType)
      .withPageable(pageable)
      .build()
    val result: Page[T] = elasticSearchTemplate.queryForPage(searchQuery, x)
    result
  }

  def findAllWithIndexAndType[T](indexName: String, indexType: String, pageable: Pageable, x: Class[T]): Page[T] = {
    val queryBuilder: QueryBuilder = matchAllQuery()
    searchQueryMethodWithIndexAndType(indexName, indexType, queryBuilder, pageable, x: Class[T])
  }

  def findByIdWithIndexAndType[T >: Null](indexName: String, indexType: String, id: String, x: Class[T]): T = {
    val queryBuilder: QueryBuilder = boolQuery().must(termQuery(ID, id))
    val pageable: Page[T] = searchQueryMethodWithIndexAndType(indexName, indexType, queryBuilder, null, x)
    if(pageable.getContent.toList.isEmpty) null
    else pageable.getContent.toList.head
  }

  def findAllByPropertyWithIndexAndType[T](indexName: String, indexType: String, propertyName: String, propertyValue: String, pageable: Pageable, x: Class[T]): Page[T] = {
    val queryBuilder: QueryBuilder = boolQuery().must(termQuery(propertyName, propertyValue))
    searchQueryMethodWithIndexAndType(indexName, indexType, queryBuilder, pageable, x: Class[T])
  }

  def saveWithIndexAndType[T](indexName: String, indexType: String, x: T, id: String = null): String = {
    val indexQueryBuilder: IndexQueryBuilder = new IndexQueryBuilder()
    val indexQuery = indexQueryBuilder.withIndexName(indexName)
      .withType(indexType)
      .withObject(x)
      .withId(id)
      .build()
    val createdId = elasticSearchOperations.index(indexQuery)
    elasticSearchOperations.refresh(indexName)
    createdId
  }

  def deleteWithIndexAndType[T](indexName: String, indexType: String, id: String): String = {
    val deletedId = elasticSearchOperations.delete(indexName, indexType, id)
    elasticSearchOperations.refresh(indexName)
    deletedId
  }
}
