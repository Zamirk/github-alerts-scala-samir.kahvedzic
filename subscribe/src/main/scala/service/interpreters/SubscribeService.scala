package service.interpreters
import cats._
import doobie.Transactor
import service.algebras.SubscribeServiceAlg
import service.domain.errors.ApiErrors.SubscriberServiceException._
import service.domain.ApiModels._
import service.domain.DatabaseModels._
import cats.effect._
import cats.free.Free
import cats.implicits.toFunctorOps
import doobie.implicits._
import cats.implicits._
import doobie.free.connection.{ConnectionIO, ConnectionOp}
import service.database.queries.SubscribeServiceQueries
import service.domain.ApiModels.Subscription.{Organisation, Repositoryy}

import java.time._
import scala.util.control.NoStackTrace

object SubscribeService {

  def make[F[_]](
      db: Transactor[F]
  )(implicit F: MonadCancelThrow[F]): SubscribeServiceAlg[F] = new SubscribeServiceAlg[F] {

    type FS[F] = Free[ConnectionOp, F]
    type U[A]  = ConnectionIO[A]

    val q: SubscribeServiceQueries.type = SubscribeServiceQueries

    def insertUser(s: SlackUserId, b: String, c: String): U[Int] = q.upsertUser(s, b, c).run
    def insertRepository(d: DBRepository): U[Int]                = q.upsertRepository(d).run
    def insertSubscription(d: DBSubscription): U[Int]            = q.insertSubscription(d).run
    def getSubscribers(s: SlackUserId): U[List[Subscription]]    = q.getSubscriptions(s).to[List]
    def existsUser(s: SlackUserId): U[Boolean]                   = q.existsUser(s).unique
    def existsRepo(s: SlackUserId, r: RepoId): U[Boolean]        = q.existsRepo(s, r).unique
    def deleteSubscription(s: SlackUserId, r: RepoId): U[Int]    = q.deleteSubscription(s, r).run

    def raiseError[D](e: NoStackTrace): FS[F[D]]           = Free.pure(MonadThrow[F].raiseError(e))
    def flatTransact[A](a: Free[ConnectionOp, F[A]]): F[A] = a.transact(db).flatten

    override def getSubscriptions(user: SlackUserId): F[SubscriptionsResponse] =
      flatTransact(
        existsUser(user)
          .flatMap {
            case true  => getSubscribers(user).map(l => F.pure(SubscriptionsResponse(l)))
            case false => raiseError(UserNameNotFound(user.uuid))
          }
      )

    override def deleteSubscription(s: SlackUserId, sub: RepoCarrier): F[Unit] =
      flatTransact[Unit] {
        val r = RepoId(sub.subscription.genUUID)

        (existsUser(s), existsRepo(s, r)).tupled.flatMap {
          case (true, true) => deleteSubscription(s, r).as(F.unit)
          case (false, _)   => raiseError(UserNameNotFound(s.uuid))
          case (_, false)   => raiseError(RepositoryNotFound(sub))
        }
      }

    override def insertSubscription(
        id: SlackUserId,
        user: String,
        channel: String,
        r: Repository,
        e: RequestType = RequestType.OpenPR
    ): F[Unit] = {
      val (uuid, org, repo) = (r.genUUID, r.organization, r.repository)

      for {
        _   <- insertUser(id, user, channel)
        _   <- insertRepository(DBRepository(uuid, org, repo))
        res <- insertSubscription(DBSubscription(id.uuid, uuid, OffsetDateTime.now(), e))
      } yield res
    }.transact(db).as(())

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
        id: SlackUserId,
        user: String,
        channel: String,
        r: Repository,
        e: RequestType = RequestType.OpenPR
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
          id: SlackUserId,
          user: String,
          channel: String,
          r: Repository,
          e: RequestType = RequestType.OpenPR
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
