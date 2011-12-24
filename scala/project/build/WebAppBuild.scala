import sbt._
class WebAppBuild(info: ProjectInfo) extends DefaultWebProject(info) {
val jetty6 = "org.mortbay.jetty" % "jetty" % "6.1.14" % "test" 
val servletApi = "javax.servlet" % "servlet-api" % "2.5"
}
