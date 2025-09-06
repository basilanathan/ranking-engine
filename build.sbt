ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.example"

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.17" % Test
  ),
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Wunused:imports"
  )
)

lazy val root = (project in file("."))
  .settings(
    name := "ranking-engine"
  )
  .aggregate(rankingCore, rankingConfig, rankingApi, rankingTests)

lazy val rankingCore = (project in file("ranking-core"))
  .settings(
    commonSettings,
    name := "ranking-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.9.0",
      "com.lihaoyi" %% "fastparse" % "3.0.2"
    )
  )

lazy val rankingConfig = (project in file("ranking-config"))
  .settings(
    commonSettings,
    name := "ranking-config",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-yaml" % "0.14.2",
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6"
    )
  )
  .dependsOn(rankingCore)

lazy val rankingApi = (project in file("ranking-api"))
  .settings(
    commonSettings,
    name := "ranking-api",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % "0.23.23",
      "org.http4s" %% "http4s-dsl" % "0.23.23",
      "org.http4s" %% "http4s-circe" % "0.23.23",
      "com.github.scopt" %% "scopt" % "4.1.0"
    )
  )
  .dependsOn(rankingCore, rankingConfig)

lazy val rankingTests = (project in file("ranking-tests"))
  .settings(
    commonSettings,
    name := "ranking-tests",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.17",
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.17.0"
    )
  )
  .dependsOn(rankingCore, rankingConfig, rankingApi)