package com.latamautos.kernel.scala.rest_api

import com.latamautos.kernel.scala.util.SiteConfigUtil
import com.typesafe.config.ConfigFactory

trait Config {
  protected val config = ConfigFactory.load(EnvironmentAppConf.getApplicationConfig)
  protected val httpConfig = config.getConfig("http")
  protected val akkaConfig = config.getConfig("akka")
  protected val amazonConfig = config.getConfig("amazon")
  protected val firebase = config.getConfig("firebase")
  protected val routing = config.getConfig("routing")
  protected val routingSubscriberEventHandler = config.getConfig("routing.subscriber-event-handler")
  protected val time = config.getInt("timeout")
  protected val authConfig = config.getConfig("auth")
  protected val swaggerConfig = config.getConfig("swagger")

  val httpInterface = httpConfig.getString("interface")
  val httpPort = httpConfig.getInt("port")
  val page = httpConfig.getInt("page")
  val size = httpConfig.getInt("size")
  val firebaseURL = firebase.getString("url")

  val lowerBound = routingSubscriberEventHandler.getInt("lower-bound")
  val upperBound = routingSubscriberEventHandler.getInt("upper-bound")
  val pressureThreshold = routingSubscriberEventHandler.getInt("pressure-threshold")
  val rampupRate = routingSubscriberEventHandler.getInt("rampup-rate")
  val backoffThreshold = routingSubscriberEventHandler.getDouble("backoff-threshold")
  val backoffRate = routingSubscriberEventHandler.getDouble("backoff-rate")
  val messagesPerResize = routingSubscriberEventHandler.getInt("messages-per-resize")
  val nrOfInstances = routingSubscriberEventHandler.getInt("nr-of-instances")

  val secretKey = amazonConfig.getString("secretKey")
  val accessKey = amazonConfig.getString("accessKey")

  val enableHttps = config.getBoolean("enableHttps")
  val enableCors = config.getBoolean("enableCors")
  val microserviceAuthenticationKey = authConfig.getString("microserviceAuthenticationKey")
  val microserviceAuthenticationName = authConfig.getString("microserviceAuthenticationName")
  val microserviceAutenticationEndPoint = authConfig.getString("microserviceAutenticationEndPoint")
  val userAutenticationEndPoint = authConfig.getString("userAutenticationEndPoint")

  val swaggerHost = swaggerConfig.getString("host")
  val swaggerSchema = swaggerConfig.getBoolean("httpsSchema")

  val tokenTTL = config.getLong("tokenTTL")
  val tokenSecretKey = config.getString("tokenSecretKey")

  val elasticsearchNode = config.getString("spring.elasticsearch.node")
  val elasticsearchClusterName = config.getString("spring.elasticsearch.cluster_name")
  val cassandraContactPoints = config.getList("cassandra-journal.contact-points")
  val cassandraSnapShotStoreContactPoints = config.getList("cassandra-snapshot-store.contact-points")
}

object EnvironmentAppConf {

  def getApplicationConfig: String = {
    Option(System.getenv(SiteConfigUtil.ENV)) match {
      case Some(x) =>
        SiteConfigUtil.PREFIX_APP_CONFIG + "-" + x
      case _ =>
        SiteConfigUtil.PREFIX_APP_CONFIG + "-" + SiteConfigUtil.DEVELOPMENT
    }
  }

   def getSiteConfig: String = {
    Option(System.getenv(SiteConfigUtil.ENV))  match {
      case Some(x) =>
        SiteConfigUtil.PREFIX_SITE_CONFIG + "-" + x
      case _ =>
        SiteConfigUtil.PREFIX_SITE_CONFIG + "-" + SiteConfigUtil.DEVELOPMENT
    }
  }

}
