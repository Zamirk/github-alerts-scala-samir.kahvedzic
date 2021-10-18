package service.database

import service.domain.ConfigModels._
import cats.effect.Resource
import cats.effect.kernel.Async
import org.typelevel.log4cats.SelfAwareStructuredLogger
//import doobie.ExecutionContexts
//import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor
object DatabaseConnection {

  def dbConnection[F[_]](
      loadedConfig: Config,
      logger: SelfAwareStructuredLogger[F]
  )(implicit s: Async[F]): Resource[F, HikariTransactor[F]] = {
    for {
      _  <- Resource.liftK(logger.info("Creating db connection."))
      ec <- ExecutionContexts.fixedThreadPool[F](32)
      db <- HikariTransactor.newHikariTransactor[F](
        loadedConfig.database.driver,
        loadedConfig.database.url,
        "postgres",
        loadedConfig.database.password,
        ec
      )
    } yield db
  }

  //Creates the table in db.migration if it does not exist
  def createDatabaseTables[F[_]: Async](transactor: HikariTransactor[F]): Resource[F, Unit] = {
    Resource.liftK(
      transactor.configure { dataSource =>
        Async[F].pure {
          Flyway.configure().dataSource(dataSource).load().migrate()
          ()
        }
      }
    )
  }
}
