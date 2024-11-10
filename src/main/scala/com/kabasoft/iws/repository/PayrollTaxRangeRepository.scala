package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.PayrollTaxRange
import zio._
import zio.stream._

trait PayrollTaxRangeRepository:
  def create(item: PayrollTaxRange, flag: Boolean):ZIO[Any, RepositoryError, Int]
  def create(models: List[PayrollTaxRange]):ZIO[Any, RepositoryError, Int]
  def modify(model: PayrollTaxRange):ZIO[Any, RepositoryError, Int]
  def modify(models: List[PayrollTaxRange]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[PayrollTaxRange]]
  def getById(Id: (String, Int, String)): ZIO[Any, RepositoryError, PayrollTaxRange]
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[PayrollTaxRange]]
  def delete(p: (String, Int, String)): ZIO[PayrollTaxRangeRepository, RepositoryError, Int]

object PayrollTaxRangeRepository:
  def create(item: PayrollTaxRange, flag: Boolean): ZIO[PayrollTaxRangeRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PayrollTaxRangeRepository](_.create(item, flag))

  def create(models: List[PayrollTaxRange]): ZIO[PayrollTaxRangeRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PayrollTaxRangeRepository](_.create(models))

  def modify(model: PayrollTaxRange): ZIO[PayrollTaxRangeRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PayrollTaxRangeRepository](_.modify(model))

  def modify(models: List[PayrollTaxRange]): ZIO[PayrollTaxRangeRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PayrollTaxRangeRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[PayrollTaxRangeRepository, RepositoryError, List[PayrollTaxRange]] =
    ZIO.serviceWithZIO[PayrollTaxRangeRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[PayrollTaxRangeRepository, RepositoryError, PayrollTaxRange] =
    ZIO.serviceWithZIO[PayrollTaxRangeRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[PayrollTaxRangeRepository, RepositoryError, List[PayrollTaxRange]] =
    ZIO.serviceWithZIO[PayrollTaxRangeRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[PayrollTaxRangeRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PayrollTaxRangeRepository](_.delete(p))
