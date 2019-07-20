val versions = new {
  val cats = "2.0.0-M4"
  val catsEffect = "2.0.0-M4"
  val http4s = "0.21.0-M2"
  val circe = "0.12.0-M4"
  val logback = "1.2.3"
  val commonsIo = "2.6"
  val pureconfig = "0.11.1"
  val refined = "0.9.8"
  val guava = "28.0-jre"
  val scala = "2.13.0"
  val scalaLogging = "3.9.2"
  val scalaTest = "3.0.8"
  val scalaCheck = "1.14.0"
}

val dependencies = {
  import versions._
  new {
    val `cats-core` = "org.typelevel" %% "cats-core" % cats
    val `cats-effect` = "org.typelevel" %% "cats-effect" % catsEffect
    val `http4s-server` = "org.http4s" %% "http4s-blaze-server" % http4s
    val `http4s-client` = "org.http4s" %% "http4s-blaze-client" % http4s
    val `http4s-dsl` = "org.http4s" %% "http4s-dsl" % http4s
    val `http4s-circe` = "org.http4s" %% "http4s-circe" % http4s
    val `circe-generic` = "io.circe" %% "circe-generic" % circe
    val `commons-io` = "commons-io" % "commons-io" % commonsIo
    val pureconfig = "com.github.pureconfig" %% "pureconfig" % versions.pureconfig
    val refined = "eu.timepit" %% "refined" % versions.refined
    val guava = "com.google.guava" % "guava" % versions.guava
    val `logback-classic` = "ch.qos.logback" % "logback-classic" % logback
    val `scala-logging` = "com.typesafe.scala-logging" %% "scala-logging" % scalaLogging
    val `scala-test` = "org.scalatest" %% "scalatest" % scalaTest % "test"
    val scalacheck = "org.scalacheck" %% "scalacheck" % scalaCheck % "test"
  }
}

val commonSettings = Seq(
  organization := "com.hastybox.deplayedwebhookstoo",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := versions.scala,
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
  dependencyOverrides ++= Seq(
  )
)

lazy val core = Project(
  id = "delayed-webhooks-too-core",
  base = file("core")
)
  .settings(
    libraryDependencies ++= {
      import dependencies._
      Seq(
        `cats-core`,
        `cats-effect`,
        `http4s-client`,
        `http4s-dsl`,
        guava,
        `scala-test`,
        scalacheck,
        `scala-logging`,
        `logback-classic`
      )
    }
  )
  .settings(commonSettings: _*)

lazy val app = Project(
  id = "delayed-webhooks-too-app",
  base = file("app")
)
  .settings(
    libraryDependencies ++= {
      import dependencies._
      Seq(
        `http4s-server`,
        `http4s-circe`,
        `circe-generic`
      )
    },
    crossPaths := false
  )
  .settings(commonSettings: _*)
  .settings(Seq(
    dockerBaseImage := "openjdk:8-jdk-slim",
    dockerExposedPorts := List(8080),
    packageName in Docker := "delayed-webhooks-too",
//    version in Docker := "latest",
    maintainer in Docker := "Patrick Sy <suicide@get-it.us>",
    dockerUsername := Some("suicide"),
//    dockerRepository := Some("docker.io"),
    dockerUpdateLatest := true,
  ))
  .enablePlugins(JavaAppPackaging, UniversalDeployPlugin, DockerPlugin)
  .dependsOn(core)


lazy val `delayed-webhooks-too` = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(core, app)

