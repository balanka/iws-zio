val zioVersion                 = "2.0.8"
val zioHttpVersion             = "0.0.4"
val zioJsonVersion             = "0.3.0"
val zioConfigVersion           = "3.0.1"
val zioJdbcVersion             = "0.0.1"
val zioSqlVersion              = "0.1.2"
val logbackVersion             = "1.2.7"
val testcontainersVersion      = "1.17.5"
val testcontainersScalaVersion = "0.40.11"
val postgresql                 = "42.5.0"
val JwtCoreVersion             = "9.1.1"
val ZioSchemaVersion           = "0.4.1"


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
    ),
    name           := "iws-zio",
    libraryDependencies ++= Seq(
      // core
      "dev.zio"           %% "zio"                             % zioVersion,
      // sql
      "dev.zio"           %% "zio-sql"                         % zioSqlVersion,
      "dev.zio"           %% "zio-sql-postgres"                % zioSqlVersion,
      "dev.zio"            %% "zio-jdbc"                       % zioJdbcVersion,
      "dev.zio"             %% "zio-http"                       % zioHttpVersion,
      "dev.zio"             %% "zio-http"                       % zioHttpVersion % Test,
      "dev.zio"            %% "zio-schema"                      % ZioSchemaVersion,
        // config
      "dev.zio"           %% "zio-config"                      % zioConfigVersion,
      "dev.zio"           %% "zio-config-typesafe"             % zioConfigVersion,
      "dev.zio"           %% "zio-config-magnolia"             % zioConfigVersion,
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
      "org.testcontainers" % "jdbc"                            % testcontainersVersion      % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .enablePlugins(JavaAppPackaging)
