

name := "bastigram-srv"

version := "1.0"

scalaVersion := "2.11.8"


libraryDependencies +="com.typesafe.akka" %% "akka-http" % "10.0.8"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.8"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.9"