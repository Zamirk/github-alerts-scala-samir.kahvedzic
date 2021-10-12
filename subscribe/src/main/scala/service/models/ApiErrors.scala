package service.models

import service.models.ApiModels.InsertRepository

import scala.util.control.NoStackTrace

object ApiErrors {

  sealed trait BadRepositoryException extends NoStackTrace

  object BadRepositoryException {
    final case class InvalidRepository(repo: InsertRepository) extends BadRepositoryException
  }

  sealed trait ApiException extends NoStackTrace

  object ApiException {
    final case class UserNameNotFound(userName: String) extends ApiException
  }

}
