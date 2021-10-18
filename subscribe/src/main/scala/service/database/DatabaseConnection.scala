package service.database

import service.domain.ConfigModels._
import cats.syntax.all._
import cats.effect.Resource
import cats.effect.kernel.Async
import org.typelevel.log4cats.SelfAwareStructuredLogger
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
        loadedConfig.database.databaseUser,
        loadedConfig.database.password,
        ec
      )
    } yield db
  }

  def createDatabaseTables[F[_]: Async](transactor: HikariTransactor[F]): Resource[F, Unit] = {
    Resource.liftK(
      transactor.configure { dataSource =>
        Async[F].delay {
          Flyway.configure().dataSource(dataSource).load().migrate()
        } *> Async[F].unit
      }
    )
  }
}
