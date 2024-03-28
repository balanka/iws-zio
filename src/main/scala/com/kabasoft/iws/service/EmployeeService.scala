package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait EmployeeService {
  def generate(company: String): ZIO[Any, RepositoryError, Int]
}

object EmployeeService {
  def generate(company: String): ZIO[EmployeeService, RepositoryError, Int]         =
    ZIO.service[EmployeeService] flatMap (_.generate(company))
}
