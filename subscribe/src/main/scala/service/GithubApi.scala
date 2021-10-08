package service

import cats.{Applicative, ApplicativeError, MonadError}
import service.BadRepositoryException.InvalidRepository

trait GithubApi[F[_]] {
  def validateRepo(repo: service.models.ApiModels.InsertRepository): F[Unit]
}

object GithubApi {

  def make[F[_]] = new GithubApi[F] {
    def validateRepo(repo: service.models.ApiModels.InsertRepository) = ???
  }

  var mockGithub = Map(
    "48deg" -> "thool",
    "47deg" -> "thool"
  )

  def stub[F[_]: MonadError[*[_], Throwable]] = new GithubApi[F] {

    override def validateRepo(repo: service.models.ApiModels.InsertRepository): F[Unit] = {

      GithubApi.mockGithub.get(repo.subscription.organization) match {
        case Some(_) =>
          Applicative[F].pure(())
        case None =>
          ApplicativeError[F, Throwable].raiseError(InvalidRepository(repo))
      }
    }
  }

}
