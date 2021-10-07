import sbt.{ModuleID, _}

object Dependencies {

  object Version {
    val Http4sVersion          = "0.23.4"
    val CirceVersion           = "0.14.1"
    val MunitVersion           = "0.7.29"
    val LogbackVersion         = "1.2.6"
    val MunitCatsEffectVersion = "1.0.6"

    val catsEffect        = "3.1.0"
    val catsEffectTesting = "1.1.0"

    lazy val classicLogger = "1.0.13" //For logging
    lazy val pureconfig    = "0.15.0" //Used for reading config
    lazy val log4Cats      = "2.1.0"

    lazy val ScalaTestVersion = "3.2.10"
    lazy val ScalaMockVersion = "4.4.0"

    lazy val libs: Seq[ModuleID] = {
      Seq(
        "org.http4s" %% "http4s-blaze-server" % Version.Http4sVersion,
        "org.http4s" %% "http4s-blaze-client" % Version.Http4sVersion,
        //      "org.http4s"    %% "http4s-ember-server" % Http4sVersion, //Blaze vs ember?
        //      "org.http4s"    %% "http4s-ember-client" % Http4sVersion,
        "org.http4s"    %% "http4s-circe"    % Version.Http4sVersion,
        "io.circe"      %% "circe-generic"   % Version.CirceVersion,
        "io.circe"      %% "circe-parser"    % Version.CirceVersion,
        "io.circe"      %% "circe-core"      % Version.CirceVersion,
        "org.http4s"    %% "http4s-dsl"      % Version.Http4sVersion,
        "ch.qos.logback" % "logback-classic" % Version.LogbackVersion,
        "org.scalameta" %% "svm-subs"        % "20.2.0"
      ) ++ log4cats ++ pureConfug ++ testing
    }

    lazy val testing: Seq[ModuleID] = List(
      "org.scalatest" %% "scalatest" % Version.ScalaTestVersion % "it,test",
      "org.scalamock" %% "scalamock" % Version.ScalaMockVersion % "it,test",
//      "org.typelevel" %% "cats-effect-laws"              % Version.catsEffect,
      "org.typelevel" %% "cats-effect-testing-scalatest" % Version.catsEffectTesting,
      "org.typelevel" %% "cats-effect-testkit"           % Version.catsEffect
    )

    lazy val pureConfug: Seq[ModuleID] = List(
      "com.github.pureconfig" %% "pureconfig"             % Version.pureconfig,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % Version.pureconfig
    )

    lazy val log4cats: Seq[ModuleID] = List(
      "org.typelevel" %% "log4cats-core"  % Version.log4Cats,
      "org.typelevel" %% "log4cats-slf4j" % Version.log4Cats,
      // Classic logger is required for log4cats to work
      "ch.qos.logback" % "logback-classic" % Version.classicLogger
    )

  }

}
