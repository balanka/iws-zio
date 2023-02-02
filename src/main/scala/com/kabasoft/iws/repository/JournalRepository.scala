package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Journal
import zio._
import zio.stream._

trait JournalRepository {

  def create(item: Journal): IO[RepositoryError, Unit]
  def create(models: List[Journal]): IO[RepositoryError, Int]
  def delete(Id: Long, company: String): IO[RepositoryError, Int]
  //def delete(Ids: List[Long], company: String): ZStream[Any, RepositoryError, Int]
  def list(company: String): ZStream[Any, RepositoryError, Journal]
  def getBy(id: Long, company: String): IO[RepositoryError, Journal]
  def getByModelId(modelid: Int, company: String): IO[RepositoryError, Journal]
  def find4Period(accountId:String, fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, Journal]

}

object JournalRepository {

  def create(item: Journal): ZIO[JournalRepository, RepositoryError, Unit]                             =
    ZIO.service[JournalRepository] flatMap (_.create(item))
  def create(items: List[Journal]): ZIO[JournalRepository, RepositoryError, Int]                       =
    ZIO.service[JournalRepository] flatMap (_.create(items))
  def delete(Id: Long, company: String): ZIO[JournalRepository, RepositoryError, Int]              =
    ZIO.service[JournalRepository] flatMap (_.delete(Id, company))
  //def delete(Ids: List[Long], company: String): ZStream[JournalRepository, RepositoryError, Int]   =
  //  ZStream.service[JournalRepository] flatMap (_.delete(Ids, company))
  def list(company: String): ZStream[JournalRepository, RepositoryError, Journal]                      =
    ZStream.service[JournalRepository] flatMap (_.list(company))
  def getBy(id: Long, company: String): ZIO[JournalRepository, RepositoryError, Journal]             =
    ZIO.service[JournalRepository] flatMap (_.getBy(id, company))

  def getByModelId(modelid: Int, company: String): ZIO[JournalRepository, RepositoryError, Journal] =
    ZIO.service[JournalRepository] flatMap (_.getByModelId(modelid, company))

  def find4Period(accountId:String, fromPeriod: Int, toPeriod: Int, company: String): ZStream[JournalRepository, RepositoryError, Journal] =
    ZStream.service[JournalRepository] flatMap (_.find4Period(accountId, fromPeriod, toPeriod, company))

}
