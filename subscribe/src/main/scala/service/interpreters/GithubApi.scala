package service.interpreters

import cats.effect.kernel.Async
import cats.MonadThrow
import org.http4s.Status
import org.http4s.client.Client
import service.algebras.GithubApiAlg
import service.domain.errors.ApiErrors.BadRepositoryException.InvalidRepository
import org.http4s.implicits._
import service.domain.errors.ApiErrors.SubscriberServiceException.UnexpectedError
import service.domain.ApiModels

object GithubApi {

  def make[F[_]: Async](client: Client[F]): GithubApiAlg[F] = new GithubApiAlg[F] {
    def validateRepo(repo: ApiModels.RepoCarrier): F[Unit] = {
      client.get(
        uri"https://api.github.com" / "repos" / repo.subscription.organization / repo.subscription.repository / "contents"
      ) {
        case Status.Successful(_) =>
          MonadThrow[F].unit
        case _ =>
          MonadThrow[F].raiseError(InvalidRepository(repo))
      }
    }
  }

  def stub2[F[_]: MonadThrow]: GithubApiAlg[F] = new GithubApiAlg[F] {
    override def validateRepo(repo: ApiModels.RepoCarrier): F[Unit] = {
      MonadThrow[F].raiseError(UnexpectedError())
    }
  }

  def stub[F[_]: MonadThrow]: GithubApiAlg[F] = new GithubApiAlg[F] {
    override def validateRepo(repo: ApiModels.RepoCarrier): F[Unit] = {
      GithubApi.mockGithub.get(repo.subscription.organization) match {
        case Some(r) if r == repo.subscription.repository =>
          MonadThrow[F].unit
        case _ =>
          MonadThrow[F].raiseError(InvalidRepository(repo))
      }
    }
  }

  var mockGithub = Map(
    "47deg" -> "thool"
  )
}
