name := "microservice-kernel"

organization := "com.latamautos"

version := "1.0.3"

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq("scala-kernel" at "http://jenkins.latamautos.com:8081/nexus/content/repositories/scala-kernel/",
  "Spring milestones" at "https://repo.spring.io/milestone/")

libraryDependencies ++= {
  val akkaVersion = "2.4.6"
  val guiceVersion = "4.0"
  val sprayVersion = "1.3.2"
  val springDataVersion = "2.0.0.M1"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.14",
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.google.inject" % "guice" % guiceVersion,
    "net.codingwell" %% "scala-guice" % "4.0.1",
    "io.spray" %% "spray-json" % sprayVersion,
    "io.spray" %% "spray-http" % sprayVersion,
    "io.spray" %% "spray-client" % sprayVersion,
    "com.typesafe.akka"  %% "akka-http-spray-json-experimental"    % akkaVersion,
    "org.springframework.data" % "spring-data-commons" % springDataVersion,
    "org.springframework.data" % "spring-data-elasticsearch" % "2.0.1.RELEASE",
    "com.firebase" % "firebase-client-jvm" % "2.5.2",
    "org.json4s" %% "json4s-native" % "3.3.0",
    "com.amazonaws" % "aws-java-sdk" % "1.11.48",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
    "ch.megard" %% "akka-http-cors" % "0.1.7",
    "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.7.2",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.7.4",
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.7.2",
    "com.jason-goodwin" %% "authentikat-jwt" % "0.3.5",
    "com.google.code.gson" % "gson" % "2.6.2",
  "org.cassandraunit" % "cassandra-unit" % "3.0.0.1"
  )
}

assemblyMergeStrategy in assembly := {
  case PathList("application.conf") => MergeStrategy.concat
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "spring.tooling" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.first
    }
  case _ => MergeStrategy.first
}

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

fork in run := true