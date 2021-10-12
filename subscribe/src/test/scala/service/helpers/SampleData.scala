package service.helpers

import service.models.ApiModels.{InsertRepository, Repository, Subscription, SubscriptionsResponse}

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
}
