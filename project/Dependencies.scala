import sbt._

object Dependencies {

  object Version {
    lazy val http4sVersion = "0.23.6"
    lazy val circe         = "0.14.1"
    lazy val logback       = "1.2.6"

    lazy val catsEffect           = "3.1.1"
    lazy val catsEffectTesting    = "1.1.0"
    lazy val catsTestkitScalatest = "2.1.5"

    lazy val classicLogger = "1.2.6"
    lazy val pureconfig    = "0.17.0"
    lazy val log4Cats      = "2.1.0"

    lazy val scalaTest = "3.2.10"
    lazy val scalaMock = "4.4.0"

    lazy val flyway = "7.15.0"
    lazy val doobie = "1.0.0-RC1"

    lazy val fs2KafkaVer                        = "2.2.0"
    lazy val embeddedKafkaVer                   = "2.8.1"
    lazy val embeddedkafkaSchemaRegistryVersion = "6.2.1"
  }

  lazy val resolversList: List[MavenRepository] =
    List(
      "Artifactory" at "https://kaluza.jfrog.io/artifactory/maven",
      "confluent" at "https://packages.confluent.io/maven/",
      "jitpack" at "https://jitpack.io"
    )

  lazy val kafkaLibraries: Seq[ModuleID] = Seq(
    "com.github.fd4s" %% "fs2-kafka-vulcan" % Version.fs2KafkaVer,
    "com.github.fd4s" %% "fs2-kafka"        % Version.fs2KafkaVer
  )

  lazy val kafkaTesting: Seq[ModuleID] = Seq(
    "io.github.embeddedkafka" %% "embedded-kafka"                 % Version.embeddedKafkaVer                   % Test,
    "io.github.embeddedkafka" %% "embedded-kafka-schema-registry" % Version.embeddedkafkaSchemaRegistryVersion % Test
  )

  lazy val libs: Seq[ModuleID] =
    http4s ++
      doobie ++
      flyway ++
      circe ++
      pureConfug ++
      log4cats ++
      testing ++
      doobieScalatest ++
      catsTestkitScalatest ++
      kafkaLibraries ++
      kafkaTesting

  lazy val http4s: Seq[ModuleID] = List(
    "org.http4s" %% "http4s-blaze-server" % Version.http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % Version.http4sVersion,
    "org.http4s" %% "http4s-dsl"          % Version.http4sVersion
  )

  lazy val testing: Seq[ModuleID] = List(
    "org.scalatest" %% "scalatest"                     % Version.scalaTest         % "it, test",
    "org.scalamock" %% "scalamock"                     % Version.scalaMock         % "it, test",
    "org.typelevel" %% "cats-effect-testing-scalatest" % Version.catsEffectTesting % "it, test",
    "org.typelevel" %% "cats-effect-testkit"           % Version.catsEffect        % "it, test"
  )

  lazy val circe: Seq[ModuleID] = List(
    "org.http4s" %% "http4s-circe"  % Version.http4sVersion,
    "io.circe"   %% "circe-generic" % Version.circe,
    "io.circe"   %% "circe-parser"  % Version.circe,
    "io.circe"   %% "circe-core"    % Version.circe
  )

  lazy val doobie: Seq[ModuleID] = List(
    "org.tpolecat" %% "doobie-core"     % Version.doobie,
    "org.tpolecat" %% "doobie-h2"       % Version.doobie, // H2 driver 1.4.200 + type mappings.
    "org.tpolecat" %% "doobie-hikari"   % Version.doobie, // HikariCP transactor.
    "org.tpolecat" %% "doobie-postgres" % Version.doobie // Postgres driver 42.2.19 + type mappings
  )

  lazy val doobieScalatest: Seq[ModuleID] = List(
    "org.tpolecat" %% "doobie-scalatest" % Version.doobie % "it, test"
  )

  lazy val catsTestkitScalatest: Seq[ModuleID] = List(
    "org.typelevel" %% "cats-effect"            % Version.catsEffect,
    "org.typelevel" %% "cats-effect-laws"       % Version.catsEffect,
    "org.typelevel" %% "cats-testkit-scalatest" % Version.catsTestkitScalatest % "it, test"
  )

  lazy val pureConfug: Seq[ModuleID] = List(
    "com.github.pureconfig" %% "pureconfig"             % Version.pureconfig,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % Version.pureconfig
  )

  lazy val log4cats: Seq[ModuleID] = List(
    "org.typelevel" %% "log4cats-noop"   % Version.log4Cats,
    "org.typelevel" %% "log4cats-core"   % Version.log4Cats,
    "org.typelevel" %% "log4cats-slf4j"  % Version.log4Cats,
    "ch.qos.logback" % "logback-classic" % Version.classicLogger,
    "org.scalameta" %% "svm-subs"        % "20.2.0"
  )

  lazy val flyway: Seq[ModuleID] = List(
    "org.flywaydb" % "flyway-core" % Version.flyway
  )
}
