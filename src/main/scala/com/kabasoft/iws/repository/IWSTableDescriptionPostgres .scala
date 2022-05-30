package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._
import zio.schema.{ Schema, StandardType }
import zio.sql.postgresql.{ PostgresJdbcModule, PostgresSqlModule }
import zio.stream._

import java.time.Instant
import java.time.format.DateTimeFormatter

trait IWSTableDescriptionPostgres extends PostgresSqlModule with PostgresJdbcModule {

  implicit val instantSchema: Schema[Instant] =
    Schema.primitive(StandardType.InstantType(DateTimeFormatter.ISO_INSTANT))

  implicit class ZStreamSqlExt[T](zstream: ZStream[SqlDriver, Exception, T]) {
    def provideDriver(driver: ULayer[SqlDriver]): ZStream[Any, RepositoryError, T] =
      zstream
        .tapError(e => ZIO.logError(e.getMessage))
        .mapError(e => RepositoryError(e.getCause))
        .provideLayer(driver)

    def findFirst(driver: ULayer[SqlDriver], id: String): ZIO[Any, RepositoryError, T] =
      zstream.runHead.some.tapError {
        case None    => ZIO.unit
        case Some(e) => ZIO.logError(e.getMessage())
      }.mapError {
        case None    =>
          RepositoryError(
            new RuntimeException(s"Order with id $id does not exists")
          )
        case Some(e) => RepositoryError(e.getCause())
      }
        .provide(driver)

    def findFirstInt(driver: ULayer[SqlDriver], id: Int): ZIO[Any, RepositoryError, T] =
      zstream.runHead.some.tapError {
        case None    => ZIO.unit
        case Some(e) => ZIO.logError(e.getMessage)
      }.mapError {
        case None    =>
          RepositoryError(
            new RuntimeException(s"Order with id $id does not exists")
          )
        case Some(e) => RepositoryError(e.getCause)
      }
        .provide(driver)

    def findFirstLong(driver: ULayer[SqlDriver], id: Long): ZIO[Any, RepositoryError, T] =
      zstream.runHead.some.tapError {
        case None    => ZIO.unit
        case Some(e) => ZIO.logError(e.getMessage)
      }.mapError {
        case None    =>
          RepositoryError(
            new RuntimeException(s"Order with id $id does not exists")
          )
        case Some(e) => RepositoryError(e.getCause)
      }
        .provide(driver)
  }

  implicit class ZioSqlExt[T](zio: ZIO[SqlDriver, Exception, T]) {
    def provideAndLog(driver: ULayer[SqlDriver]): ZIO[Any, RepositoryError, T] =
      zio
        .tapError(e => ZIO.logError(e.getMessage()))
        .mapError(e => RepositoryError(e.getCause()))
        .provide(driver)
  }
}
