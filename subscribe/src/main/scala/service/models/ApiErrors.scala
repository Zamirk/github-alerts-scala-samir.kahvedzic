package service.models

import service.models.ApiModels.{InsertRepository}

import scala.util.control.NoStackTrace

object ApiErrors {

  sealed trait BadRepositoryException extends NoStackTrace

  object BadRepositoryException {
    final case class InvalidRepository(repo: InsertRepository) extends BadRepositoryException
  }

  sealed trait SubscriberServiceException extends NoStackTrace

  object SubscriberServiceException {
    final case class UserNameNotFound(userName: String)         extends SubscriberServiceException
    final case class RepositoryNotFound(repo: InsertRepository) extends SubscriberServiceException
    final case class UnexpectedError()                          extends SubscriberServiceException
  }

}
