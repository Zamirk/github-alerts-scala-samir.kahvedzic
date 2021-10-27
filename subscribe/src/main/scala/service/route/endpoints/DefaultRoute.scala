package service.route.endpoints

import org.http4s.Response
import service.domain.errors.ApiErrors.BadRepositoryException.InvalidRepository
import service.domain.errors.ApiErrors.SubscriberServiceException._
import cats.effect._
import org.http4s.dsl._
import service.domain.errors.ApiErrors.ApiRouteError.UsernameInvalidFormat
import cats.implicits._
import org.http4s.circe.CirceEntityEncoder._

import java.util.UUID
class DefaultRoute[F[_]]()(implicit F: Async[F]) extends Http4sDsl[F] {

  type ThrowToResponseF = PartialFunction[Throwable, F[Response[F]]]

  protected def validateUUID(username: String): F[UUID] = {
    Async[F]
      .delay {
        UUID.nameUUIDFromBytes(username.getBytes)
      }
      .adaptError({ _ =>
        UsernameInvalidFormat(username): Throwable
      }: PartialFunction[Throwable, Throwable])
  }

  val userNameValidation: ThrowToResponseF = {
    case UsernameInvalidFormat(u) =>
      BadRequest(s"""The user name provided is an invalid format: $u.""")
    case UserNameNotFound(u) =>
      NotFound(s"""The username was not found.""")
  }

  val repoNotFound: ThrowToResponseF = { case RepositoryNotFound(r) =>
    NotFound(s"Repository not found in subscription ${r.subscription.repository}")
  }

  val githubValidation: ThrowToResponseF = { case InvalidRepository(_) =>
    BadRequest("The github repository is invalid.")
  }

  val internalServerError: ThrowToResponseF = { case _ =>
    InternalServerError()
  }
}
