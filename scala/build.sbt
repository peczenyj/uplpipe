
seq(webSettings :_*)

name := "Uplpipe"

version := "0.1"

scalaVersion := "2.9.1"

resolvers += "Java.net Maven 2 Repo" at "http://download.java.net/maven/2"

libraryDependencies += "log4j" % "log4j" % "1.2.16"

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.1.v20110908" % "container",
  "javax.servlet" % "servlet-api" % "2.5" % "provided"
)

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "latest.integration" % "test"

libraryDependencies += "org.jmock" % "jmock" % "2.5.1" % "test"

libraryDependencies += "org.jmock" % "jmock-legacy" % "2.5.1" % "test"


