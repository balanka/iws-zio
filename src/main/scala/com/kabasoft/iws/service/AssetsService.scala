package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.FinancialsTransaction
import zio.*

trait AssetsService {
  def generate( period:Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
}

object AssetsService {
  def generate(period:Int, company: String): ZIO[AssetsService, RepositoryError, List[FinancialsTransaction]]         =
    ZIO.service[AssetsService] flatMap (_.generate(period, company))
}
