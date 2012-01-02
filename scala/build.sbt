

name := "Uplpipe"

version := "0.1"

scalaVersion := "2.9.1"

libraryDependencies += "javax" % "javaee-api" % "6.0" % "provided"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "latest.integration" % "test"

libraryDependencies += "org.jmock" % "jmock" % "2.5.1" % "test"

libraryDependencies += "org.jmock" % "jmock-legacy" % "2.5.1" % "test"

seq(jacoco.settings : _*)

