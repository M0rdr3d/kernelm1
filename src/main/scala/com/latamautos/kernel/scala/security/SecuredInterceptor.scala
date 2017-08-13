package com.latamautos.kernel.scala.security


import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.Uri.Query
import com.latamautos.kernel.scala.SystemInjector
import com.latamautos.kernel.scala.rest_api.Config
import com.latamautos.kernel.scala.util.{CodeException, Message401}
import org.aopalliance.intercept.{MethodInterceptor, MethodInvocation}
import spray.http._
import net.codingwell.scalaguice.InjectorExtensions._

import scala.concurrent.{Await, Future}
import spray.client.pipelining._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import spray.httpx.SprayJsonSupport._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Success, Try}

/**
  * Created by hermes on 3/10/16.
  */

case class Data(microserviceName: String, checked: Boolean, message: String)
case class BaseResponse(data: Data)

case class UserData(isValid: Boolean, isExpired: Boolean, message: String)
case class UserBaseResponse(data: UserData)

class SecuredInterceptor extends MethodInterceptor with Config with DefaultJsonProtocol{

  implicit val actorSystem = SystemInjector.injector.instance[ActorSystem]




  override def invoke(invocation: MethodInvocation): AnyRef = {
    val queryString: Query = getQueryStringArgument(invocation)


    if(queryString.get("accessToken").isDefined){
      implicit val dataFormat: RootJsonFormat[UserData] = jsonFormat3(UserData)
      implicit val baseResponse: RootJsonFormat[UserBaseResponse] = jsonFormat1(UserBaseResponse)
      val pipeline: HttpRequest => Future[UserBaseResponse] = sendReceive ~> setContentType(MediaTypes.`application/json`) ~> unmarshal[UserBaseResponse]
      var url: String = userAutenticationEndPoint.replace("{accessToken}",queryString.get("accessToken").get)
      val pipe = pipeline(Get(url))
      val result: Try[UserBaseResponse] = Await.ready(pipe,Duration.Inf).value.get
      val resultEither = result match {
        case Success(t) => {
          if(!t.data.isValid)
            throw new CodeException(Message401(t.data.message))
          else if (t.data.isExpired)
            throw new CodeException(Message401(t.data.message))
        }
      }
    }
    else if(queryString.get("microserviceSecretKey").isDefined && queryString.get("microserviceName").isDefined){
      implicit val dataFormat: RootJsonFormat[Data] = jsonFormat3(Data)
      implicit val baseResponse: RootJsonFormat[BaseResponse] = jsonFormat1(BaseResponse)
      val pipeline: HttpRequest => Future[BaseResponse] = sendReceive ~> setContentType(MediaTypes.`application/json`) ~> unmarshal[BaseResponse]
      var url: String = microserviceAutenticationEndPoint.replace("{microserviceSecretKey}",queryString.get("microserviceSecretKey").get)
      url = url.replace("{microserviceName}",queryString.get("microserviceName").get)

      val pipe = pipeline(Get(url))
      val result: Try[BaseResponse] = Await.ready(pipe,Duration.Inf).value.get
      val resultEither = result match {
        case Success(t) => {
          if(!t.data.checked){
            throw new CodeException(Message401("Authentification Microservice Error!!! microserviceName or microserviceSecretKey not valid"))
          }
        }
      }
    }
    else {
      throw new CodeException(Message401("No credentials detected in queryString"))
    }
    invocation.proceed()
  }

  def setContentType(mediaType: MediaType)(r: HttpResponse): HttpResponse = {
    r.withEntity(HttpEntity(ContentType(mediaType), r.entity.data))
  }

  def getQueryStringArgument(invocation: MethodInvocation) : Query = {
    var i,pos : Int = 0
    for(a <- invocation.getMethod.getParameterTypes){
      if(a.getName.equals(classOf[Query].getName)){
        pos = i
      }
      i += 1
    }
    invocation.getArguments()(pos).asInstanceOf[Query]
  }

}
