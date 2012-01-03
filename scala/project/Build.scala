import sbt._
import Keys._
import ScalaMockPlugin._

object MyBuild extends Build {
 
  override lazy val settings = super.settings ++ Seq(
    organization := "com.uplpipe",
    version := "1.0",
    scalaVersion := "2.9.1",
 
    resolvers += ScalaToolsSnapshots,
    libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "2.1",
    libraryDependencies += "javax" % "javaee-api" % "6.0",
    autoCompilerPlugins := true,
    addCompilerPlugin("org.scalamock" %% "scalamock-compiler-plugin" % "2.1"))
 
  lazy val myproject = Project("MyProject", file(".")) settings(generateMocksSettings: _*) configs(Mock)
}
