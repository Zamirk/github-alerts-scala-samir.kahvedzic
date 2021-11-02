import Dependencies.{libs, resolversList}

ThisBuild / scalaVersion := "2.13.7"

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test")

lazy val subscribe = (project in file("subscribe"))
  .settings(
    name := "subscribe",
    version := "0.0.1-SNAPSHOT",
    scalafmtOnly := true,
    libraryDependencies ++= libs,
    resolvers := resolversList,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / fork := true,
    IntegrationTest / parallelExecution := true,
    Test / fork := true,
    publish / skip := true
  )

lazy val `github-alerts` = (project in file("."))
  .settings(
    name := "github-alerts-scala-samir.kahvedzic",
    publish / skip := true
  )
  .aggregate(
    subscribe
  )
