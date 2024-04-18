package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._
import zio.sql.postgresql.{ PostgresJdbcModule, PostgresSqlModule }
import zio.stream._

trait IWSTableDescriptionPostgres extends PostgresSqlModule with PostgresJdbcModule {

  implicit class ZStreamSqlExt[T](zstream: ZStream[SqlDriver, Exception, T]) {
    def provideDriver(driver: ULayer[SqlDriver]): ZStream[Any, RepositoryError, T] =
      zstream
        .tapError(e => ZIO.logError(e.getMessage))
        .mapError(e => RepositoryError(e.getMessage))
        .provideLayer(driver)

    def findFirst(driver: ULayer[SqlDriver], id: String): ZIO[Any, RepositoryError, T] =
      zstream.runHead.some.tapError {
          case None    => ZIO.unit
          case Some(e) => println("Message:" + e.getMessage); ZIO.logError(e.getMessage)
        }.mapError {
          case None    => println(s"Object with id/name $id does not exists"); RepositoryError(s"Object with id/name $id does not exists")
          case Some(e) => RepositoryError(e.getMessage)
        }
        .provide(driver)
    def findFirst(driver: ULayer[SqlDriver], id: String, dummy: T): ZIO[Any, RepositoryError, T] =
      zstream.runHead.some.tapError {
        case None    => ZIO.unit
        case Some(e) => ZIO.logError(e.getMessage)
      }.mapError {
        case None    => RepositoryError(s"Order with id $id does not exists")
        case Some(e) => RepositoryError(e.getMessage)
      }
        .provide(driver)
        .catchAll(_ => ZIO.succeed(dummy))

    def findFirstInt(driver: ULayer[SqlDriver], id: Int): ZIO[Any, RepositoryError, T] =
      zstream.runHead.some.tapError {
        case None    => ZIO.unit
        case Some(e) => ZIO.logError(e.getMessage)
      }.mapError {
        case None    => RepositoryError(s"Order with id $id does not exists")
        case Some(e) => RepositoryError(e.getMessage)
      }
        .provide(driver)

    def findFirstLong(driver: ULayer[SqlDriver], id: Long): ZIO[Any, RepositoryError, T] =
      zstream.runHead.some.tapError {
        case None    => ZIO.unit
        case Some(e) => println("Message:" + e.getMessage); ZIO.logError(e.getMessage)
      }.mapError {
        case None    =>RepositoryError(s"Order with id $id does not exists")
        case Some(e) => RepositoryError(e.getMessage)
      }
        .provide(driver)
  }

  implicit class ZioSqlExt[T](zio: ZIO[SqlDriver, Exception, T]) {
    def provideAndLog(driver: ULayer[SqlDriver]): ZIO[Any, RepositoryError, T] =
      zio
        .tapError(e => ZIO.logError(e.getMessage))
        .mapError { e => println("Message:" + e.getMessage); RepositoryError(e.getMessage) }
        .provide(driver)
  }
}
