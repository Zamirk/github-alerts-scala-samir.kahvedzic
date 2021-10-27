package service.interpreters

import cats.effect.kernel.Async
import cats.MonadThrow
import cats.implicits.catsSyntaxMonadError
import org.http4s.Status
import org.http4s.client.Client
import service.algebras.GithubApiAlg
import service.domain.errors.ApiErrors.BadRepositoryException.InvalidRepository
import org.http4s.implicits._
import service.domain.errors.ApiErrors.SubscriberServiceException.UnexpectedError
import service.domain.ApiModels
import org.http4s.Status._

object GithubApi {

  def make[F[_]: Async](client: Client[F]): GithubApiAlg[F] = new GithubApiAlg[F] {
    def validateRepo(repo: ApiModels.RepoCarrier): F[Status] = {
      client
        .statusFromUri(
          uri"https://api.github.com" / "repos" / repo.subscription.organization / repo.subscription.repository
        )
        .ensure(InvalidRepository(repo))(_.isSuccess)
    }
  }

  def stub2[F[_]: MonadThrow]: GithubApiAlg[F] = new GithubApiAlg[F] {
    override def validateRepo(repo: ApiModels.RepoCarrier): F[Status] = {
      MonadThrow[F].raiseError(UnexpectedError())
    }
  }

  def stub[F[_]: MonadThrow]: GithubApiAlg[F] = new GithubApiAlg[F] {
    override def validateRepo(repo: ApiModels.RepoCarrier): F[Status] = {
      GithubApi.mockGithub.get(repo.subscription.organization) match {
        case Some(r) if r == repo.subscription.repository =>
          MonadThrow[F].pure(Ok)
        case _ =>
          MonadThrow[F].raiseError(InvalidRepository(repo))
      }
    }
  }

  var mockGithub = Map(
    "47deg" -> "thool"
  )
}
