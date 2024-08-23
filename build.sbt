import com.typesafe.sbt.packager.docker.{Cmd, DockerAlias}

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
organization := "com.codacy"
scalaVersion := "2.13.14"
name := "codacy-metrics-radon"
// App Dependencies
libraryDependencies ++= Seq("com.codacy" %% "codacy-metrics-scala-seed" % "0.3.2",
                            "org.specs2" %% "specs2-core" % "4.20.8" % Test)

mappings in Universal ++= {
  (resourceDirectory in Compile).map { resourceDir: File =>
    val src = resourceDir / "docs"
    val dest = "/docs"

    for {
      path <- src.allPaths.get if !path.isDirectory
    } yield path -> path.toString.replaceFirst(src.toString, dest)
  }
}.value

val radonVersion = scala.io.Source.fromFile(".radon-version").mkString.trim

Docker / packageName := packageName.value
dockerBaseImage := "python:3.12-alpine3.19"
Docker / daemonUser := "docker"
Docker / daemonGroup := "docker"
dockerEntrypoint := Seq(s"/opt/docker/bin/${name.value}")
dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("ADD", _) =>
    List(Cmd("RUN", "adduser -u 2004 -D docker"),
         cmd,
         Cmd("RUN", s"""apk update &&
               | apk upgrade &&
               | apk add --no-cache bash &&
               | apk add --no-cache --virtual=build-dependencies unzip &&
               | apk add --no-cache curl &&
               | apk add --no-cache openjdk8-jre &&
               |python -m ensurepip &&
               |python -m pip install -I -U --no-cache-dir radon==$radonVersion &&
               |rm -rf /tmp/* &&
               |rm -rf /var/cache/apk/*""".stripMargin.replaceAll(System.lineSeparator(), " ")),
         Cmd("RUN", "mv /opt/docker/docs /docs"))

  case other => List(other)
}
