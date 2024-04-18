package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait AssetsService {
  def generate( period:Int, company: String): ZIO[Any, RepositoryError, Int]
}

object AssetsService {
  def generate(period:Int, company: String): ZIO[AssetsService, RepositoryError, Int]         =
    ZIO.service[AssetsService] flatMap (_.generate(period, company))
}
