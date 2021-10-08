package service

import cats.{Applicative, ApplicativeError, MonadError}
import service.models.ApiModels.{InsertRepository, Subscription, SubscriptionResponse}

trait SubscribeService[F[_]] {

  /** Returns a list of subscriptions
    * @param name               The id of the slack user
    * @return
    */
  def getSubscription(name: String): F[SubscriptionResponse]

  def insertSubscription(userName: String, sub: InsertRepository): F[Int]

}

object SubscribeService {
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

  def make[F[_]] = new SubscribeService[F] {
    override def getSubscription(userName: String): F[SubscriptionResponse] =
      ??? // Production implementation

    override def insertSubscription(userName: String, sub: InsertRepository): F[Int] = ???
  }

  def stub[F[_]: MonadError[*[_], Throwable]] = new SubscribeService[F] {
    override def getSubscription(userName: String): F[SubscriptionResponse] =
      SubscribeService.mockDatabase.get(userName) match {
        case Some(res) =>
          Applicative[F].pure(res)
        case None =>
          ApplicativeError[F, Throwable].raiseError(UserNameNotFound(userName))
      }

    override def insertSubscription(userName: String, sub: InsertRepository): F[Int] = {

      SubscribeService.mockDatabase.get(userName) match {
        case Some(a) => a.subscriptions ++ Subscription(sub.subscription.organization, sub.subscription.organization, "")
        case None => SubscribeService.mockDatabase = SubscribeService.mockDatabase ++ Map(userName -> SubscriptionResponse(
          Subscription(sub.subscription.organization, sub.subscription.organization, "")
        ))
      }

    }



      SubscribeService.mockDatabase = SubscribeService.mockDatabase ++ Map(
        //      userName -> SubscriptionResponse(
        //        List(Subscription(sub.subscription.organization, sub.subscription.repository, ""))
        //      )
      )
      Applicative[F].pure(1)
    }
  }


}

sealed trait ApiException extends Exception

final case class UserNameNotFound(userName: String) extends ApiException
