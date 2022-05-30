//val zioSqlVersion = "0.0.2"
val zioSqlVersion = "0.0.2+82-b1e773bc+20220526-2224-SNAPSHOT"

val zioVersion = "2.0.0-RC6"
val zioHttpVersion = "2.0.0-RC7"
val zioJsonVersion = "0.3.0-RC7"
val zioConfigVersion = "3.0.0-RC8"

val logbackVersion = "1.2.7"
val testcontainersVersion = "1.16.2"
val testcontainersScalaVersion = "0.39.12"

lazy val root = (project in file("."))
  .settings(
    inThisBuild(
      List(
        name := "zio-sql-example",
        organization := "sviezypan",
        version := "0.02",
        scalaVersion := "2.13.8"
        // scalaVersion := "3.0.0"
      )
    ),
    name := "zio-sql-example",
    libraryDependencies ++= Seq(
      //core
      "dev.zio" %% "zio" % zioVersion,
      //sql
      "dev.zio" %% "zio-sql" % zioSqlVersion,

      "dev.zio" %% "zio-sql-postgres" % zioSqlVersion,
      //http
      "io.d11" %% "zhttp" % zioHttpVersion,
      "io.d11" %% "zhttp-test" % zioHttpVersion % Test,
      //config
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      //json
      "dev.zio" %% "zio-json" % zioJsonVersion,
      // test dependencies
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test-junit" % zioVersion % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % Test,
      "org.testcontainers" % "testcontainers" % testcontainersVersion % Test,
      "org.testcontainers" % "database-commons" % testcontainersVersion % Test,
      "org.testcontainers" % "postgresql" % testcontainersVersion % Test,
      "org.testcontainers" % "jdbc" % testcontainersVersion % Test,
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .enablePlugins(JavaAppPackaging)
