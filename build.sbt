name := "pipeline"

version := "0.1"

scalaVersion := "2.13.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-explaintypes", "-encoding", "utf8")

libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % "2.6.4"