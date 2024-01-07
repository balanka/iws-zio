package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait EmployeeService {
  def generate(modelid:Int, company: String): ZIO[Any, RepositoryError, Int]
}

object EmployeeService {
  def generate(modelid:Int, company: String): ZIO[EmployeeService, RepositoryError, Int]         =
    ZIO.service[EmployeeService] flatMap (_.generate(modelid, company))
}
