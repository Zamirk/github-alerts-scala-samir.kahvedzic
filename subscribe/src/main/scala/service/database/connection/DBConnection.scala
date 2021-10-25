package service.database.connection

import cats.effect.Resource
import cats.effect.kernel.Async
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import org.typelevel.log4cats.SelfAwareStructuredLogger
import service.domain.ConfigModels.Config
import cats.syntax.all._
import doobie.ExecutionContexts

object DBConnection {

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
