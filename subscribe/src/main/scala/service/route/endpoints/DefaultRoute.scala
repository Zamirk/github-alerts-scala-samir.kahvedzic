package service.route.endpoints

import cats.MonadThrow
import org.http4s.Response
import service.domain.errors.ApiErrors.BadRepositoryException.InvalidRepository
import service.domain.errors.ApiErrors.SubscriberServiceException._
import cats.effect.Async
import cats.free.Free
import org.http4s.dsl._
import service.domain.errors.ApiErrors.ApiRouteError.UsernameInvalidFormat
import java.util.UUID

class DefaultRoute[F[_]]()(implicit F: Async[F]) extends Http4sDsl[F] {

  type ThrowToResponseF = PartialFunction[Throwable, F[Response[F]]]

  protected def validateUUID(username: String): F[UUID] = try {
    F.pure(UUID.nameUUIDFromBytes(username.getBytes))
  } catch {
    case _: Throwable => MonadThrow[F].raiseError(UsernameInvalidFormat(username))
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
