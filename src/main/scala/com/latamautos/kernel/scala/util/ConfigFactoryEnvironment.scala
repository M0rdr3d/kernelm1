package com.latamautos.kernel.scala.util
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by marcoguillen on 27/10/16.
  */
object ConfigFactoryEnvironment{

  var env: String = System.getenv(SiteConfigUtil.ENV)

  def load(resourceBasename: String): Config = {
    var resourceBasenameWithEnv: String = ""
    Option(System.getenv(SiteConfigUtil.ENV)) match {
      case Some(x) =>
        resourceBasenameWithEnv = resourceBasename + EnvironmentEnum.CHARACTER_SEPARATOR + env
      case _ =>
        resourceBasenameWithEnv = resourceBasename + EnvironmentEnum.CHARACTER_SEPARATOR + EnvironmentEnum.DEVELOPMENT
    }
    println(s"--------------------------------------------------------------------------------------------------------- Config Loaded: $resourceBasenameWithEnv")
    ConfigFactory.load(resourceBasenameWithEnv)

  }
}

object EnvironmentEnum {
  final val CHARACTER_SEPARATOR = "-"
  final val DEVELOPMENT = "development"
}