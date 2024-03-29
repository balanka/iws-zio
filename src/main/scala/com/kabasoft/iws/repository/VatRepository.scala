package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Vat
import zio._
import zio.stream._

trait VatRepository {
  def create(item: Vat): ZIO[Any, RepositoryError, Vat]
  def create(models: List[Vat]): ZIO[Any, RepositoryError, List[Vat]]
  def create2(item: Vat): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Vat]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Vat]]
  def list(Id:(Int, String)): ZStream[Any, RepositoryError, Vat]
  def getBy(id: (String, String)): ZIO[Any, RepositoryError, Vat]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[Vat]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Vat]
  def modify(model: Vat): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Vat]): ZIO[Any, RepositoryError, Int]
}

object VatRepository {
  def create(item: Vat): ZIO[VatRepository, RepositoryError, Vat] =
    ZIO.service[VatRepository] flatMap (_.create(item))

  def create(items: List[Vat]): ZIO[VatRepository, RepositoryError, List[Vat]] =
    ZIO.service[VatRepository] flatMap (_.create(items))
  def create2(item: Vat): ZIO[VatRepository, RepositoryError, Unit]                               =
    ZIO.service[VatRepository] flatMap (_.create2(item))
  def create2(items: List[Vat]): ZIO[VatRepository, RepositoryError, Int]                         =
    ZIO.service[VatRepository] flatMap (_.create2(items))
  def delete(item: String, company: String): ZIO[VatRepository, RepositoryError, Int]              =
    ZIO.service[VatRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[VatRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def all(Id:(Int, String)): ZIO[VatRepository, RepositoryError, List[Vat]]                =
    ZIO.service[VatRepository] flatMap (_.all(Id))
  def list(Id:(Int, String)): ZStream[VatRepository, RepositoryError, Vat]                   =
    ZStream.service[VatRepository] flatMap (_.list(Id))
  def getBy(id: (String, String)): ZIO[VatRepository, RepositoryError, Vat]          =
    ZIO.service[VatRepository] flatMap (_.getBy(id))
  def getByModelId(modelid:(Int, String)): ZIO[VatRepository, RepositoryError, List[Vat]] =
    ZIO.service[VatRepository] flatMap (_.getByModelId(modelid))

  def getByModelIdStream(modelid: Int, company: String): ZStream[VatRepository, RepositoryError, Vat]=
    ZStream.service[VatRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Vat): ZIO[VatRepository, RepositoryError, Int]                          =
    ZIO.service[VatRepository] flatMap (_.modify(model))
  def modify(models: List[Vat]): ZIO[VatRepository, RepositoryError, Int]                   =
    ZIO.service[VatRepository] flatMap (_.modify(models))

}
