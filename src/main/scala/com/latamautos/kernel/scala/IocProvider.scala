package com.latamautos.kernel.scala

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.{AbstractModule, Guice, Injector, Provider}
import com.latamautos.kernel.scala.KernelInfrastructureModule.{ActorSystemProvider, MaterializerProvider}
import com.latamautos.kernel.scala.infrastructure.dao.AggregateLifecycleEventDAO
import com.latamautos.kernel.scala.util.AwsAutoScallingConfig
import net.codingwell.scalaguice.ScalaModule
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate
import java.util.Properties
import com.latamautos.kernel.scala.rest_api.EnvironmentAppConf
import com.typesafe.config.ConfigFactory
import org.springframework.context.ApplicationContext

object KernelInfrastructureModule {

  val SPRING_KERNEL = "spring-kernel.xml"
  val SPRING_ELASTICSEARCH_NODE = "SPRING_ELASTICSEARCH_NODE"
  val SPRING_ELASTICSEARCH_CLUSTER_NAME = "SPRING_ELASTICSEARCH_CLUSTER_NAME"
  val SPRING_CONFIG_KEY              = "spring"
  val SPRING_CONFIG_ES_NODE: String          = "elasticsearch.node"
  val SPRING_CONFIG_ES_CLUSTER_NAME: String  = "elasticsearch.cluster_name"
  val AGG_LIFECYCLE_EVENT_DAO = "aggregateLifecycleEventDAO"
  val ELASTICSEARCH_TEMPLEATE = "elasticsearchTemplate"

  implicit val system = ActorSystem(AwsAutoScallingConfig.akkaActorSystemName, AwsAutoScallingConfig.configAWS)
  val materializer: ActorMaterializer = ActorMaterializer()

  var applicationSpringConfig = ConfigFactory.load(EnvironmentAppConf.getApplicationConfig).getConfig(SPRING_CONFIG_KEY)

  private val context: ApplicationContext = getDefaultSpringApplicationContext(SPRING_KERNEL)

  class ActorSystemProvider() extends Provider[ActorSystem] {
    override def get(): ActorSystem = system
  }

  class MaterializerProvider() extends Provider[ActorMaterializer] {
    override def get(): ActorMaterializer = materializer
  }

  class AggregateEventProvider() extends Provider[AggregateLifecycleEventDAO] {
    override def get(): AggregateLifecycleEventDAO = {
      val aggregateEventDAO = context.getBean(AGG_LIFECYCLE_EVENT_DAO).asInstanceOf[AggregateLifecycleEventDAO]
      aggregateEventDAO
    }
  }

  class ElasticsearchTemplateProvider() extends Provider[ElasticsearchTemplate] {
    override def get(): ElasticsearchTemplate = context.getBean(ELASTICSEARCH_TEMPLEATE).asInstanceOf[ElasticsearchTemplate]
  }


  def getDefaultSpringApplicationContext(appContextFile: String): ClassPathXmlApplicationContext = {
    val context: ClassPathXmlApplicationContext =  new ClassPathXmlApplicationContext()
    val properties: Properties = new Properties()
    properties.setProperty(SPRING_ELASTICSEARCH_NODE, applicationSpringConfig.getString(SPRING_CONFIG_ES_NODE))
    properties.setProperty(SPRING_ELASTICSEARCH_CLUSTER_NAME, applicationSpringConfig.getString(SPRING_CONFIG_ES_CLUSTER_NAME))
    context.addBeanFactoryPostProcessor(getSpringConfigurer(properties))
    context.setConfigLocation(appContextFile)
    context.refresh()
    context
  }

  def getSpringApplicationContextByMap(appContextFile: String, propertiesMap: Map[String, String]): ClassPathXmlApplicationContext = {
    val context: ClassPathXmlApplicationContext =  new ClassPathXmlApplicationContext()
    val properties: Properties = new Properties()
    propertiesMap.foreach{
        keyVal =>  properties.setProperty(keyVal._1, applicationSpringConfig.getString(keyVal._2))
    }
    context.addBeanFactoryPostProcessor(getSpringConfigurer(properties))
    context.setConfigLocation(appContextFile)
    context.refresh()
    context
  }

  def getSpringConfigurer(properties: Properties): PropertyPlaceholderConfigurer = {
    val configurer: PropertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
    configurer.setProperties(properties)
    configurer
  }

}

class KernelInfrastructureModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[ActorSystem].toProvider[ActorSystemProvider].asEagerSingleton()
    bind[ActorMaterializer].toProvider[MaterializerProvider].asEagerSingleton()
    bind[AggregateLifecycleEventDAO].toProvider[KernelInfrastructureModule.AggregateEventProvider].asEagerSingleton()
    bind[ElasticsearchTemplate].toProvider[KernelInfrastructureModule.ElasticsearchTemplateProvider].asEagerSingleton()
  }
}

object SystemInjector {
  val injector: Injector = Guice.createInjector(new KernelInfrastructureModule)
}