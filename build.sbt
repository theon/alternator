name := "alternator"

organization  := "com.github.theon"

version       := "0.0.1"

scalaVersion  := "2.11.8"

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.10" ::
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.10"/* % "provided"*/ ::
  "io.spray" %%  "spray-json" % "1.3.2"/* % "provided" */:: Nil

// Test Dependencies
libraryDependencies ++=
  "org.scalatest" %% "scalatest" % "3.0.0" % "test" :: Nil
