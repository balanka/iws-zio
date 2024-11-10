package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Supplier
import zio.Task
import zio.*
import zio.stream.*

import java.time.LocalDate

trait SupplierRepository:

  def create(item: Supplier, flag: Boolean):ZIO[Any, RepositoryError, Int]

  def create(models: List[Supplier]):ZIO[Any, RepositoryError, Int]

  def modify(model: Supplier):ZIO[Any, RepositoryError, Int]

  def modify(models: List[Supplier]):ZIO[Any, RepositoryError, Int]

  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Supplier]]

  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, Supplier]

  def getByIban(Id: (String, Int, String)): ZIO[Any, RepositoryError, Supplier]

  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Supplier]]

  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]

object SupplierRepository:

  def create(item: Supplier, flag: Boolean): ZIO[SupplierRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[SupplierRepository](_.create(item, flag))

  def create(models: List[Supplier]): ZIO[SupplierRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[SupplierRepository](_.create(models))

  def modify(model: Supplier): ZIO[SupplierRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[SupplierRepository](_.modify(model))

  def modify(models: List[Supplier]): ZIO[SupplierRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[SupplierRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[SupplierRepository, RepositoryError, List[Supplier]] =
    ZIO.serviceWithZIO[SupplierRepository](_.all(Id))
    
  def getByIban(Id: (String, Int, String)): ZIO[SupplierRepository, RepositoryError, Supplier] =
    ZIO.serviceWithZIO[SupplierRepository](_.getByIban(Id))
    
  def getById(Id: (String, Int, String)): ZIO[SupplierRepository, RepositoryError, Supplier] =
    ZIO.serviceWithZIO[SupplierRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[SupplierRepository, RepositoryError, List[Supplier]] =
    ZIO.serviceWithZIO[SupplierRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[SupplierRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[SupplierRepository](_.delete(p))
