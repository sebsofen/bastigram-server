name := "bastigram-srv"

version := "1.0"

scalaVersion := "2.11.8"

//create native apps

enablePlugins(JavaAppPackaging)

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.8"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.8"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.9"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "org.mockito" % "mockito-core" % "1.9.5"
