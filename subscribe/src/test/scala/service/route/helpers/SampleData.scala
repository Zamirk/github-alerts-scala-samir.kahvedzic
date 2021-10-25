package service.route.helpers

import org.http4s.UrlForm
import service.domain.ApiModels.Subscription.{Organisation, Repositoryy}
import service.domain.ApiModels._

import java.time.{OffsetDateTime, ZoneOffset}

object SampleData {
  val offsetDatTime = OffsetDateTime.of(2000, 10, 10, 10, 10, 10, 10, ZoneOffset.ofHours(1))

  val sampleDataOne = {
    RepoCarrier(
      Repository(
        "47deg",
        "thool"
      )
    )
  }

  val dataToInsert2 = {
    RepoCarrier(
      Repository(
        "47deg",
        "thool2"
      )
    )
  }

  val sampleData3 = {
    RepoCarrier(
      Repository(
        "higherkindness",
        "skeuomorph"
      )
    )
  }

  val nonExistantOrganisation = {
    RepoCarrier(
      Repository(
        "werwerwer",
        "thool"
      )
    )
  }
  val sampleData =
    SubscriptionsResponse(
      List(
        Subscription(
          Organisation("47deg"),
          Repositoryy("thool"),
          offsetDatTime
        ),
        Subscription(
          Organisation("higherkindness"),
          Repositoryy("skeuomorph"),
          offsetDatTime
        ),
        Subscription(
          Organisation("47degrees"),
          Repositoryy("github4s"),
          offsetDatTime
        )
      )
    )

  val data = SlackResponseBody(
    ResponseField.InChannel,
    "It's 80 degrees right now.",
    List("*It's 80 degrees right now.*", "Partly cloudy today and tomorrow").map(m =>
      SlackMessageBlock1(
        "section",
        SlackMessageBlock2(
          "mrkdwn",
          m
        )
      )
    )
  )

  lazy val validUrlEncodedData: UrlForm = UrlForm(
    "token"           -> "gIkuvaNzQIHg97ATvDxqgjtO",
    "team_id"         -> "T0001",
    "team_domain"     -> "VTSL",
    "enterprise_id"   -> "E0001",
    "enterprise_name" -> "Globular%20Construct%20Inc",
    "channel_id"      -> "C2147483705",
    "channel_name"    -> "test",
    "user_id"         -> "U2147483697",
    "user_name"       -> "Steve",
    "command"         -> "/weather",
    "text"            -> "94070",
    "response_url"    -> "https://hooks.slack.com/commands/1234/5678",
    "trigger_id"      -> "13345224609.738474920.8088930838d88f008e0",
    "api_app_id"      -> "A123456"
  )

  lazy val invalidUrlEncodedData: UrlForm = UrlForm(
    "tokentoke" -> "toke"
  )
}
