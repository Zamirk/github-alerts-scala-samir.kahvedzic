package service.interpreters

import cats.{Applicative, ApplicativeError, MonadError}
import service.algebras.SubscribeServiceAlg
import service.models.ApiErrors.SubscriberServiceException.{RepositoryNotFound, UserNameNotFound}
import service.models.ApiModels.{InsertRepository, SlackUserId, Subscription, SubscriptionsResponse}
object SubscribeServiceAlg {

  def stub2[F[_]: MonadError[*[_], Throwable]]: SubscribeServiceAlg[F] = {
    new SubscribeServiceAlg[F] {
      override def getSubscriptions(s: SlackUserId): F[SubscriptionsResponse] =
        ApplicativeError[F, Throwable].raiseError(new Exception("Unknown Error"))

      override def insertSubscription(s: SlackUserId, sub: InsertRepository): F[Unit] =
        ApplicativeError[F, Throwable].raiseError(new Exception("Unknown Error"))

      override def deleteSubscription(s: SlackUserId, sub: InsertRepository): F[Unit] =
        ApplicativeError[F, Throwable].raiseError(new Exception("Unknown Error"))
    }
  }

  def make[F[_]]: SubscribeServiceAlg[F] = new SubscribeServiceAlg[F] {
    override def getSubscriptions(s: SlackUserId): F[SubscriptionsResponse] =
      ???

    override def insertSubscription(s: SlackUserId, sub: InsertRepository): F[Unit] = ???

    override def deleteSubscription(s: SlackUserId, sub: InsertRepository): F[Unit] = ???
  }

  def stub[F[_]: MonadError[*[_], Throwable]]: SubscribeServiceAlg[F] = new SubscribeServiceAlg[F] {
    override def getSubscriptions(s: SlackUserId): F[SubscriptionsResponse] =
      SubscribeServiceAlg.mockDatabase.get(s.s) match {
        case Some(res) =>
          Applicative[F].pure(res)
        case None =>
          ApplicativeError[F, Throwable].raiseError(UserNameNotFound(s.s))
      }

    override def insertSubscription(s: SlackUserId, sub: InsertRepository): F[Unit] =
      Applicative[F].pure(())

    override def deleteSubscription(s: SlackUserId, sub: InsertRepository): F[Unit] =
      SubscribeServiceAlg.mockDatabase.get(s.s) match {
        case Some(subs) if {
              subs.subscriptions.exists(a =>
                a.repository == sub.subscription.repository && a.organization == sub.subscription.organization
              )
            } =>
          Applicative[F].pure(())
        case Some(_) => ApplicativeError[F, Throwable].raiseError(RepositoryNotFound(sub))
        case None    => ApplicativeError[F, Throwable].raiseError(UserNameNotFound(s.s))
      }
  }

  var mockDatabase = Map(
    "1234" -> SubscriptionsResponse(
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
    ),
    "2345" -> SubscriptionsResponse()
  )

}
