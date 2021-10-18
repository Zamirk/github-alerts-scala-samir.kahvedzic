package service.route.helpers

import org.http4s.UrlForm
import service.domain.ApiModels.{InsertRepository, Repository, Subscription, SubscriptionsResponse}

object SampleData {

  val sampleDataOne = {
    InsertRepository(
      Repository(
        "47deg",
        "thool"
      )
    )
  }

  val dataToInsert2 = {
    InsertRepository(
      Repository(
        "47deg",
        "thool2"
      )
    )
  }

  val sampleData3 = {
    InsertRepository(
      Repository(
        "higherkindness",
        "skeuomorph"
      )
    )
  }

  val nonExistantOrganisation = {
    InsertRepository(
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
          "47deg",
          "thool",
          "2021-04-15T12:30:15"
        ),
        Subscription(
          "higherkindness",
          "skeuomorph",
          "2021-04-26T12:45:25"
        ),
        Subscription(
          "47degrees",
          "github4s",
          "2021-05-01T09:15:05"
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
