package com.latamautos.kernel.scala.test.integration_test

import java.util.Properties

import com.latamautos.kernel.scala.util.StringHelper
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.context.support.ClassPathXmlApplicationContext

import scala.reflect.ClassTag

case class TestingCassandraFactory(var applicationXmlTestContext: String, var host: String = "127.0.0.1", var port: String = "9042", var keyspace: String = "keyspace_test", var cassandraYmlFile: String = "cu-cassandra.yaml") {

  final val CASSANDRA_HOST = "CASSANDRA_HOST"
  final val CASSANDRA_PORT = "CASSANDRA_PORT"
  final val CASSANDRA_KEYSPACE = "CASSANDRA_KEYSPACE"

  def startEmbeddedCassandra() = {
    println("::::::::::::::::::::::::::::::::::::::::::::  INITIALIZING CASSANDRA EMBEDDED SERVER ::::::::::::::::::::::::::::::::::::::::::::")
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(cassandraYmlFile)
    val cluster = com.datastax.driver.core.Cluster.builder().addContactPoints("127.0.0.1").build()
    val cassandraSession = cluster.connect()
    cassandraSession.execute("CREATE KEYSPACE " + keyspace + " WITH replication = {'class':'SimpleStrategy', 'replication_factor':3};")
    println("::::::::::::::::::::::::::::::::::::::::::::  CASSANDRA HAS STARTED AND CREATED KEYSPACE   ::::::::::::::::::::::::::::::::::::::::::::")
  }

  def cleanEmbeddedCassandra() = {
    EmbeddedCassandraServerHelper.cleanEmbeddedCassandra()
  }

  def getDao[T: ClassTag]: T = {

    val properties: Properties = new Properties()
    properties.setProperty(CASSANDRA_HOST, host)
    properties.setProperty(CASSANDRA_PORT, port)
    properties.setProperty(CASSANDRA_KEYSPACE, keyspace)

    val context: ClassPathXmlApplicationContext = new ClassPathXmlApplicationContext()
    context.addBeanFactoryPostProcessor(getSpringConfigure(properties))
    context.setConfigLocation(applicationXmlTestContext)
    context.refresh()

    val genericClassTag = implicitly[reflect.ClassTag[T]]
    context.getBean(StringHelper.decapitalize(genericClassTag.runtimeClass.getSimpleName)).asInstanceOf[T]
  }

  def getSpringConfigure(properties: Properties): PropertyPlaceholderConfigurer = {
    val configure: PropertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer()
    configure.setProperties(properties)
    configure
  }
}
