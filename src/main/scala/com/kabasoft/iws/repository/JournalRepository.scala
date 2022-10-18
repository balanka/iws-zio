package com.kabasoft.iws.repository

import zio.stream._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait JournalRepository {

  def create(item: Journal): IO[RepositoryError, Unit]
  def create(models: List[Journal]): IO[RepositoryError, Int]
  def delete(item: String, company: String): IO[RepositoryError, Int]
  def delete(items: List[String], company: String): IO[RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, Journal]
  def getBy(id: String, company: String): IO[RepositoryError, Journal]
  def getByModelId(modelid: Int, company: String): IO[RepositoryError, Journal]
  def find4Period(accountId:String, fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, Journal]

}

object JournalRepository {

  def create(item: Journal): ZIO[JournalRepository, RepositoryError, Unit]                             =
    ZIO.service[JournalRepository] flatMap (_.create(item))
  def create(items: List[Journal]): ZIO[JournalRepository, RepositoryError, Int]                       =
    ZIO.service[JournalRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[JournalRepository, RepositoryError, Int]              =
    ZIO.service[JournalRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[JournalRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[JournalRepository, RepositoryError, Journal]                      =
    ZStream.service[JournalRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[JournalRepository, RepositoryError, Journal]             =
    ZIO.service[JournalRepository] flatMap (_.getBy(id, company))

  def getByModelId(modelid: Int, company: String): ZIO[JournalRepository, RepositoryError, Journal] =
    ZIO.service[JournalRepository] flatMap (_.getByModelId(modelid, company))

  def find4Period(accountId:String, fromPeriod: Int, toPeriod: Int, company: String): ZStream[JournalRepository, RepositoryError, Journal] =
    ZStream.service[JournalRepository] flatMap (_.find4Period(accountId, fromPeriod, toPeriod, company))

}
