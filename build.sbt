import Dependencies.Version.libs

lazy val subscribe = (project in file("subscribe"))
  .settings(
    name := "subscribe",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.6",
    scalafmtOnly := true,
    libraryDependencies ++= libs,
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.0" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )
