package service.interpreters

import cats.effect.kernel.Async
import cats.{Applicative, ApplicativeError, MonadError}
import org.http4s.Status.Ok
import org.http4s.client.Client
import service.algebras.GithubApiAlg
import service.models.ApiErrors.BadRepositoryException.InvalidRepository
import org.http4s.implicits._

object GithubApiAlg {

  def make[F[_]: Async](client: Client[F]): GithubApiAlg[F] = new GithubApiAlg[F] {
    def validateRepo(repo: service.models.ApiModels.InsertRepository): F[Unit] = {
      client.get(
        uri"https://api.github.com" / "repos" / repo.subscription.organization / repo.subscription.repository / "contents"
      ) {
        case Status.Successful(_) =>
          Applicative[F].pure(())
        case _ =>
          ApplicativeError[F, Throwable].raiseError(InvalidRepository(repo))
      }
    }
  }

  def stub2[F[_]: MonadError[*[_], Throwable]]: GithubApiAlg[F] = new GithubApiAlg[F] {

    override def validateRepo(repo: service.models.ApiModels.InsertRepository): F[Unit] = {
      ApplicativeError[F, Throwable].raiseError(new Exception("Unknown Error"))
    }
  }

  def stub[F[_]: MonadError[*[_], Throwable]]: GithubApiAlg[F] = new GithubApiAlg[F] {

    override def validateRepo(repo: service.models.ApiModels.InsertRepository): F[Unit] = {
      GithubApiAlg.mockGithub.get(repo.subscription.organization) match {
        case Some(r) if r == repo.subscription.repository =>
          Applicative[F].pure(())
        case _ =>
          ApplicativeError[F, Throwable].raiseError(InvalidRepository(repo))
      }
    }
  }

  var mockGithub = Map(
    "47deg" -> "thool"
  )

}
