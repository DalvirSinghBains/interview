name := "forex"
version := "1.0.0"

scalaVersion := "2.12.4"
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-Ypartial-unification",
  "-language:experimental.macros",
  "-language:implicitConversions"
)

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

val http4sVersion = "0.17.6"

libraryDependencies ++= Seq(
  "com.github.pureconfig"          %% "pureconfig"           % "0.7.2",
  "com.softwaremill.quicklens"     %% "quicklens"            % "1.4.11",
  "com.typesafe.akka"              %% "akka-actor"           % "2.4.19",
  "com.typesafe.akka"              %% "akka-http"            % "10.0.10",
  "de.heikoseeberger"              %% "akka-http-circe"      % "1.18.1",
  "io.circe"                       %% "circe-core"           % "0.8.0",
  "io.circe"                       %% "circe-generic"        % "0.8.0",
  "io.circe"                       %% "circe-generic-extras" % "0.8.0",
  "io.circe"                       %% "circe-java8"          % "0.8.0",
  "io.circe"                       %% "circe-jawn"           % "0.8.0",
  "org.atnos"                      %% "eff"                  % "4.5.0",
  "org.atnos"                      %% "eff-monix"            % "4.5.0",
  "org.typelevel"                  %% "cats-core"            % "0.9.0",
  "org.typelevel"                  %% "cats-effect"          % "0.2",
  "org.zalando"                    %% "grafter"              % "2.3.0",
  "ch.qos.logback"                 %  "logback-classic"      % "1.2.3",
  "com.typesafe.scala-logging"     %% "scala-logging"        % "3.7.2",
  "org.http4s"                     %% "http4s-dsl"           % http4sVersion,
  "org.http4s"                     %% "http4s-blaze-client"  % http4sVersion,
  "org.http4s"                     %% "http4s-circe"         % http4sVersion,
  "com.github.cb372"               %% "scalacache-caffeine"  % "0.24.2",

  compilerPlugin("org.spire-math"  %% "kind-projector"       % "0.9.4"),
  compilerPlugin("org.scalamacros" %% "paradise"             % "2.1.1" cross CrossVersion.full)
)
