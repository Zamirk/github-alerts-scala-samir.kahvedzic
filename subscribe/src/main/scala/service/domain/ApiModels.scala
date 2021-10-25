package service.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, HCursor, Json}
import service.domain.ApiModels.ResponseField.InChannel
import service.domain.ApiModels.SlackCommand._
import service.domain.ApiModels.Subscription.{Organisation, Repositoryy}

import java.time.OffsetDateTime
import java.util.UUID

object ApiModels {

  final case class SlackUserId(uuid: UUID)
  final case class RepoId(uuid: UUID)

  final case class SubscriptionsResponse(subscriptions: List[Subscription] = Nil)

  object SubscriptionsResponse {
    implicit val sd: Decoder[SubscriptionsResponse] = deriveDecoder[SubscriptionsResponse]
    implicit val se: Encoder[SubscriptionsResponse] = deriveEncoder[SubscriptionsResponse]
  }

  final case class Subscription(
      organization: Organisation,
      repository: Repositoryy,
      subscribedAt: OffsetDateTime
  )

  object Subscription {
    final case class Organisation(org: String)
    final case class Repositoryy(rep: String)

    val org = "organization"
    val rep = "repository"
    val sub = "subscribedAt"

    implicit val subscriptionEncode: Encoder[Subscription] = (s: Subscription) =>
      Json.obj(
        (org, Json.fromString(s.organization.org)),
        (rep, Json.fromString(s.repository.rep)),
        (sub, Encoder[OffsetDateTime].apply(s.subscribedAt))
      )

    implicit val subscriptionDecode: Decoder[Subscription] = (c: HCursor) => {
      for {
        o <- c.downField(org).as[String]
        r <- c.downField(rep).as[String]
        s <- c.downField(sub).as[OffsetDateTime]
      } yield new Subscription(Organisation(o), Repositoryy(r), s)
    }
  }

  final case class RepoCarrier(subscription: Repository)

  object RepoCarrier {

    implicit val ri: Decoder[RepoCarrier]  = deriveDecoder[RepoCarrier]
    implicit val ri2: Encoder[RepoCarrier] = deriveEncoder[RepoCarrier]
  }

  final case class Repository(organization: String, repository: String) {
    def genUUID: UUID = UUID.nameUUIDFromBytes((organization + ": " + repository).getBytes)
  }

  object Repository {
    implicit val rD1: Decoder[Repository] = deriveDecoder[Repository]
    implicit val rE2: Encoder[Repository] = deriveEncoder[Repository]
  }

  case class SlackCommand(
      command: Command,
      text: Text,
      responseUrl: ResponseUrl,
      triggerId: TriggerId,
      userId: UserId
  )

  object SlackCommand {
    lazy val formData: List[String] =
      List("command", "text", "response_url", "trigger_id", "user_id")

    case class Command(s: String)
    case class Text(s: String)
    case class ResponseUrl(s: String)
    case class TriggerId(s: String)
    case class UserId(s: String)
  }

  sealed trait ResponseField { val s: String }

  object ResponseField {
    case object InChannel extends ResponseField {
      override val s: String = "in_channel"
    }
    case object Ephemeral extends ResponseField {
      override val s: String = "ephemeral"
    }

    implicit val decodeMode: Decoder[ResponseField] = Decoder[String].emap {
      case InChannel.s => Right(InChannel)
      case Ephemeral.s => Right(Ephemeral)
      case other       => Left(s"Invalid mode: $other")
    }

    implicit val encodeMode: Encoder[ResponseField] = Encoder[String].contramap {
      case InChannel => InChannel.s
      case Ephemeral => Ephemeral.s
    }
  }

  final case class SlackMessageBlock1(`type`: String, text: SlackMessageBlock2)

  object SlackMessageBlock1 {
    implicit val smbD: Decoder[SlackMessageBlock1] = deriveDecoder
    implicit val smbE: Encoder[SlackMessageBlock1] = deriveEncoder
  }

  final case class SlackMessageBlock2(`type`: String, text: String)

  object SlackMessageBlock2 {
    implicit val smb2D: Decoder[SlackMessageBlock2] = deriveDecoder
    implicit val smb2E: Encoder[SlackMessageBlock2] = deriveEncoder
  }

  final case class SlackResponseBody(
      response_type: ResponseField,
      text: String,
      blocks: List[SlackMessageBlock1]
  )

  object SlackResponseBody {
    implicit val srbD: Decoder[SlackResponseBody] = deriveDecoder
    implicit val srbE: Encoder[SlackResponseBody] = deriveEncoder

    def generateSlackResponse(text: String, msgBlock: List[String]): SlackResponseBody = {
      SlackResponseBody(
        InChannel,
        text,
        msgBlock.map(m => SlackMessageBlock1("section", SlackMessageBlock2("mrkdwn", m)))
      )
    }

  }

}
