import sbt.Keys._

name := "scala-akka-env"

version := "1.0"

scalaVersion := "2.12.1"
val akkaVersion = "2.5.4"

libraryDependencies ++= Seq(
  // Typesafe
  "com.typesafe.play" % "play-json_2.11" % "2.5.4",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  // Scala JS

  // Akka cluster
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  // Akka persistence
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query-experimental" % "2.4.14",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.21",
  "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.4.17.3",
  // Tagging utilities
  "com.softwaremill.macwire" %% "macros" % "2.2.5",
  "com.softwaremill.macwire" %% "util" % "2.2.5",
  "com.softwaremill.common" %% "tagging" % "2.0.0",
  // Circe
  "io.circe" %% "circe-core" % "0.7.0",
  "io.circe" %% "circe-generic" % "0.7.0",
  "io.circe" %% "circe-parser" % "0.7.0",
  // Avro
  "com.sksamuel.avro4s" %% "avro4s-core" % "1.6.4",
  "com.sksamuel.avro4s" %% "avro4s-macros" % "1.6.4",
  // Cats
  "org.typelevel" %% "cats" % "0.8.1"
)

// JMH
enablePlugins(JmhPlugin)
enablePlugins(ScalaJSPlugin)