package com.latamautos.kernel.scala.util

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.ec2.AmazonEC2Client
import com.latamautos.kernel.scala.rest_api.{Config, EnvironmentAppConf}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

object AwsAutoScallingConfig extends Config {
  protected lazy val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))
  val akkaRemotePort = akkaConfig.getInt("remote.netty.tcp.port")
  val akkaActorSystemName = akkaConfig.getString("actor-system")

  private lazy val ec2 = {
    val credentials = new DefaultAWSCredentialsProviderChain
    val region = Region.getRegion(Regions.US_EAST_1)
    val scalingClient = new AmazonAutoScalingClient(credentials) { setRegion(region) }
    val ec2Client = new AmazonEC2Client(credentials) { setRegion(region) }
    new AwsEc2(scalingClient, ec2Client)
  }

  val (host, siblings) = {
    if(amazonConfig.getBoolean("autoscaling")){
      logger.info("Using EC2 autoscaling configuration")
      logger.info(s"host      --> ${ec2.currentIp}")
      logger.info(s"siblings  --> ${ec2.siblingIps}")
      logger.info(s"port      --> $akkaRemotePort")
      (ec2.currentIp, ec2.siblingIps)
    }else{
      (ec2.currentIp, List.empty[String])
    }
  }

  val seeds = siblings map (ip => s"akka.tcp://$akkaActorSystemName@$ip:$akkaRemotePort")

  private val defaults = config

  private val overrideConfig = {
    if (seeds.nonEmpty) {
      ConfigFactory.empty()
        .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(host))
        .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(akkaRemotePort))
        .withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seeds))
    }else {
      defaults
    }
  }

  val configAWS = {
    if(amazonConfig.getBoolean("autoscaling")){
      overrideConfig withFallback defaults
    }else {
      defaults
    }
  }
}
