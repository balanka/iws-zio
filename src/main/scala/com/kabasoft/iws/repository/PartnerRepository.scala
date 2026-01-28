package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Partner
import zio.*

trait PartnerRepository:
  def create(item: Partner): ZIO[Any, RepositoryError, Int]
  def create(models: List[Partner]): ZIO[Any, RepositoryError, Int]
  def modify(model: Partner): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Partner]):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Partner]]
  def getById(Id: (String, Int, String)): ZIO[Any, RepositoryError, Partner]
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Partner]]
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int]
  def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int]

object PartnerRepository:
  def create(item: Partner):ZIO[PartnerRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[PartnerRepository](_.create(item).mapError(e => RepositoryError(e.message)))
  def create(models: List[Partner]): ZIO[PartnerRepository, RepositoryError,Int] =
    ZIO.serviceWithZIO[PartnerRepository](_.create(models).mapError(e => RepositoryError(e.message)))
  def modify(model: Partner): ZIO[PartnerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PartnerRepository](_.modify(model).mapError(e => RepositoryError(e.message)))
  def modify(models: List[Partner]): ZIO[PartnerRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[PartnerRepository](_.modify(models).mapError(e => RepositoryError(e.message)))
  def all(Id: (Int, String)): ZIO[PartnerRepository, RepositoryError, List[Partner]] =
    ZIO.serviceWithZIO[PartnerRepository](_.all(Id).mapError(e => RepositoryError(e.message)))
  def getById(Id: (String, Int, String)): ZIO[PartnerRepository, RepositoryError, Partner]=
    ZIO.serviceWithZIO[PartnerRepository](_.getById(Id).mapError(e => RepositoryError(e.message)))
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[PartnerRepository, RepositoryError, List[Partner]]=
    ZIO.serviceWithZIO[PartnerRepository](_.getBy(ids, modelid, company).mapError(e => RepositoryError(e.message)))
  def delete(p: (String, Int, String)): ZIO[PartnerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PartnerRepository](_.delete(p).mapError(e => RepositoryError(e.message)))
  def deleteAll(p: List[(String, Int, String)]): ZIO[PartnerRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PartnerRepository](_.deleteAll(p))  