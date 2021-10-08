import sbt._

object Dependencies {

  object Version {
    lazy val Http4sVersion  = "0.23.4"
    lazy val CirceVersion   = "0.14.1"
    lazy val LogbackVersion = "1.2.6"

    lazy val catsEffect        = "3.1.0"
    lazy val catsEffectTesting = "1.1.0"

    lazy val classicLogger = "1.0.13"
    lazy val pureconfig    = "0.15.0"
    lazy val log4Cats      = "2.1.0"

    lazy val ScalaTestVersion = "3.2.10"
    lazy val ScalaMockVersion = "4.4.0"

    lazy val libs: Seq[ModuleID] = {
      Seq(
        "org.http4s"    %% "http4s-blaze-server" % Version.Http4sVersion,
        "org.http4s"    %% "http4s-blaze-client" % Version.Http4sVersion,
        "org.http4s"    %% "http4s-circe"        % Version.Http4sVersion,
        "io.circe"      %% "circe-generic"       % Version.CirceVersion,
        "io.circe"      %% "circe-parser"        % Version.CirceVersion,
        "io.circe"      %% "circe-core"          % Version.CirceVersion,
        "org.http4s"    %% "http4s-dsl"          % Version.Http4sVersion,
        "ch.qos.logback" % "logback-classic"     % Version.LogbackVersion,
        "org.scalameta" %% "svm-subs"            % "20.2.0"
      ) ++ log4cats ++ pureConfug ++ testing
    }

    lazy val testing: Seq[ModuleID] = List(
      "org.scalatest" %% "scalatest"                     % Version.ScalaTestVersion % "it,test",
      "org.scalamock" %% "scalamock"                     % Version.ScalaMockVersion % "it,test",
      "org.typelevel" %% "cats-effect-testing-scalatest" % Version.catsEffectTesting,
      "org.typelevel" %% "cats-effect-testkit"           % Version.catsEffect
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
  }
}
