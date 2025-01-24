package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.TransactionLog
import zio._
import zio.stream._

trait TransactionLogRepository:
  def create(item: TransactionLog): ZIO[Any, RepositoryError, Int]
  def create(models: List[TransactionLog]): ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[TransactionLog]]
  //def getById(Id: (String, Int, String)): ZIO[Any, RepositoryError, TransactionLog]
 // def getBy(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]]
  //def delete(p: (Long, Int, String)): ZIO[Any, RepositoryError, Int]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]]
  def find4StorePeriod(store: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]]
  def find4ArticlePeriod(article: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]]
  def find4StoreArticlePeriod(store: String, article: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]]
  def deleteAll(): ZIO[Any, RepositoryError, Int]

object TransactionLogRepository:

  def create(item: TransactionLog): ZIO[TransactionLogRepository, RepositoryError, Int]               =
    ZIO.serviceWithZIO[TransactionLogRepository](_.create(item))
  def create(items: List[TransactionLog]): ZIO[TransactionLogRepository, RepositoryError, Int]         =
    ZIO.serviceWithZIO[TransactionLogRepository](_.create(items))

  def deleteAll(): ZIO[TransactionLogRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[TransactionLogRepository](_.deleteAll())
  //def getBy(ids: List[Long], modelid: Int, company: String): ZIO[TransactionLogRepository, RepositoryError, List[TransactionLog]] =
 //   ZIO.serviceWithZIO[TransactionLogRepository](_.getBy(ids, modelid, company))

  def getByModelId(modelid: Int, company: String): ZIO[TransactionLogRepository, RepositoryError, List[TransactionLog]] =
    ZIO.serviceWithZIO[TransactionLogRepository](_.getByModelId(modelid, company))

  def find4StorePeriod(store: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[TransactionLogRepository, RepositoryError, List[TransactionLog]] =
    ZIO.serviceWithZIO[TransactionLogRepository](_.find4StorePeriod(store, fromPeriod, toPeriod, company))

  def find4ArticlePeriod(article: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[TransactionLogRepository, RepositoryError, List[TransactionLog]] =
   ZIO.serviceWithZIO[TransactionLogRepository](_.find4ArticlePeriod(article, fromPeriod, toPeriod, company))

  def find4StoreArticlePeriod(store: String, article: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[TransactionLogRepository, RepositoryError, List[TransactionLog]] =
  ZIO.serviceWithZIO[TransactionLogRepository](_.find4StoreArticlePeriod(store, article, fromPeriod, toPeriod, company))