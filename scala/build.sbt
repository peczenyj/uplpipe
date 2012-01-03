

name := "Uplpipe"

version := "0.1"

scalaVersion := "2.9.1"

libraryDependencies += "javax" % "javaee-api" % "6.0" % "provided"

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "latest.integration" % "test"

libraryDependencies += "org.jmock" % "jmock" % "2.5.1" % "test"

libraryDependencies += "org.jmock" % "jmock-legacy" % "2.5.1" % "test"

resolvers += "Java.net Maven 2 Repo" at "http://download.java.net/maven/2"

seq(jacoco.settings : _*)

libraryDependencies += "log4j" % "log4j" % "1.2.16"

//libraryDependencies += "ch.qos.logback" % "logback-core" % "0.9.24" % "compile" 

//libraryDependencies += "ch.qos.logback" % "logback-classic" % "0.9.24" % "compile"

//libraryDependencies += "org.slf4j" % "log4j-over-slf4j" % "1.6.1"


