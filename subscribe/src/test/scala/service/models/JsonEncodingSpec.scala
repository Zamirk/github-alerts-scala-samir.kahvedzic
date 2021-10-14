package service.models

import io.circe.Encoder
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import service.models.SlackModels.{
  ResponseField,
  SlackMessageBlock1,
  SlackMessageBlock2,
  SlackResponseBody
}

class JsonEncodingSpec extends AnyFreeSpec with Matchers {

  "SlackResponseModel should successfully encode and decode" in {

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

    jsonEncodingTest(data, DomainSamples.SlackResponseBody.jsonString)
  }

  private def jsonEncodingTest[T](testData: T, result: String)(implicit
      `type`: Encoder[T]
  ): Assertion =
    cleanString(Encoder[T].apply(testData).toString()) shouldBe
      cleanString(result)

  def cleanString: String => String = (s: String) =>
    s.stripMargin.filterNot((x: Char) => x.isWhitespace)

}
