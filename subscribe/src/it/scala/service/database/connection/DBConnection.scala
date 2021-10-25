package service.database.connection

import cats.effect.IO
import doobie.util.transactor.Transactor.Aux
object DBConnection {
  lazy val db: Aux[IO, Unit] = doobie.util.transactor.Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5436/test",
    "postgres",
    "mysecretpassword"
  )

}
