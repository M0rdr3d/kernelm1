package com.latamautos.kernel.scala.util

import com.latamautos.kernel.scala.rest_api.EnvironmentAppConf

import scala.collection.JavaConversions._

object SiteConfigUtil {
  val NAME = "name"
  val ALIASES = "aliases"
  val COUNTRY = "country"
  val TIMEZONE = "timezone"
  val OLD_PTX_SERVER = "old-ptx-server"
  val COUNTRY_ABBR = "country-abbreviation"
  val GUIDE_ADVISE_URL = "guide-advice-url"
  val CONTACT_US_URL = "contact_us_url"
  val FORMAT_PRICE = "price-format"
  val DECIMALS = "decimals"
  val DECIMAL_POINT = "decimal-point"
  val SEPARATOR = "separator"
  val PRODUCTION = "production"
  val DEVELOPMENT = "development"
  val ENV = "MSENV"
  val PREFIX_SITE_CONFIG = "site-config"
  val PREFIX_APP_CONFIG = "application"

  def isProdEnvironment(env: String): Boolean ={
    PRODUCTION.equals(env)
  }

}

class SiteConfigUtil(override val nameConfig: String = "site-config") extends SiteConfig {

  import SiteConfigUtil._
  
  def getCountry(siteAlias: String): Option[String] = {
    var country: Option[String] = None
    sites.foreach(x => {
      val aliases: List[String] = x.getStringList(ALIASES).toList
      if (aliases.contains(siteAlias)) {
        country = Some(x.getString(COUNTRY))
      }
    })
    country
  }

  def getSite(siteAlias: String): Option[String] = {
    var site: Option[String] = None
    sites.foreach(x => {
      val aliases: List[String] = x.getStringList(ALIASES).toList
      if (aliases.contains(siteAlias)) {
        site = Some(x.getString(NAME))
      }
    })
    site
  }
}