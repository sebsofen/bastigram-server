name := "bastigram-srv"

version := "1.0"

scalaVersion := "2.12.4"

//create native apps

enablePlugins(JavaAppPackaging)

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.8"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.8"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.9"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "org.mockito" % "mockito-core" % "1.9.5"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"


libraryDependencies += "de.bastigram" %% "bastigram-creator" % "0.1"

//add bastigram creator as dep
//libraryDependencies +=