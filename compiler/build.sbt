name := "llambda"

version := "0.0.1"

organization := "llambda"

scalaVersion := "2.10.3"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.2" % "test"

libraryDependencies += "org.scala-lang" % "jline" % "2.10.3"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.0.0"

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint")
