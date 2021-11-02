package service.algebras.services

import org.http4s.Status
import service.domain.ApiModels

trait GithubApiAlg[F[_]] {
  def validateRepo(repo: ApiModels.RepoCarrier): F[Status]
}
