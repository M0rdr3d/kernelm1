package com.latamautos.kernel.scala.util

import java.text.{DateFormat, SimpleDateFormat}
import java.util
import java.util.{Locale, TimeZone}
import scala.collection.JavaConversions._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.routing.{DefaultResizer, RoundRobinPool}
import com.amazonaws.auth.{AWSSessionCredentials, BasicAWSCredentials, BasicSessionCredentials}
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient
import com.amazonaws.services.cognitoidentity.model._
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.{InvokeRequest, InvokeResult}
import com.google.gson.Gson
import com.latamautos.kernel.scala.SystemInjector
import com.latamautos.kernel.scala.application.SubscriberEventHandler
import com.typesafe.config.Config
import com.latamautos.kernel.scala.rest_api.{Config => ConfigParent}
import net.codingwell.scalaguice.InjectorExtensions._

/**
  * Created by marcoguillen on 27/10/16.
  */
case class OpenIdIdentityToken(identityId: String, openIdToken: String)
case class ClientContext(client_id: String, app_title: String)
case class EventContext(platform: String)
case class ServiceContext(mobile_analytics: ServiceMobileAnalytics)
case class ServiceMobileAnalytics(app_id: String)
case class ClientContextHeader(client: ClientContext, env: EventContext, services: ServiceContext)
case class SessionRequestBody(id: String, startTimestamp: String)
case class AttributesRequestBody(target_feature: String, utm_campaign_souce: String, utm_campaign_medium: String,
                                 utm_campaign_name: String, utm_campaign_content: String)
case class MetricsRequestBody(target_number: Int)
case class RequestBody(eventType: String, timestamp: String, version: String, session: SessionRequestBody,
                       attributes: AttributesRequestBody, metrics: MetricsRequestBody)
case class Payload(clientContext: String, events: util.ArrayList[RequestBody])
case class PutAMACmd(namespace: String, lambda: String, userId: String, eventType: String, target_feature: String, utm_campaign_souce: String,
                     utm_campaign_medium: String, utm_campaign_name: String, utm_campaign_content: String)


object AMAServiceSupervisorActor extends ConfigParent{
  val injector = SystemInjector.injector
  implicit val system: ActorSystem = injector.instance[ActorSystem]
  val amaSupervisorDefinition = BackoffSupervisor.props(
    Backoff.onStop(Props[AMAServiceActor], childName = AMAServiceActor.getClass.getSimpleName,
      minBackoff = SubscriberEventHandler.MINBACKOFF, maxBackoff = SubscriberEventHandler.MAXBACKOFF,
      randomFactor = SubscriberEventHandler.SUPERVISORRANDON_FACTOR)
  )
  val resizer = DefaultResizer(lowerBound = lowerBound, upperBound = upperBound, backoffRate = backoffRate,
    backoffThreshold = backoffThreshold, messagesPerResize = messagesPerResize)
  def getAMAActor(): ActorRef = {
    system.actorOf(RoundRobinPool(1, Some(resizer)).props(amaSupervisorDefinition),AMAServiceActor.getClass.getSimpleName)
  }
}

class AMAServiceActor extends Actor{
  override def receive: Receive = {
    case PutAMACmd(namespace: String, lambda: String, userId: String, eventType: String, target_feature: String, utm_campaign_souce: String,
    utm_campaign_medium: String, utm_campaign_name: String, utm_campaign_content: String) => {
      AMAServiceActor.setConfigBySite(namespace)
      val payload: Payload = AMAServiceActor.getPayload(eventType, target_feature, utm_campaign_souce, utm_campaign_medium,
        utm_campaign_name, utm_campaign_content)
      AMAServiceActor.putAMAEvent(namespace, lambda, AMAServiceActor.AWS_USER_POLL_ID, userId, payload)
    }
    case _ => None
  }
}

object AMAServiceActor {

  val siteConfig = ConfigFactoryEnvironment.load("site-config")
  val sites: List[Config] = siteConfig.getConfigList("sites").toList
  var AWS_IDENTITY_POOL_ID: String        = ""
  var AWS_TOKEN_DURATION: Long            = 0
  var AWS_ACCESS_KEY: String              = ""
  var AWS_SECRET_KEY: String              = ""
  var AWS_USER_POLL_ID: String            = ""
  var AWS_COGNITO_PROFILE: String         = ""
  var adminCredentials: BasicAWSCredentials = null
  var AWS_MOBILE_ANALYTICS_APP_ID     = ""
  var AWS_MOBILE_BACKEND_PLATFORM         = ""
  var AWS_MOBILE_ANALYTICS_CONTEXT_HEADER = "v2.0"
  var AWS_MOBILE_ANALYTICS_ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
  var identityId: String = ""
  var openIdToken: String = ""

  def setConfigBySite(namespace: String): Unit = {
    var mobileAnalyticsConfig: Option[Config] = None
    sites.foreach(x => {
       val aliases: List[String] = x.getStringList(SiteConfigUtil.ALIASES).toList
      if (aliases.contains(namespace)) {
        mobileAnalyticsConfig = Some(x.getConfig("mobile-analytics"))
      }
    })

    mobileAnalyticsConfig match {
      case Some(config: Config) => {
        AWS_IDENTITY_POOL_ID        = config.getString("identity_pool_id")
        AWS_TOKEN_DURATION          = config.getInt("token_duration")
        AWS_ACCESS_KEY              = config.getString("accessKey")
        AWS_SECRET_KEY              = config.getString("secretKey")
        AWS_USER_POLL_ID            = config.getString("user_poll_id")
        AWS_COGNITO_PROFILE         = config.getString("user_cognito_profile")
        adminCredentials            = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)
        AWS_MOBILE_ANALYTICS_APP_ID = config.getString("analytics_app_id")
        AWS_MOBILE_BACKEND_PLATFORM = config.getString("platform")
      }
      case _ => None
    }
  }


  def putAMAEvent(namespace: String, lambda: String, userPoolId: String, userId: String, payload: Payload): Unit ={
    getOpenIdTokenForDeveloperIdentityRequest(userPoolId, userId) match {
      case Some(x: OpenIdIdentityToken) =>
        getSessionCredentialsBasedOnToken(x) match {
          case Some(y:AWSSessionCredentials)  => invokeMobileAnalyticsLambda(y,lambda, payload)
          case _ => None
        }
      case _ => None
    }
  }

  def getOpenIdTokenForDeveloperIdentityRequest(userPoolId: String, userId: String): Option[OpenIdIdentityToken] = {
    try {
      val client: AmazonCognitoIdentityClient = new AmazonCognitoIdentityClient(adminCredentials)
      val tokenRequest: GetOpenIdTokenForDeveloperIdentityRequest = new GetOpenIdTokenForDeveloperIdentityRequest()
      client.setRegion(Region.getRegion(Regions.US_EAST_1))
      tokenRequest.setIdentityPoolId(AWS_IDENTITY_POOL_ID)
      val jmap = new java.util.HashMap[String, String]()
      jmap.put(userPoolId, userId)
      tokenRequest.setLogins(jmap)
      tokenRequest.setTokenDuration(AWS_TOKEN_DURATION)
      val result: GetOpenIdTokenForDeveloperIdentityResult = client.getOpenIdTokenForDeveloperIdentity(tokenRequest)
      identityId = result.getIdentityId()
      openIdToken = result.getToken()
      /*println("-----GetOpenIdTokenForDeveloperIdentityRequest-----");
      println("[OK] identityId: " + identityId)
      println("[OK] openIdToken: " + openIdToken)*/
      Some(OpenIdIdentityToken(identityId, openIdToken))
    }
    catch{
      case e: Exception =>
        println("Error:", e.getMessage)
        None
    }
  }

  def getSessionCredentialsBasedOnToken(openIdIdentityToken: OpenIdIdentityToken): Option[AWSSessionCredentials] = {
    val jmap = new java.util.HashMap[String, String]()
    jmap.put(AWS_COGNITO_PROFILE, openIdIdentityToken.openIdToken)
    val getCredentialsRequest = new GetCredentialsForIdentityRequest().withIdentityId(openIdIdentityToken.identityId).withLogins(jmap)
    val cognitoIdentityClient: AmazonCognitoIdentityClient = new AmazonCognitoIdentityClient(adminCredentials)
    val getCredentialsResult: GetCredentialsForIdentityResult = cognitoIdentityClient.getCredentialsForIdentity(getCredentialsRequest)
    val credentials: Credentials = getCredentialsResult.getCredentials();
    /*println("-----GetCredentialsBasedOnToken-----")
    println("[OK] accessKeyId: " + credentials.getAccessKeyId())
    println("[OK] secretAccessKey: " + credentials.getSecretKey())
    println("[OK] sessionToken: " + credentials.getSessionToken())*/
    val sessionCredentials: AWSSessionCredentials = new BasicSessionCredentials(
      credentials.getAccessKeyId(),
      credentials.getSecretKey(),
      credentials.getSessionToken()
    )
    Some(sessionCredentials)
  }

  def invokeMobileAnalyticsLambda(sessionCredentials: AWSSessionCredentials, lambda: String, payload: Payload): Unit = {
    val lambdaClient:AWSLambdaClient  = new AWSLambdaClient(sessionCredentials)
    lambdaClient.setRegion(Region.getRegion(Regions.US_EAST_1))
    try {
      val invokeRequest: InvokeRequest  = new InvokeRequest()
      invokeRequest.setFunctionName(lambda)
      invokeRequest.setPayload(new Gson().toJson(payload))
      //println("Payload JSON:", new Gson().toJson(payload))
      val result: InvokeResult = lambdaClient.invoke(invokeRequest)
      println(s"[OK] InvokeMobileAnalyticsLambda status (${result.getStatusCode})")
    } catch {
      case e: Exception =>
        println(s"ERROR when Mobile Analytics Lambda was invoked ($lambda)", e)
        false
    }
  }

  def getISO8601StringForDate(date: java.util.Date): String = {
    val dateFormat: DateFormat = new SimpleDateFormat(AWS_MOBILE_ANALYTICS_ISO8601_FORMAT, Locale.US)
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
    dateFormat.format(date)
  }

  def getPayload(eventType: String, target_feature: String, utm_campaign_souce: String, utm_campaign_medium: String,
                 utm_campaign_name: String, utm_campaign_content: String): Payload = {
    val uuid = java.util.UUID.randomUUID.toString
    val timestamp = getISO8601StringForDate(new java.util.Date())
    val clientContextHeader = ClientContextHeader(ClientContext(uuid, eventType) , EventContext(AWS_MOBILE_BACKEND_PLATFORM),
      ServiceContext(ServiceMobileAnalytics(AWS_MOBILE_ANALYTICS_APP_ID)))
    val requestBody = RequestBody(
      eventType,
      timestamp,
      AWS_MOBILE_ANALYTICS_CONTEXT_HEADER,
      SessionRequestBody(uuid, timestamp),
      AttributesRequestBody(target_feature, utm_campaign_souce, utm_campaign_medium, utm_campaign_name, utm_campaign_content),
      MetricsRequestBody(0)
    )
    val list : util.ArrayList[RequestBody] = new util.ArrayList[RequestBody]
    list.add(requestBody)
    Payload(new Gson().toJson(clientContextHeader), list)
  }

}
