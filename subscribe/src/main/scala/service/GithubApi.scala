package service

import cats.{Applicative, Monad}
import service.models.ResponseModels

trait GithubApi[F[_]] {
  def validateRepo(repo: service.models.ResponseModels.InsertRepository): F[Unit]

}

case class FakeGithubApi[F[_]: Monad]() extends GithubApi[F]() {
  override def validateRepo(repo: ResponseModels.InsertRepository): F[Unit] =
    Applicative[F].pure(())
}
