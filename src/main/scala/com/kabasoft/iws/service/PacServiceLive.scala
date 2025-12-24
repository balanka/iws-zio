package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Account, PeriodicAccountBalance}
import com.kabasoft.iws.repository.{AccountRepository, PacRepository}
import zio.{ZIO, ZLayer}

final class  PacServiceLive (accRepo: AccountRepository, pacRepo: PacRepository) extends PacService:
  def getBalance4Parent(accId: String,  fromPeriod:Int, toPeriod: Int, companyId: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]= for {
    accounts <-accRepo.getByParentId(accId, Account.MODELID, companyId)
    _        <- ZIO.logInfo(s"Got  accounts $accounts.map(_.id) ")
    pacs<- pacRepo.getByParent(accounts.map(_.id), fromPeriod,  toPeriod, PeriodicAccountBalance.MODELID,  companyId)
    _        <- ZIO.logInfo(s"Got the PAC entries   $pacs ")
  } yield  pacs


object PacServiceLive:
  val live: ZLayer[AccountRepository & PacRepository, RepositoryError, PacService] =
    ZLayer.fromFunction(new PacServiceLive(_, _))
      
