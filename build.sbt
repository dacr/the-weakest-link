name := "the-weakest-link"
organization := "org.dummy"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.github.pathikrit" %% "better-files"    % "3.4.0",
  "ch.qos.logback"        % "logback-classic" % "1.2.3",
  "org.scalatest"        %% "scalatest"       % "3.0.5" % "test",
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"             % "10.1.0",
  "io.kamon"          %% "kamon-core"            % "1.1.0",
  "io.kamon"          %% "kamon-akka-2.5"        % "1.0.1",
  "io.kamon"          %% "kamon-akka-http-2.5"   % "1.1.0",
  "io.kamon"          %% "kamon-akka-remote-2.5" % "1.0.1",
  "io.kamon"          %% "kamon-scala-future"    % "1.0.0",
  "io.kamon"          %% "kamon-jaeger"          % "1.0.1",
  "io.kamon"          %% "kamon-prometheus"      % "1.0.0",
  "io.kamon"          %% "kamon-logback"         % "1.0.0",
  "de.heikoseeberger" %% "akka-http-json4s"      % "1.20.0",
  "org.json4s"        %% "json4s-jackson"        % "3.5.3",
)
