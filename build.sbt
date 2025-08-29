val zioVersion                 = "2.1.20"
val zioHttpVersion             = "3.3.3"
val zioJsonVersion             = "0.7.44"
val zioConfigVersion           = "4.0.4"
val logbackVersion             = "1.2.7"
val testcontainersVersion      = "1.20.3"
val testcontainersScalaVersion = "0.41.4"
val postgresql                 = "42.7.4"
val JwtCoreVersion             = "9.1.1"
val zioSchemaVersion           = "1.6.6"
val skunkVersion              = "0.6.4"
//val skunkVersion              = "1.0.0-M7"
val zioPreludeVersion         = "1.0.0-RC39"
val zioInteropCatsVersion = "23.1.0.3"
val catsVersion           = "2.13.0"
val catsEffectVersion     = "3.5.7"


ThisBuild / resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
ThisBuild / scalacOptions ++= Seq("-Wunused:all","-Xmax-inlines",  "128")
maintainer := "batexy@gmail.com"
dockerBaseImage := "openjdk:26-slim"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

lazy val root = (project in file("."))
  .settings(
    Docker / packageName := "iws-api",
    Compile / mainClass := Some("com.kabasoft.iws.IwsApp"),
    dockerEnvVars ++= Map(("BUILDPLATFORM", "linux/amd64")),
    inThisBuild(
      List(
        name         := "iws-skunk",
        organization := "KABA Soft GmbH",
        version      := "1.8.6",
         scalaVersion := "3.7.1"
      )
    ),
    name           := "iws-zio",
    libraryDependencies ++= Seq(
      "dev.zio"           %% "zio"                             % zioVersion,
      "dev.zio"           %% "zio-streams"                      % zioVersion,
      "dev.zio"           %% "zio-http"                       % zioHttpVersion,
      "dev.zio"           %% "zio-schema"                      % zioSchemaVersion,
      "dev.zio"           %% "zio-config"                      % zioConfigVersion,
      "dev.zio"           %% "zio-config-typesafe"             % zioConfigVersion,
      "dev.zio"           %% "zio-config-magnolia"             % zioConfigVersion,
      //"dev.zio"           %% "zio-cache"                      % zioCacheVersion,
      "dev.zio"           %% "zio-json"                        % zioJsonVersion,
      "com.github.jwt-scala"   %% "jwt-core"                  % JwtCoreVersion,
      "org.tpolecat"     %% "skunk-core"                     % skunkVersion,
      "dev.zio"           %% "zio-prelude"                   % zioPreludeVersion,
      "dev.zio"           %% "zio-interop-cats"              % zioInteropCatsVersion,
      "org.typelevel"     %% "cats-core"                     % catsVersion,
      "org.typelevel"     %% "cats-effect"                   % catsEffectVersion,
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


