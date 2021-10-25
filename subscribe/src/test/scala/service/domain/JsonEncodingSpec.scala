package service.domain

import io.circe.{Decoder, Encoder}
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import service.route.helpers.SampleData
import io.circe.parser.decode

class JsonEncodingSpec extends AnyFreeSpec with Matchers {

  "SlackResponseModel should successfully encode" in {
    jsonEncodingTest(SampleData.data, DomainSamples.SlackResponseBody.jsonString)
  }

  "SlackResponseModel should successfully decode from a string" in {
    jsonDecodingTest(DomainSamples.SlackResponseBody.jsonString, SampleData.data)
  }

  "SlackResponseBody should successfully encode" in {
    jsonEncodingTest(SampleData.sampleData, DomainSamples.jsonString2)
  }

  "SlackResponseBody should successfully decode from a string" in {
    jsonDecodingTest(DomainSamples.jsonString2, SampleData.sampleData)
  }

  private def jsonDecodingTest[T](decodeString: String, result: T)(implicit
      `type`: Decoder[T]
  ): Assertion = decode[T](decodeString) match {
    case Left(_)      => assert(false)
    case Right(value) => value shouldBe result
  }

  private def jsonEncodingTest[T](testData: T, result: String)(implicit
      `type`: Encoder[T]
  ): Assertion =
    cs(Encoder[T].apply(testData).toString()) shouldBe cs(result)

  def cs: String => String = (s: String) => s.stripMargin.filterNot((x: Char) => x.isWhitespace)

}
