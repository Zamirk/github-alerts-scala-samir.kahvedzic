package service.interpreters

import cats.{Applicative, ApplicativeError, MonadError}
import service.algebras.SubscribeServiceAlg
import service.models.ApiErrors.ApiException.UserNameNotFound
import service.models.ApiModels.{InsertRepository, Subscription, SubscriptionResponse}

object SubscribeServiceAlg {

  def stub2[F[_]: MonadError[*[_], Throwable]]: SubscribeServiceAlg[F] = {
    new SubscribeServiceAlg[F] {
      override def getSubscription(userName: String): F[SubscriptionResponse] =
        ApplicativeError[F, Throwable].raiseError(new Exception("Unknown Error"))

      override def insertSubscription(userName: String, sub: InsertRepository): F[Unit] =
        ApplicativeError[F, Throwable].raiseError(new Exception("Unknown Error"))
    }
  }

  def make[F[_]] = new SubscribeServiceAlg[F] {
    override def getSubscription(userName: String): F[SubscriptionResponse] =
      ???

    override def insertSubscription(userName: String, sub: InsertRepository): F[Unit] = ???
  }

  def stub[F[_]: MonadError[*[_], Throwable]]: SubscribeServiceAlg[F] = new SubscribeServiceAlg[F] {
    override def getSubscription(userName: String): F[SubscriptionResponse] =
      SubscribeServiceAlg.mockDatabase.get(userName) match {
        case Some(res) =>
          Applicative[F].pure(res)
        case None =>
          ApplicativeError[F, Throwable].raiseError(UserNameNotFound(userName))
      }

    override def insertSubscription(userName: String, sub: InsertRepository): F[Unit] =
      Applicative[F].pure(())
  }

  var mockDatabase = Map(
    "1234" -> SubscriptionResponse(
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
    ),
    "2345" -> SubscriptionResponse()
  )

}
