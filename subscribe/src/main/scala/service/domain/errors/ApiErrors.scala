package service.domain.errors

import service.domain.ApiModels.RepoCarrier

import java.util.UUID
import scala.util.control.NoStackTrace

object ApiErrors {

  sealed trait BadRepositoryException extends NoStackTrace

  object BadRepositoryException {
    final case class InvalidRepository(repo: RepoCarrier) extends BadRepositoryException
  }

  sealed trait ApiRouteError extends NoStackTrace

  object ApiRouteError {
    final case class UsernameInvalidFormat(slackUser: String) extends ApiRouteError
  }

  sealed trait SubscriberServiceException extends NoStackTrace

  object SubscriberServiceException {
    final case class UserNameNotFound(slackUser: UUID)     extends SubscriberServiceException
    final case class RepositoryNotFound(repo: RepoCarrier) extends SubscriberServiceException
    final case class UnexpectedError()                     extends SubscriberServiceException
  }

}
