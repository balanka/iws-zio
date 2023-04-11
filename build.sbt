val zioVersion                 = "2.0.11"
//val zioHttpVersion             = "0.0.4+36-dab4ab27-SNAPSHOT"
val zioHttpVersion             = "0.0.5"
//val zioHttpVersion             = "0.0.4+32-51285fc4+20230301-2319-SNAPSHOT"
val zioJsonVersion             = "0.3.0"
val zioConfigVersion           = "3.0.7"
val zioJdbcVersion             = "0.0.1"
val zioSqlVersion              = "0.1.2"
val logbackVersion             = "1.2.7"
val testcontainersVersion      = "1.17.5"
val testcontainersScalaVersion = "0.40.11"
val postgresql                 = "42.5.0"
val JwtCoreVersion             = "9.1.1"
val zioSchemaVersion           = "0.4.2"
val zioCacheVersion           = "0.2.2"
val zioQueryVersion           = "0.4.0"

ThisBuild / resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
ThisBuild / scalacOptions += "-Wconf:any:wv"

lazy val root = (project in file("."))
  .settings(
    inThisBuild(
      List(
        name         := "iws-zio",
        organization := "KABA SoftGmbH",
        version      := "0.9",
        scalaVersion := "2.13.9"
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
  .enablePlugins(JavaAppPackaging)
