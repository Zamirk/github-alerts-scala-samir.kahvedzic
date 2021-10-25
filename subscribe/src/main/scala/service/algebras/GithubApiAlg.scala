package service.algebras

import service.domain.ApiModels

trait GithubApiAlg[F[_]] {
  def validateRepo(repo: ApiModels.RepoCarrier): F[Unit]
}
