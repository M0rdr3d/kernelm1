package com.latamautos.kernel.scala.test.integration_test

import com.latamautos.kernel.scala.util.StringHelper
import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext

import scala.reflect.ClassTag

case class TestingDaoFactory(applicationXmlTestContext: String) {
  protected val context: ApplicationContext = new ClassPathXmlApplicationContext(applicationXmlTestContext)

  def getDao[T: ClassTag]: T = {
    val genericClassTag = implicitly[reflect.ClassTag[T]]
    context.getBean(StringHelper.decapitalize(genericClassTag.runtimeClass.getSimpleName)).asInstanceOf[T]
  }
}
