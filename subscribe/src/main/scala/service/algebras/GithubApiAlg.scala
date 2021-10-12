package service.algebras

trait GithubApiAlg[F[_]] {
  def validateRepo(repo: service.models.ApiModels.InsertRepository): F[Unit]
}
