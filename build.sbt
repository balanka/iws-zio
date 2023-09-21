val zioVersion                 = "2.0.17"
//val zioHttpVersion             = "3.0.0-RC1"
//val zioHttpVersion             = "0.0.5"
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
dockerBaseImage := "adoptopenjdk:11-jre-hotspot"

//assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x => MergeStrategy.first
//}

lazy val root = (project in file("."))
  .settings(
    Docker / packageName := "iws",
    Compile / mainClass := Some("com.kabasoft.iws.Main"),

    //dockerEnvVars ++= Map(("IWS_NODE_HOST", "localhost"), ("IWS_NODE_PORT", "3000"), ("IWS_API_HOST", "192.168.1.6"), ("IWS_API_PORT", "8080")),
    //dockerExposedPorts ++= Seq(8080),
    dockerExposedVolumes := Seq("/var/lib/postgresql/data", "/tmp/datax"),
    //assembly / assemblyJarName := "iws-1.0.0.jar",
    //assembly / mainClass := Some("com.kabasoft.iws.Main"),
    //assembly / logLevel := Level.Debug,
    inThisBuild(
      List(
        name         := "iws-zio",
        organization := "KABA SoftGmbH",
        version      := "1.0.0",
        scalaVersion := "2.13.10"
         //scalaVersion := "3.1.1"
      )
    ),//++ScalaSettings.scala213Settings,
    name           := "iws-zio",
    libraryDependencies ++= Seq(
      // core
      "dev.zio"           %% "zio"                             % zioVersion,
      "dev.zio"           %% "zio-streams"                      % zioVersion,
      // sql
      "dev.zio"           %% "zio-sql"                         % zioSqlVersion,
      "dev.zio"           %% "zio-sql-postgres"                % zioSqlVersion,
      "dev.zio"             %% "zio-http"                       % zioHttpVersion,
      //"dev.zio"             %% "zio-http"                       % zioHttpVersion % Test,
      "dev.zio"            %% "zio-schema"                      % zioSchemaVersion,
        // config
      "dev.zio"           %% "zio-config"                      % zioConfigVersion,
      "dev.zio"           %% "zio-config-typesafe"             % zioConfigVersion,
      "dev.zio"           %% "zio-config-magnolia"             % zioConfigVersion,
      "dev.zio"           %% "zio-cache"                      % zioCacheVersion,
      "dev.zio"           %% "zio-query"                      % zioQueryVersion,
      // json
      "dev.zio"           %% "zio-json"                        % zioJsonVersion,
       "com.github.jwt-scala"   %% "jwt-core"                  % JwtCoreVersion,
      // test dependencies

      "dev.zio"           %% "zio-test"                        % zioVersion                 % Test,
      "dev.zio"           %% "zio-test-sbt"                    % zioVersion                 % Test,
      "dev.zio"           %% "zio-test-junit"                  % zioVersion                 % Test,
      "org.postgresql"     % "postgresql"                      % postgresql,
      "com.dimafeng"      %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % Test,
      "org.testcontainers" % "testcontainers"                  % testcontainersVersion      % Test,
      "org.testcontainers" % "database-commons"                % testcontainersVersion      % Test,
      "org.testcontainers" % "postgresql"                      % testcontainersVersion      % Test,
      //"org.testcontainers" % "jdbc"                            % testcontainersVersion      % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)


