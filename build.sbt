
name := "WebServer"

version := "0.1"

scalaVersion := "2.12.2"


val sparkVersion = "2.4.3"
val akkaVersion = "10.1.8"
//val sparkVersion = "2.3.1"


assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}


libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % sparkVersion, //  % "provided",
  "org.apache.spark" %% "spark-streaming" % sparkVersion, // % "provided",
  "org.apache.spark" %% "spark-hive" % sparkVersion, //  % "provided",
  "com.typesafe.akka" %% "akka-http" % akkaVersion, //% "provided",
  "com.typesafe.akka" %% "akka-actor" % "2.5.19",
  "com.typesafe.akka" %% "akka-stream" % "2.5.19",
  "io.spray" %% "spray-json" % "1.3.5"
)
