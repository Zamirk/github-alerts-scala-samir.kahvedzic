package service.models

import service.models.ApiModels.InsertRepository

object ApiErrors {

  sealed trait BadRepositoryException extends Exception

  object BadRepositoryException {
    final case class InvalidRepository(repo: InsertRepository) extends BadRepositoryException

    final case class MalformedRepository() extends BadRepositoryException
  }

  sealed trait ApiException extends Exception

  object ApiException {
    final case class UserNameNotFound(userName: String) extends ApiException
  }

}
