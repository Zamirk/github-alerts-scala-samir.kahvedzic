package service.route.helpers

import org.http4s.Response
import org.scalatest.compatible.Assertion
import org.scalatest.matchers.should.Matchers._
import org.http4s.dsl.io._

object DefaultAssertions {

  type ResponseToAssertion[F[_]] = Response[F] => Assertion

  private def assertStatus[F[_], A](status: A): ResponseToAssertion[F] = (r: Response[F]) =>
    r.status shouldBe status

  def isOk[F[_]]: ResponseToAssertion[F] = assertStatus(Ok)

  def isNotFound[F[_]]: ResponseToAssertion[F] = assertStatus(NotFound)

  def isCreated[F[_]]: ResponseToAssertion[F] = assertStatus(Created)

  def isAccepted[F[_]]: ResponseToAssertion[F] = assertStatus(Accepted)

  def isNoContent[F[_]]: ResponseToAssertion[F] = assertStatus(NoContent)

  def isBadRequest[F[_]]: ResponseToAssertion[F] = assertStatus(BadRequest)

  def isInternalServerError[F[_]]: ResponseToAssertion[F] = assertStatus(InternalServerError)

}
