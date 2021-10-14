package service.models

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import service.models.SlackModels.ResponseField.In_channel

object SlackModels {

  lazy val formData: List[String] = List("command", "text", "response_url", "trigger_id", "user_id")

  case class SlackCommand(
      command: String,
      text: String,
      responseUrl: String,
      triggerId: String,
      userId: String
  )

  sealed trait ResponseField

  object ResponseField {
    case object In_channel extends ResponseField
    case object Ephemeral  extends ResponseField

    implicit val decodeMode: Decoder[ResponseField] = Decoder[String].emap {
      case "in_channel" => Right(In_channel)
      case "ephermeral" => Right(Ephemeral)
      case other        => Left(s"Invalid mode: $other")
    }

    implicit val encodeMode: Encoder[ResponseField] = Encoder[String].contramap {
      case In_channel => "in_channel"
      case Ephemeral  => "ephermeral"
    }
  }

  final case class SlackMessageBlock1(`type`: String, text: SlackMessageBlock2)

  object SlackMessageBlock1 {
    implicit val smbD: Decoder[SlackMessageBlock1] = deriveDecoder[SlackMessageBlock1]
    implicit val smbE: Encoder[SlackMessageBlock1] = deriveEncoder[SlackMessageBlock1]
  }

  final case class SlackMessageBlock2(`type`: String, text: String)

  object SlackMessageBlock2 {
    implicit val smb2D: Decoder[SlackMessageBlock2] = deriveDecoder[SlackMessageBlock2]
    implicit val smb2E: Encoder[SlackMessageBlock2] = deriveEncoder[SlackMessageBlock2]
  }

  final case class SlackResponseBody(
      response_type: ResponseField,
      text: String,
      blocks: List[SlackMessageBlock1]
  )

  object SlackResponseBody {
    implicit val srbD: Decoder[SlackResponseBody] = deriveDecoder[SlackResponseBody]
    implicit val srbE: Encoder[SlackResponseBody] = deriveEncoder[SlackResponseBody]

    def generateSlackResponse(text: String, msgBlock: List[String]): SlackResponseBody = {
      SlackResponseBody(
        In_channel,
        text,
        msgBlock.map(m => SlackMessageBlock1("section", SlackMessageBlock2("mrkdwn", m)))
      )
    }
  }

}
