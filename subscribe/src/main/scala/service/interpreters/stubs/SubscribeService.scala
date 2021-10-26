package service.interpreters.stubs

import cats.MonadThrow
import service.algebras.SubscribeServiceAlg
import service.domain.ApiModels.Subscription.{Organisation, Repositoryy}
import service.domain.ApiModels._
import service.domain.DatabaseModels.RequestType
import service.domain.errors.ApiErrors.SubscriberServiceException._
import service.interpreters.stubs
import java.time.{OffsetDateTime, ZoneOffset}

object SubscribeService {

  def stub[F[_]: MonadThrow]: SubscribeServiceAlg[F] = new SubscribeServiceAlg[F] {
    override def getSubscriptions(s: SlackUserId): F[SubscriptionsResponse] =
      stubs.SubscribeService.mockDatabase.get(s.uuid.toString) match {
        case Some(res) =>
          MonadThrow[F].pure(res)
        case None =>
          MonadThrow[F].raiseError(UserNameNotFound(s.uuid))
      }

    override def insertSubscription(
        id: SlackUserId,
        user: String,
        channel: String,
        r: Repository,
        e: RequestType = RequestType.All
    ): F[Unit] =
      MonadThrow[F].unit

    //TODO Implementation in future PR
    override def postSlackCommand(s: SlackUserId, sub: SlackCommand): F[SlackResponseBody] =
      MonadThrow[F].pure(
        SlackResponseBody.generateSlackResponse(
          "It's 80 degrees right now.",
          List(
            "*It's 80 degrees right now.*",
            "Partly cloudy today and tomorrow"
          )
        )
      )

    override def deleteSubscription(s: SlackUserId, sub: RepoCarrier): F[Unit] =
      mockDatabase.get(s.uuid.toString) match {
        case Some(subs) if {
              subs.subscriptions.exists(a =>
                a.repository.rep == sub.subscription.repository && a.organization.org == sub.subscription.organization
              )
            } =>
          MonadThrow[F].unit
        case Some(_) => MonadThrow[F].raiseError(RepositoryNotFound(sub))
        case None    => MonadThrow[F].raiseError(UserNameNotFound(s.uuid))
      }
  }

  def stub2[F[_]: MonadThrow]: SubscribeServiceAlg[F] = {
    new SubscribeServiceAlg[F] {
      override def getSubscriptions(s: SlackUserId): F[SubscriptionsResponse] =
        MonadThrow[F].raiseError(UnexpectedError())

      def insertSubscription(
          id: SlackUserId,
          user: String,
          channel: String,
          r: Repository,
          e: RequestType = RequestType.All
      ): F[Unit] =
        MonadThrow[F].raiseError(UnexpectedError())

      override def deleteSubscription(s: SlackUserId, sub: RepoCarrier): F[Unit] =
        MonadThrow[F].raiseError(UnexpectedError())

      override def postSlackCommand(s: SlackUserId, sub: SlackCommand): F[SlackResponseBody] =
        MonadThrow[F].raiseError(UnexpectedError())

    }
  }

  val offsetDatTime: OffsetDateTime =
    OffsetDateTime.of(2000, 10, 10, 10, 10, 10, 10, ZoneOffset.ofHours(1))

  var mockDatabase = Map(
    "48918a8f-3175-3474-9d92-e19d5fcd9124" -> SubscriptionsResponse(
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
        Subscription(Organisation("47degrees"), Repositoryy("github4s"), offsetDatTime)
      )
    ),
    "Ali_Shah" -> SubscriptionsResponse()
  )
}
