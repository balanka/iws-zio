package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, Journal, PeriodicAccountBalance}
import zio._

trait FinancialsService:
  def post(id: Long, company: String): ZIO[Any, RepositoryError, Int]
  def postAll(ids: List[Long], modelid:Int, company: String): ZIO[Any, RepositoryError, Int]
  def getBy(id: String, company: String): ZIO[FinancialsService, RepositoryError, PeriodicAccountBalance]
  def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Journal]]
  def getByIds(ids: List[String], company: String): ZIO[FinancialsService, RepositoryError, List[PeriodicAccountBalance]]
  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, modelid: Int, company: String): ZIO[Any, RepositoryError, Int]
  //def buildPacIds(model: FinancialsTransaction): List[String]
  def postNewFinancialsTransaction(transaction: FinancialsTransaction): ZIO[Any, RepositoryError,
    (FinancialsTransaction, List[PeriodicAccountBalance], UIO[List[PeriodicAccountBalance]], List[Journal])]
  


object FinancialsService:
  def post(id: Long, company: String): ZIO[FinancialsService, RepositoryError, Int]         =
    ZIO.serviceWithZIO[FinancialsService](_.post(id, company))
  def postAll(ids: List[Long], modelid:Int, company: String): ZIO[FinancialsService, RepositoryError, Int]                                       =
    ZIO.serviceWithZIO[FinancialsService](_.postAll(ids, modelid, company))
  def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[FinancialsService, RepositoryError, List[Journal]] =
    ZIO.serviceWithZIO[FinancialsService](_.journal(accountId, fromPeriod, toPeriod, company))

  def getBy(id: String, company: String): ZIO[FinancialsService, RepositoryError, PeriodicAccountBalance]                 =
    ZIO.serviceWithZIO[FinancialsService](_.getBy(id, company))
  def getByIds(ids: List[String], company: String): ZIO[FinancialsService, RepositoryError, List[PeriodicAccountBalance]] =
    ZIO.serviceWithZIO[FinancialsService](_.getByIds(ids, company))
  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, modelid: Int,company: String): ZIO[FinancialsService, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsService](_.postTransaction4Period(fromPeriod, toPeriod, modelid, company))
  
  def buildPacIds(model: FinancialsTransaction): List[String] = {
    val pacIds: List[String] = model.lines.map(line => PeriodicAccountBalance.createId(model.getPeriod, line.account))
    val pacOids: List[String] = model.lines.map(line => PeriodicAccountBalance.createId(model.getPeriod, line.oaccount))
    (pacIds ++ pacOids).distinct
  }

  def postNewFinancialsTransaction(transaction: FinancialsTransaction): ZIO[FinancialsService, RepositoryError,
    (FinancialsTransaction, List[PeriodicAccountBalance], UIO[List[PeriodicAccountBalance]], List[Journal])]=
    ZIO.serviceWithZIO[FinancialsService](_.postNewFinancialsTransaction(transaction))

