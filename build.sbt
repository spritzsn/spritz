name := "spritz"

version := "0.0.35"

versionScheme := Some("early-semver")

scalaVersion := "3.2.0"

enablePlugins(ScalaNativePlugin)

nativeLinkStubs := true

nativeMode := "debug"

nativeLinkingOptions := Seq(s"-L${baseDirectory.value}/native-lib")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:existentials",
  "-language:dynamics",
)

organization := "io.github.spritzsn"

githubOwner := "spritzsn"

githubRepository := name.value

Global / onChangedBuildSource := ReloadOnSourceChanges

resolvers += Resolver.githubPackages("edadma")

licenses := Seq("ISC" -> url("https://opensource.org/licenses/ISC"))

homepage := Some(url("https://github.com/edadma/" + name.value))

//libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.13" % "test"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.1.1",
)

libraryDependencies ++= Seq(
//  "com.lihaoyi" %%% "pprint" % "0.7.2", /*% "test"*/
  "io.github.cquiroz" % "scala-java-time_native0.4_3" % "2.4.0",
)

libraryDependencies ++= Seq(
  "io.github.edadma" %%% "json" % "0.1.13",
)

libraryDependencies ++= Seq(
  "io.github.spritzsn" %%% "libuv" % "0.0.24",
  "io.github.spritzsn" %%% "async" % "0.0.11",
)

publishMavenStyle := true

Test / publishArtifact := false
