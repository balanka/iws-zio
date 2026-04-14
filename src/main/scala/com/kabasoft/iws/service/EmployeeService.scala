package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.FinancialsTransaction
import zio.*

trait EmployeeService {
  def generate(period:Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
}

object EmployeeService:
  def generate(period:Int, company: String): ZIO[EmployeeService, RepositoryError, List[FinancialsTransaction]]         =
    ZIO.service[EmployeeService] flatMap (_.generate(period, company))
