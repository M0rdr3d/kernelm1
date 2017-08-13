package com.latamautos.kernel.scala.test.integration_test

import java.io.File
import java.util.Properties

import com.latamautos.kernel.scala.util.StringHelper
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.context.support.ClassPathXmlApplicationContext

import scala.reflect.ClassTag

case class TestingElasticsearchFactory(var applicationXmlTestContext: String, var pathHome: String = "integration-data-test") {

  final val PATH_HOME = "PATH_HOME"

  def getDao[T: ClassTag]: T = {

    val properties: Properties = new Properties()
    properties.setProperty(PATH_HOME, pathHome)

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

  def stopEmbeddedElasticsearch() = {
    val index: File = new File(getCurrentDirectory + File.separator + pathHome)
    removeDirectory(index)
  }

  private def removeDirectory(dir: File) {
    if (dir.isDirectory) {
      val files: List[File] = dir.listFiles().toList
      if (files != null && files.nonEmpty) {
        files.foreach(x => {
          removeDirectory(x)
        })
      }
      dir.delete()
    } else {
      dir.delete()
    }
  }

  private def getCurrentDirectory = new java.io.File(".").getCanonicalPath
}
