package com.kabasoft.iws.repository

import zio.sql.postgresql.PostgresJdbcModule
import zio.stream._
import zio._
import com.kabasoft.iws.domain.AppError.RepositoryError

trait PostgresTableDescription extends PostgresJdbcModule {

  implicit class ZStreamSqlExt[T](zstream: ZStream[SqlDriver, Exception, T]) {
    def provideDriver(
      driver: ULayer[SqlDriver]
    ): ZStream[Any, RepositoryError, T] =
      zstream
        .tapError(e => ZIO.logError(e.getMessage))
        .mapError(e => RepositoryError(e.getMessage))
        .provideLayer(driver)

    def findFirst(
      driver: ULayer[SqlDriver],
      id: java.util.UUID
    ): ZIO[Any, RepositoryError, T] =
      zstream.runHead.some.tapError {
        case None    => ZIO.unit
        case Some(e) => ZIO.logError(e.getMessage)
      }.mapError {
        case None    => RepositoryError(s"Order with id $id does not exists")
        case Some(e) => RepositoryError(e.getMessage)
      }
        .provide(driver)

    def findFirst(driver: ULayer[SqlDriver], id: String): ZIO[Any, RepositoryError, T] =
      zstream.runHead.some.tapError {
        case None    => ZIO.unit
        case Some(e) => println("Message:" + e.getMessage); ZIO.logError(e.getMessage)
      }.mapError {
        case None    => RepositoryError(s"Object with id/name $id does not exists")
        case Some(e) => RepositoryError(e.getMessage)
      }
        .provide(driver)
  }

  implicit class ZioSqlExt[T](zio: ZIO[SqlDriver, Exception, T]) {
    def provideAndLog(driver: ULayer[SqlDriver]): ZIO[Any, RepositoryError, T] =
      zio
        .tapError(e => ZIO.logError(e.getMessage))
        .mapError(e => RepositoryError(e.getMessage))
        .provide(driver)
  }
}
