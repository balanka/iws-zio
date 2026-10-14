val zioVersion                 = "2.1.26"
val zioHttpVersion             = "3.11.6"
val zioJsonVersion             = "1.0.0"
val zioConfigVersion           = "4.0.5"
val logbackVersion             = "1.2.7"
val testcontainersVersion      = "1.21.3"
//val testcontainersScalaVersion = "0.41.4"
val testcontainersScalaVersion = "0.43.0"
val postgresql                 = "42.7.7"
val JwtCoreVersion             = "9.1.1"
val zioSchemaVersion           = "1.8.7"
val zioSchemaJsonVersion       = "1.8.7"
val skunkVersion              = "0.6.5"
//val skunkVersion              = "2.0.0-RC2"
val zioPreludeVersion         = "1.0.0-RC48"
val zioInteropCatsVersion = "23.1.0.13"
val catsVersion           = "2.13.0"
val catsEffectVersion     = "3.7.0"

ThisBuild / resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
ThisBuild / scalacOptions ++= Seq("-Wunused:all","-Xmax-inlines",  "128")
maintainer := "batexy@gmail.com"
//dockerBaseImage := "openjdk:26-rc-slim"//"openjdk:26-ea-slim"
dockerBaseImage := "eclipse-temurin:25.0.2_10-jre-alpine-3.21"
//dockerBaseImage := "eclipse-temurin:24-jre-alpine"
//Docker / dockerBaseImage := "eclipse-temurin:24-jre-alpine"
jlinkIgnoreMissingDependency := JlinkIgnore.everything
dockerEntrypoint := Seq("/opt/docker/jre/bin/java", "-jar", "/opt/docker/lib/iws-api.jar")

dockerBuildCommand := {
  //if (sys.props("os.arch") == "amd64") {
  if (sys.props("os.arch") != "amd64") {
    // use buildx with platform to build supported amd64 images on other CPU architectures
    // this may require that you have first run 'docker buildx create' to set docker buildx up
    dockerExecCommand.value ++ Seq("buildx", "build", "--platform=linux/amd64", "--load") ++ dockerBuildOptions.value :+ "."
  } else dockerBuildCommand.value
}
assembly / assemblyMergeStrategy := {
  val old = (assembly / assemblyMergeStrategy).value
  (path: String) =>
    if (path.contains("META-INF/versions/") && (path.endsWith("module-info.class") || path.contains("OSGI-INF/MANIFEST.MF")))
      MergeStrategy.discard
    else if (path == "META-INF/io.netty.versions.properties")
      MergeStrategy.first
    else if (path == "scala/annotation/unroll.class" || path == "scala/annotation/unroll.tasty")
      MergeStrategy.first
    else
      old(path)
}

lazy val root = (project in file("."))
  .settings(
    Docker / packageName := "iws-api",
    Compile / mainClass := Some("com.kabasoft.iws.IwsApp"),
    //dockerEnvVars ++= Map(("BUILDPLATFORM", "linux/amd64")),
    inThisBuild(
      List(
        name         := "iws-api",
        organization := "KABA Soft GmbH",
        version := sys.props.getOrElse("app.version",
          sys.env.getOrElse("APP_VERSION", "4.0.5")),
         scalaVersion := "3.9.0"
      )
    ),
    name           := "iws-api",
      nativeImageCommand := Seq(s"${sys.env("JAVA_HOME")}/bin/native-image"),
    ThisBuild / libraryDependencySchemes += "dev.zio" %% "zio-json" % VersionScheme.Always,

      libraryDependencies ++= Seq(
      "dev.zio"           %% "zio"                 % zioVersion,
      "dev.zio"           %% "zio-streams"         % zioVersion,
      "dev.zio"           %% "zio-http"            % zioHttpVersion exclude("io.netty", "netty-pkitesting"),
      "dev.zio"           %% "zio-schema"          % zioSchemaVersion,
      "dev.zio"           %% "zio-schema-json"      % zioSchemaJsonVersion,
      "dev.zio"           %% "zio-config"           % zioConfigVersion,
      "dev.zio"           %% "zio-config-typesafe"   % zioConfigVersion,
      "dev.zio"           %% "zio-config-magnolia"   % zioConfigVersion,
      //"dev.zio"           %% "zio-cache"                      % zioCacheVersion,
      "dev.zio"           %% "zio-json"              % zioJsonVersion,
      "com.github.jwt-scala"   %% "jwt-core"          % JwtCoreVersion,
      "org.tpolecat"     %% "skunk-core"              % skunkVersion,
      "dev.zio"           %% "zio-prelude"             % zioPreludeVersion,
      "dev.zio"           %% "zio-interop-cats"        % zioInteropCatsVersion,
      "org.typelevel"     %% "cats-core"               % catsVersion,
      "org.typelevel"     %% "cats-effect"             % catsEffectVersion,
      "dev.zio"           %% "zio-test"                % zioVersion       % Test,
      "dev.zio"           %% "zio-test-sbt"            % zioVersion       % Test,
      "dev.zio"           %% "zio-test-junit"          % zioVersion       % Test,
      "org.postgresql"    % "postgresql"                % postgresql,
      "com.dimafeng"      %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % Test,
      "org.testcontainers" % "testcontainers"                  % testcontainersVersion      % Test,
      "org.testcontainers" % "database-commons"                % testcontainersVersion      % Test,
      "org.testcontainers" % "postgresql"                      % testcontainersVersion      % Test,
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .enablePlugins(JavaAppPackaging, DockerPlugin, NativeImagePlugin)

enablePlugins(NativeImagePlugin)

nativeImageOptions ++= Seq(
  "--no-fallback",
  "-H:+ReportExceptionStackTraces",
  "-H:ConfigurationFileDirectories=graal/",
  "-H:IncludeResources=application\\.conf",
  "-H:IncludeResources=reference\\.conf",
  "-H:Log=registerResource:3",
  "--enable-url-protocols=http,https",
  "--initialize-at-build-time=scala,org.slf4j",
  "--initialize-at-run-time=io.netty,com.zaxxer",
  "-J-Xmx4g"
)
nativeImageOutput := baseDirectory.value / "target" / "native-image" / "iws-api"




