package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankStatement, FinancialsTransaction}
import zio.*

trait BankStatementRepository:
  def create(item: BankStatement):ZIO[Any, RepositoryError, Int]
  def create(models: List[BankStatement]):ZIO[Any, RepositoryError, Int]
  def modify(model: BankStatement):ZIO[Any, RepositoryError, Int]
  def modify(models: List[BankStatement]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)):ZIO[Any, RepositoryError, List[BankStatement]]
  def getById(Id: (Long, Int, String)): ZIO[Any, RepositoryError, BankStatement]
  def getBy(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[BankStatement]]
  def delete(p: (Long, Int, String)):ZIO[Any, RepositoryError, Int]
  def deleteAll(): ZIO[Any, RepositoryError, Int]
  def post(bs: List[BankStatement], transactions: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]

object BankStatementRepository:

  def create(item: BankStatement):ZIO[BankStatementRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[BankStatementRepository](_.create(item))

  def create(models: List[BankStatement]): ZIO[BankStatementRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BankStatementRepository](_.create(models))

  def modify(model: BankStatement): ZIO[BankStatementRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BankStatementRepository](_.modify(model))

  def modify(models: List[BankStatement]): ZIO[BankStatementRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[BankStatementRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[BankStatementRepository, RepositoryError, List[BankStatement]] =
    ZIO.serviceWithZIO[BankStatementRepository](_.all(Id))

  def getById(Id: (Long, Int, String)): ZIO[BankStatementRepository, RepositoryError, BankStatement]=
    ZIO.serviceWithZIO[BankStatementRepository](_.getById(Id))

  def getBy(ids: List[Long], modelid: Int, company: String): ZIO[BankStatementRepository, RepositoryError, List[BankStatement]]=
    ZIO.serviceWithZIO[BankStatementRepository](_.getBy(ids, modelid, company))

  def delete(p: (Long, Int, String)): ZIO[BankStatementRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BankStatementRepository](_.delete(p))

  def deleteAll(): ZIO[BankStatementRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[BankStatementRepository](_.deleteAll())

//  def update(model: BankStatement): ZIO[BankStatementRepository, RepositoryError, BankStatement] =
//    ZIO.serviceWithZIO[BankStatementRepository](_.update(model))

  def post(bs: List[BankStatement], transactions: List[FinancialsTransaction]): ZIO[BankStatementRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BankStatementRepository](_.post(bs, transactions))