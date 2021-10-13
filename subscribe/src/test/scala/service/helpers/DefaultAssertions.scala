package service.helpers

import org.http4s.Response
import org.scalatest.compatible.Assertion
import org.scalatest.matchers.should.Matchers._
import org.http4s.dsl.io._

object DefaultAssertions {

  def okStatus[F[_]](): Response[F] => Assertion = (r: Response[F]) => r.status shouldBe Ok

  def notFoundStatus[F[_]](): Response[F] => Assertion = (r: Response[F]) =>
    r.status shouldBe NotFound

  def createdStatus[F[_]](): Response[F] => Assertion = (r: Response[F]) =>
    r.status shouldBe Created

  def noContentStatus[F[_]](): Response[F] => Assertion = (r: Response[F]) =>
    r.status shouldBe NoContent

  def badRequestStatus[F[_]](): Response[F] => Assertion = (r: Response[F]) =>
    r.status shouldBe BadRequest

  def internalServerErrorStatus[F[_]](): Response[F] => Assertion = (r: Response[F]) =>
    r.status shouldBe InternalServerError

}
