package com.kabasoft.iws.repository
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Bom
import zio.Task
import zio.*
import zio.stream.*

import java.time.LocalDate

trait BomRepository:
  def create(item: Bom, flag: Boolean):ZIO[Any, RepositoryError, Int]
  def create(models: List[Bom]): ZIO[Any, RepositoryError, Int]
  def modify(model: Bom): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Bom]): ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Bom]]
  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, Bom]
  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Bom]]
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int]

object BomRepository:
  def create(item: Bom, flag: Boolean):ZIO[BomRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[BomRepository](_.create(item, flag))
  def create(models: List[Bom]): ZIO[BomRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BomRepository](_.create(models))
  def modify(model: Bom): ZIO[BomRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BomRepository](_.modify(model))
  def modify(models: List[Bom]): ZIO[BomRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[BomRepository](_.modify(models))
  def all(Id: (Int, String)): ZIO[BomRepository, RepositoryError, List[Bom]] =
    ZIO.serviceWithZIO[BomRepository](_.all(Id))
  def getById(Id: (String, Int, String)): ZIO[BomRepository, RepositoryError, Bom]=
    ZIO.serviceWithZIO[BomRepository](_.getById(Id))
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[BomRepository, RepositoryError, List[Bom]]=
    ZIO.serviceWithZIO[BomRepository](_.getBy(ids, modelid, company))
  def delete(p: (String, Int, String)): ZIO[BomRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[BomRepository](_.delete(p))