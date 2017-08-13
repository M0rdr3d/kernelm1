package com.latamautos.kernel.scala.util

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.JavaConversions._

trait SiteConfig {
  protected val nameConfig: String
  protected val siteConfig = ConfigFactoryEnvironment.load(nameConfig)
  protected val sites: List[Config] = siteConfig.getConfigList("sites").toList
}