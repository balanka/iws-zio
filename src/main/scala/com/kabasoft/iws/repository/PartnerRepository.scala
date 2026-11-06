package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Contact
import zio.*

trait PartnerRepository:
  def create(item: Contact): ZIO[Any, RepositoryError, Int]
  def create(models: List[Contact]): ZIO[Any, RepositoryError, Int]
  def modify(model: Contact): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Contact]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Contact]]
  def getById(Id: (String, Int, String)): ZIO[Any, RepositoryError, Contact]
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Contact]]
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int]
  def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int]

object PartnerRepository:
  def create(item: Contact):ZIO[PartnerRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[PartnerRepository](_.create(item).mapError(e => RepositoryError(e.message)))
  def create(models: List[Contact]): ZIO[PartnerRepository, RepositoryError,Int] =
    ZIO.serviceWithZIO[PartnerRepository](_.create(models).mapError(e => RepositoryError(e.message)))
  def modify(model: Contact): ZIO[PartnerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PartnerRepository](_.modify(model).mapError(e => RepositoryError(e.message)))
  def modify(models: List[Contact]): ZIO[PartnerRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[PartnerRepository](_.modify(models).mapError(e => RepositoryError(e.message)))
  def all(Id: (Int, String)): ZIO[PartnerRepository, RepositoryError, List[Contact]] =
    ZIO.serviceWithZIO[PartnerRepository](_.all(Id).mapError(e => RepositoryError(e.message)))
  def getById(Id: (String, Int, String)): ZIO[PartnerRepository, RepositoryError, Contact]=
    ZIO.serviceWithZIO[PartnerRepository](_.getById(Id).mapError(e => RepositoryError(e.message)))
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[PartnerRepository, RepositoryError, List[Contact]]=
    ZIO.serviceWithZIO[PartnerRepository](_.getBy(ids, modelid, company).mapError(e => RepositoryError(e.message)))
  def delete(p: (String, Int, String)): ZIO[PartnerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PartnerRepository](_.delete(p).mapError(e => RepositoryError(e.message)))
  def deleteAll(p: List[(String, Int, String)]): ZIO[PartnerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PartnerRepository](_.deleteAll(p))  