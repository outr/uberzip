name := "uberzip"
organization := "com.matthicks"
version := "1.0.0-SNAPSHOT"
scalaVersion in ThisBuild := "2.13.2"
crossScalaVersions in ThisBuild := List("2.13.2", "2.12.2", "2.11.11")

libraryDependencies += "io.youi" %% "youi-core" % "0.13.13"