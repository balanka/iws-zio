val zioVersion                 = "2.0.20"

val zioHttpVersion             = "3.0.0-RC2"
val zioJsonVersion             = "0.3.0"
val zioConfigVersion           = "3.0.7"
val zioJdbcVersion             = "0.0.1"
val zioSqlVersion              = "0.1.2"
val logbackVersion             = "1.2.7"
val testcontainersVersion      = "1.17.5"
val testcontainersScalaVersion = "0.40.11"
val postgresql                 = "42.6.0"
val JwtCoreVersion             = "9.1.1"
val zioSchemaVersion           = "0.4.2"
val zioCacheVersion           = "0.2.3"
val zioQueryVersion           = "0.4.0"

ThisBuild / resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
ThisBuild / scalacOptions += "-Wconf:any:wv"
maintainer := "batexy@gmail.com"
//dockerBaseImage := "adoptopenjdk:11-jre-hotspot"
//dockerBaseImage := "openjdk:17-alpine"
//dockerBaseImage := "eclipse-temurin:21-alpine"
//dockerBaseImage := "amazoncorretto:21.0.2-alpine"
dockerBaseImage := "openjdk:21-jdk"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

lazy val root = (project in file("."))
  .settings(
    Docker / packageName := "iws",
    Compile / mainClass := Some("com.kabasoft.iws.Main"),
    inThisBuild(
      List(
        name         := "iws-zio",
        organization := "KABA Soft GmbH",
        version      := "1.2.4",
        scalaVersion := "2.13.10"
         //scalaVersion := "3.1.1"
      )
    ),
    name           := "iws-zio",
    libraryDependencies ++= Seq(
      "dev.zio"           %% "zio"                             % zioVersion,
      "dev.zio"           %% "zio-streams"                      % zioVersion,
      "dev.zio"           %% "zio-sql"                         % zioSqlVersion,
      "dev.zio"           %% "zio-sql-postgres"                % zioSqlVersion,
      "dev.zio"           %% "zio-http"                       % zioHttpVersion,
      "dev.zio"           %% "zio-schema"                      % zioSchemaVersion,
      "dev.zio"           %% "zio-config"                      % zioConfigVersion,
      "dev.zio"           %% "zio-config-typesafe"             % zioConfigVersion,
      "dev.zio"           %% "zio-config-magnolia"             % zioConfigVersion,
      "dev.zio"           %% "zio-cache"                      % zioCacheVersion,
      "dev.zio"           %% "zio-query"                      % zioQueryVersion,
      "dev.zio"           %% "zio-json"                        % zioJsonVersion,
       "com.github.jwt-scala"   %% "jwt-core"                  % JwtCoreVersion,
      "dev.zio"           %% "zio-test"                        % zioVersion                 % Test,
      "dev.zio"           %% "zio-test-sbt"                    % zioVersion                 % Test,
      "dev.zio"           %% "zio-test-junit"                  % zioVersion                 % Test,
      "org.postgresql"    % "postgresql"                      % postgresql,
      "com.dimafeng"      %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % Test,
      "org.testcontainers" % "testcontainers"                  % testcontainersVersion      % Test,
      "org.testcontainers" % "database-commons"                % testcontainersVersion      % Test,
      "org.testcontainers" % "postgresql"                      % testcontainersVersion      % Test,
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  //.enablePlugins(JavaAppPackaging, DockerPlugin, JlinkPlugin)


