package service.models

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import service.models.SlackModels.ResponseField.InChannel
import service.models.SlackModels.SlackCommand.{Command, Text}
import service.models.SlackModels.SlackCommand._
object SlackModels {

  lazy val formData: List[String] = List("command", "text", "response_url", "trigger_id", "user_id")

  case class SlackCommand(
      command: Command,
      text: Text,
      responseUrl: ResponseUrl,
      triggerId: TriggerId,
      userId: UserId
  )

  object SlackCommand {
    case class Command(s: String)
    case class Text(s: String)
    case class ResponseUrl(s: String)
    case class TriggerId(s: String)
    case class UserId(s: String)
  }

  sealed trait ResponseField

  object ResponseField {
    case object InChannel extends ResponseField
    case object Ephemeral extends ResponseField

    implicit val decodeMode: Decoder[ResponseField] = Decoder[String].emap {
      case "in_channel" => Right(InChannel)
      case "ephemeral"  => Right(Ephemeral)
      case other        => Left(s"Invalid mode: $other")
    }

    implicit val encodeMode: Encoder[ResponseField] = Encoder[String].contramap {
      case InChannel => "in_channel"
      case Ephemeral => "ephemeral"
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
        InChannel,
        text,
        msgBlock.map(m => SlackMessageBlock1("section", SlackMessageBlock2("mrkdwn", m)))
      )
    }
  }

}
