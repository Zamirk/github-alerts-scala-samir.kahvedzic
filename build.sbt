import Dependencies.libs

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test")

lazy val subscribe = (project in file("subscribe"))
  .settings(
    name := "subscribe",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.6",
    scalafmtOnly := true,
    libraryDependencies ++= libs,
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.0" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / fork := true,
    IntegrationTest / parallelExecution := false,
    fork in Test := true,
    publish / skip := true
  )
