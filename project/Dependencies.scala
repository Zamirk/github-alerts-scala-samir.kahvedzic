import sbt._

object Dependencies {

  object Version {
    lazy val http4sVersion = "0.23.4"
    lazy val circe         = "0.14.1"
    lazy val logback       = "1.2.6"

    lazy val catsEffect        = "3.1.0"
    lazy val catsEffectTesting = "1.1.0"

    lazy val classicLogger = "1.0.13"
    lazy val pureconfig    = "0.15.0"
    lazy val log4Cats      = "2.1.0"

    lazy val scalaTest = "3.2.10"
    lazy val scalaMock = "4.4.0"

    lazy val flyway = "7.5.2"
    lazy val doobie = "1.0.0-RC1"
  }

  lazy val libs: Seq[ModuleID] = {
    Seq(
      "org.http4s"    %% "http4s-blaze-server" % Version.http4sVersion,
      "org.http4s"    %% "http4s-blaze-client" % Version.http4sVersion,
      "org.http4s"    %% "http4s-circe"        % Version.http4sVersion,
      "io.circe"      %% "circe-generic"       % Version.circe,
      "io.circe"      %% "circe-parser"        % Version.circe,
      "io.circe"      %% "circe-core"          % Version.circe,
      "org.http4s"    %% "http4s-dsl"          % Version.http4sVersion,
      "ch.qos.logback" % "logback-classic"     % Version.logback,
      "org.scalameta" %% "svm-subs"            % "20.2.0"
    ) ++ log4cats ++ pureConfug ++ testing ++ flyway ++ doobie
  }

  lazy val testing: Seq[ModuleID] = List(
    "org.scalatest" %% "scalatest"                     % Version.scalaTest % "it,test",
    "org.scalamock" %% "scalamock"                     % Version.scalaMock % "it,test",
    "org.typelevel" %% "cats-effect-testing-scalatest" % Version.catsEffectTesting,
    "org.typelevel" %% "cats-effect-testkit"           % Version.catsEffect
  )

  lazy val doobie: Seq[ModuleID] = List(
    "org.tpolecat" %% "doobie-core"     % Version.doobie,
    "org.tpolecat" %% "doobie-h2"       % Version.doobie, // H2 driver 1.4.200 + type mappings.
    "org.tpolecat" %% "doobie-hikari"   % Version.doobie, // HikariCP transactor.
    "org.tpolecat" %% "doobie-postgres" % Version.doobie // Postgres driver 42.2.19 + type mappings
  )

  lazy val pureConfug: Seq[ModuleID] = List(
    "com.github.pureconfig" %% "pureconfig"             % Version.pureconfig,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % Version.pureconfig
  )

  lazy val log4cats: Seq[ModuleID] = List(
    "org.typelevel" %% "log4cats-noop"   % Version.log4Cats,
    "org.typelevel" %% "log4cats-core"   % Version.log4Cats,
    "org.typelevel" %% "log4cats-slf4j"  % Version.log4Cats,
    "ch.qos.logback" % "logback-classic" % Version.classicLogger
  )

  lazy val flyway: Seq[ModuleID] = List(
    "org.flywaydb" % "flyway-core" % Version.flyway
  )
}
