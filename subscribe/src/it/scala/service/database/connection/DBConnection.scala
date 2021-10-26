package service.database.connection

import cats.effect.kernel.Resource
import cats.effect.{Async, IO}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway
object DBConnection {

  def buildTransactor(): Resource[IO, HikariTransactor[IO]] = {
    for {
      db <- for {
        ce <- ExecutionContexts.fixedThreadPool[IO](32)
        xa <- HikariTransactor.newHikariTransactor[IO](
          "org.postgresql.Driver",
          "jdbc:postgresql://localhost:5436/test",
          "postgres",
          "mysecretpassword",
          ce
        )
      } yield xa
    } yield db
  }

  def createTables(db: HikariTransactor[IO]): IO[Unit] = {
    println("Heyyy")
    db.configure { dataSource =>
      Async[IO].delay {
        Flyway.configure().dataSource(dataSource).load().migrate()
      } *> Async[IO].unit
    }
  }

  def dropTables(db: HikariTransactor[IO]): IO[Unit] = {
    db.configure { dataSource =>
      IO.delay {
        Flyway.configure().dataSource(dataSource).load().clean()
      } *> Async[IO].unit
    }
  }

}
