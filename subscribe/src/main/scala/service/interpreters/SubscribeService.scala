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

import java.time._
import scala.util.control.NoStackTrace

object SubscribeService {

  def make[F[_]](
      db: Transactor[F]
  )(implicit F: MonadCancelThrow[F]): SubscribeServiceAlg[F] = new SubscribeServiceAlg[F] {

    type FS[F] = Free[ConnectionOp, F]
    type U[A]  = ConnectionIO[A]

    val q: SubscribeServiceQueries.type = SubscribeServiceQueries

    def insertUser(s: SlackUserId, b: String, c: String): U[Int] = q.insertUser(s, b, c).run
    def insertRepository(d: DBRepository): U[Int]                = q.insertRepository.toUpdate0(d).run
    def insertSubscription(d: DBSubscription): U[Int]            = q.insertSubscription.toUpdate0(d).run
    def getSubscribers(s: SlackUserId): U[List[Subscription]]    = q.getSubscriptions(s).to[List]
    def existsUser(s: SlackUserId): U[Boolean]                   = q.existsUser(s).unique
    def existsRepo(s: RepoId): U[Boolean]                        = q.existsRepo(s).unique
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

        (existsUser(s), existsRepo(r)).tupled.flatMap {
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
        e: RequestType = RequestType.All
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

}
