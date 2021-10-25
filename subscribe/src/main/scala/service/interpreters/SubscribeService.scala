package service.interpreters
import cats.{ApplicativeError, MonadThrow}
import cats.data.NonEmptyList
import doobie.Transactor
import service.algebras.SubscribeServiceAlg
import service.domain.errors.ApiErrors.SubscriberServiceException._
import service.domain.ApiModels._
import service.domain.DatabaseModels._
import cats.effect._
import cats.free.Free
import cats.implicits.toFunctorOps
import doobie.implicits._
import service.domain.ApiModels.Subscription.{Organisation, Repositoryy}
import cats.implicits._
import doobie.free.connection.ConnectionOp
import service.database.queries.SubscribeServiceQueries

import java.time._

object SubscribeService {

  def make[F[_]](
      db: Transactor[F]
  )(implicit F: MonadCancelThrow[F]): SubscribeServiceAlg[F] = new SubscribeServiceAlg[F] {

    type FreeFResponse[F[_]] = Free[ConnectionOp, F[SubscriptionsResponse]]

    override def getSubscriptions(user: SlackUserId): F[SubscriptionsResponse] = {
      (for {
        doesExist <- SubscribeServiceQueries.existsUser(user).unique
        subscription <-
          if (doesExist)
            SubscribeServiceQueries
              .getSubscriptions(user)
              .to[List]
              .map(l => F.pure(SubscriptionsResponse(l))): FreeFResponse[F]
          else
            Free.pure(MonadThrow[F].raiseError(UserNameNotFound(user.uuid))): FreeFResponse[F]
      } yield subscription).transact(db)
    }.flatten

    type FreeFResponse2[F[_]] = Free[ConnectionOp, F[Unit]]

    override def deleteSubscription(s: SlackUserId, sub: RepoCarrier): F[Unit] = {
      val r = RepoId(sub.subscription.genUUID)
      (for {
        userExists <- SubscribeServiceQueries.existsUser(s).unique
        repoExists <- SubscribeServiceQueries.existsRepo(r).unique
        deleteRepository <-
          ((userExists, repoExists) match {
            case (false, _) =>
              Free.pure(MonadThrow[F].raiseError(UserNameNotFound(s.uuid)))
            case (_, false) =>
              Free.pure(MonadThrow[F].raiseError(RepositoryNotFound(sub)))
            case _ => SubscribeServiceQueries.deleteSubscription(s, r).run.as(F.unit)
          }): FreeFResponse2[F]
      } yield deleteRepository).transact(db)
    }.flatten

    override def insertSubscription(
        slackId: SlackUserId,
        slackUser: String,
        slackChannel: String,
        sub: RepoCarrier
    ): F[Unit] = {
      (for {
        _ <- {
          SubscribeServiceQueries
            .usersInsertionQuery(
              slackId,
              slackUser,
              slackChannel
            )
            .run
        }
        _ <- SubscribeServiceQueries.repositoryInsertionQuery
          .updateMany(
            NonEmptyList.one(
              DBRepository(
                sub.subscription.genUUID,
                sub.subscription.organization,
                sub.subscription.repository
              )
            )
          )
        c <- SubscribeServiceQueries.subscriptionsInsertionQuery
          .updateMany(
            NonEmptyList.one(
              DBSubscription(
                slackId.uuid,
                sub.subscription.genUUID,
                OffsetDateTime.now(),
                RequestType.OpenPR
              )
            )
          )
      } yield c).transact(db).as(())

    }

    override def postSlackCommand(s: SlackUserId, sub: SlackCommand): F[SlackResponseBody] = ???

  }

  def stub[F[_]: MonadThrow]: SubscribeServiceAlg[F] = new SubscribeServiceAlg[F] {
    override def getSubscriptions(s: SlackUserId): F[SubscriptionsResponse] =
      SubscribeService.mockDatabase.get(s.uuid.toString) match {
        case Some(res) =>
          MonadThrow[F].pure(res)
        case None =>
          MonadThrow[F].raiseError(UserNameNotFound(s.uuid))
      }

    override def insertSubscription(
        slackId: SlackUserId,
        slackUser: String,
        slackChannel: String,
        sub: RepoCarrier
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
      SubscribeService.mockDatabase.get(s.uuid.toString) match {
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
          slackId: SlackUserId,
          slackUser: String,
          slackChannel: String,
          sub: RepoCarrier
      ): F[Unit] =
        MonadThrow[F].raiseError(UnexpectedError())

      override def deleteSubscription(s: SlackUserId, sub: RepoCarrier): F[Unit] =
        MonadThrow[F].raiseError(UnexpectedError())

      override def postSlackCommand(s: SlackUserId, sub: SlackCommand): F[SlackResponseBody] =
        MonadThrow[F].raiseError(UnexpectedError())

    }
  }

  val offsetDatTime = OffsetDateTime.of(2000, 10, 10, 10, 10, 10, 10, ZoneOffset.ofHours(1))

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
