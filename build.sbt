import sbt._
import sbt.Keys._

lazy val root =
  Project(id = "root", base = file("."))
    .aggregate(
      app,
      datamodel,      
      transIngress,
      transProcessor,
      transEgress,
      asClient
     )

def appModule(moduleID: String): Project = {
  Project(id = moduleID, base = file(moduleID))
    .settings(
      name := moduleID
    )
    .withId(moduleID)
    .settings(commonSettings)
}

lazy val app = appModule("app")
  .enablePlugins(CloudflowApplicationPlugin)
  .settings(commonSettings)
  .settings(
      name := "cf-as-fraud-detection"
  )

lazy val transIngress = appModule("trans-ingress")
  .enablePlugins(CloudflowAkkaPlugin)
  .settings(
    Test / parallelExecution := false,
    Test / fork := true
  )
  .dependsOn(datamodel)

lazy val transProcessor = appModule("trans-processor")
  .enablePlugins(CloudflowAkkaPlugin)
  .settings(
    Test / parallelExecution := false,
    Test / fork := true
  )
  .dependsOn(datamodel,asClient)

lazy val transEgress = appModule("trans-egress")
  .enablePlugins(CloudflowAkkaPlugin)
  .settings(
    Test / parallelExecution := false,
    Test / fork := true
  )
  .dependsOn(datamodel)

lazy val datamodel = appModule("datamodel")
  .enablePlugins(CloudflowLibraryPlugin)
  .settings(
    schemaCodeGenerator := SchemaCodeGenerator.Java/*,
    schemaPaths := Map(
      SchemaFormat.Proto -> "src/main/protobuf"
    )*/
  )

lazy val asClient = appModule("as-client")
  .enablePlugins(CloudflowAkkaPlugin)
  .settings(
    schemaCodeGenerator := SchemaCodeGenerator.Java,
    schemaPaths := Map(
      SchemaFormat.Proto -> "src/main/protobuf"
    )
  )
  .dependsOn(datamodel)

lazy val commonSettings = Seq(
  organization := "Lightbend",
  headerLicense := Some(HeaderLicense.ALv2("(C) 2016-2021", "Lightbend Inc. <https://www.lightbend.com>")),
  scalaVersion := "2.12.11",
  javacOptions += "-Xlint:deprecation",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-Xlog-reflective-calls",
    "-Xlint",
    "-Ywarn-unused",
    "-Ywarn-unused-import",
    "-deprecation",
    "-feature",
    "-language:_",
    "-unchecked"
  ),

  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused", "-Ywarn-unused-import"),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,

  libraryDependencies ++= Seq(
    "org.scalatest"          %% "scalatest"                 % "3.0.8"    % "test",
    "junit"                  % "junit"                      % "4.12"     % "test"),

  schemaCodeGenerator := SchemaCodeGenerator.Java,
  javacOptions ++= Seq("-Xlint:deprecation"),
  version := "1.0.0-SNAPSHOT"
)
