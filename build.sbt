val zioVersion                 = "2.1.25"
val zioHttpVersion             = "3.11.1"
val zioJsonVersion             = "0.9.1"
val zioConfigVersion           = "4.0.5"
val logbackVersion             = "1.2.7"
val testcontainersVersion      = "1.21.3"
//val testcontainersScalaVersion = "0.41.4"
val testcontainersScalaVersion = "0.43.0"
val postgresql                 = "42.7.7"
val JwtCoreVersion             = "9.1.1"
val zioSchemaVersion           = "1.8.3"
val skunkVersion              = "0.6.5"
//val skunkVersion              = "1.0.0"
val zioPreludeVersion         = "1.0.0-RC47"
val zioInteropCatsVersion = "23.1.0.13"
val catsVersion           = "2.13.0"
val catsEffectVersion     = "3.7.0"


ThisBuild / resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
ThisBuild / scalacOptions ++= Seq("-Wunused:all","-Xmax-inlines",  "128")
maintainer := "batexy@gmail.com"
//dockerBaseImage := "openjdk:26-rc-slim"//"openjdk:26-ea-slim"
//dockerBaseImage := "eclipse-temurin:17-jdk-alpine"
dockerBaseImage := "eclipse-temurin:21-jre-alpine"
jlinkIgnoreMissingDependency := JlinkIgnore.everything
dockerEntrypoint := Seq("/opt/docker/jre/bin/java", "-jar", "/opt/docker/lib/iws-api.jar")

//assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x => MergeStrategy.first
//}
dockerBuildCommand := {
  if (sys.props("os.arch") == "amd64") {
  //if (sys.props("os.arch") != "amd64") {
    // use buildx with platform to build supported amd64 images on other CPU architectures
    // this may require that you have first run 'docker buildx create' to set docker buildx up
    dockerExecCommand.value ++ Seq("buildx", "build", "--platform=linux/amd64", "--load") ++ dockerBuildOptions.value :+ "."
  } else dockerBuildCommand.value
}
assemblyMergeStrategy := {
  case PathList("scala", "annotation", "unroll.class") => MergeStrategy.last
  case PathList("scala", "annotation", "unroll.tasty") => MergeStrategy.last
  case PathList("META-INF", "versions", "11", "module-info.class") => MergeStrategy.last
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
  case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.last
  case PathList("META-INF", "versions", "9", "OSGI-INF", "MANIFEST.MF") => MergeStrategy.last
  case x => (assemblyMergeStrategy.value)(x)
}
lazy val root = (project in file("."))
  .settings(
    Docker / packageName := "iws-api",
    Compile / mainClass := Some("com.kabasoft.iws.IwsApp"),
    //dockerEnvVars ++= Map(("BUILDPLATFORM", "linux/amd64")),
    inThisBuild(
      List(
        name         := "iws-skunk",
        organization := "kabasoft",
        version      := "2.5.4",
         scalaVersion := "3.8.3"
      )
    ),
    name           := "iws-zio",
    libraryDependencies ++= Seq(
      "dev.zio"           %% "zio"                           % zioVersion,
      "dev.zio"           %% "zio-streams"                    % zioVersion,
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


