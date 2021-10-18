package service.algebras

trait GithubApiAlg[F[_]] {
  def validateRepo(repo: service.domain.ApiModels.InsertRepository): F[Unit]
}
