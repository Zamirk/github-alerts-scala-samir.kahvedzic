package service

import cats.{Applicative, ApplicativeError, MonadError}
import service.models.ResponseModels.{Subscription, SubscriptionResponse}

trait SubscribeService[F[_]] {

  /** Returns a list of subscriptions
    * @param name               The id of the slack user
    * @return
    */
  def getSubscription(name: String): F[Option[SubscriptionResponse]]
}

object SubscribeService {
  val mockDatabase = Map(
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
case class UserNameNotFound(userName: String) extends Exception

case class FakeSubscriberService[F[_]: MonadError[*[_], Throwable]]()
    extends SubscribeService[F]() {

  override def getSubscription(userName: String): F[Option[SubscriptionResponse]] =
    SubscribeService.mockDatabase.get(userName) match {
      case Some(res) =>
        Applicative[F].pure(if (res.subscriptions.isEmpty) None else Some(res))
      case None =>
        ApplicativeError[F, Throwable].raiseError(UserNameNotFound(userName))
    }
}
