ThisBuild / scalaVersion := "3.8.4"

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "gym-tracker-pwa",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io" % "0.11.0",
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test,
      "org.scalacheck" %%% "scalacheck" % "1.18.1" % Test
    )
  )
