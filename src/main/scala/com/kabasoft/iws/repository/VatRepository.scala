package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Vat
import zio._
trait VatRepository:
  def create(item: Vat):ZIO[Any, RepositoryError, Int]
  def create(models: List[Vat]):ZIO[Any, RepositoryError, Int]
  def modify(model: Vat):ZIO[Any, RepositoryError, Int]
  def modify(models: List[Vat]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Vat]]
  def getBy(ids: List[String], modelid:Int, company:String): ZIO[Any, RepositoryError, List[Vat]]
  def getById( p: (String, Int, String)):ZIO[Any, RepositoryError, Vat]
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]


object VatRepository:
  def create(item: Vat): ZIO[VatRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[VatRepository](_.create(item))

  def create(models: List[Vat]): ZIO[VatRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[VatRepository](_.create(models))

  def modify(model: Vat): ZIO[VatRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[VatRepository](_.modify(model))

  def modify(models: List[Vat]): ZIO[VatRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[VatRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[VatRepository, RepositoryError, List[Vat]] =
    ZIO.serviceWithZIO[VatRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[VatRepository, RepositoryError, Vat] =
    ZIO.serviceWithZIO[VatRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[VatRepository, RepositoryError, List[Vat]] =
    ZIO.serviceWithZIO[VatRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[VatRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[VatRepository](_.delete(p))