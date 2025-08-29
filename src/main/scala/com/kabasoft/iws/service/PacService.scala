package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._

trait  PacService:
  def getBalance4Parent(accId: String,  fromPeriod:Int, toPeriod: Int, companyId: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] 

object PacService:
  def getBalance4Parent(accId: String,  fromPeriod:Int, toPeriod: Int, companyId: String): ZIO[PacService, RepositoryError, List[PeriodicAccountBalance]]=
    ZIO.service[PacService] flatMap (_.getBalance4Parent(accId, fromPeriod, toPeriod, companyId))
