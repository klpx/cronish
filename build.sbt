name := "cronish"

organization := "ru.arigativa"

version := "1.0.0-SNAPSHOT"

parallelExecution in Test := false

scalaVersion := "2.12.2"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions ++= Seq("-feature", "-language:implicitConversions", "-language:postfixOps")

libraryDependencies +=  "com.github.philcali" %% "scalendar" % "0.1.5"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5",
  "com.typesafe.akka" %% "akka-actor" % "2.5.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
