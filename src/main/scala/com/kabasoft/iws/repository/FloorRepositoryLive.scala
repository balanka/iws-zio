package com.kabasoft.iws.repository

import cats.effect.Resource
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Apartment, Floor, Masterfile, Room}
import skunk._

import zio.{Task, ZIO, ZLayer}


final case class FloorRepositoryLive(postgres: Resource[Task, Session[Task]], mfRepo:MasterfileRepository
                                        , apartRepo:ApartmentRepository ) extends FloorRepository, MasterfileCRUD:
    import MasterfileRepositorySQL._
    def toMasterfile (ap: Floor): Masterfile = Masterfile(ap.id, ap.name, ap.description,  ap.parent, ap.enterdate, ap.changedate,
      ap.postingdate, ap.modelid, ap.company)

    def fromMasterfile(ap:Masterfile):Floor  = Floor(ap.id, ap.name, ap.description, ap.parent, ap.enterdate, ap.changedate,
         ap.postingdate, List.empty[Apartment], ap.company, ap.modelid)
  
    override def create(c: Floor): ZIO[Any, RepositoryError, Int] = create(List(c))
    override def create(models: List[Floor]):ZIO[Any, RepositoryError, Int] =
      executeWithTx(postgres, models.map(toMasterfile).map(Masterfile.encodeIt), insertAll(models.size), models.size)

  
    override def modify(model: Floor):ZIO[Any, RepositoryError, Int] = modify(List(model))
     
    override def modify(models: List[Floor]):ZIO[Any, RepositoryError, Int] =
      executeBatchWithTxK(postgres, models.map(toMasterfile), UPDATE, Masterfile.encodeIt2)

 
    override def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Floor]] = for {
                  mfs  <- mfRepo.all(Id)
                  apartments_ <- apartRepo.all(Apartment.MODEL_ID, Id._2)
             } yield mfs.map(p => fromMasterfile(p).copy(apartments = apartments_.filter(_.parent == p.id)))
  
    override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Floor] = for {
      mf <- mfRepo.getById(p)
      apartments_ <- apartRepo.getByParent(mf.id, Room.MODEL_ID, p._3)
    } yield Floor.apply1(mf).copy(apartments = apartments_.filter(_.parent == mf.id))

    override def getByParent(parent: String, modelid: Int, company: String): ZIO[Any, RepositoryError, List[Floor]] =for {
      masterfiles <-mfRepo.getByParent (parent, modelid, company)
      apartments_ <- apartRepo.all(Apartment.MODEL_ID, company)
     } yield masterfiles.map(fromMasterfile).map(p=>p.copy(apartments = apartments_.filter(_.parent == p.id)))

    override def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Floor]] = for {
      floors <- mfRepo.getBy(ids, modelid, company)
      apartments_ <-apartRepo.all(Apartment.MODEL_ID, company)
    } yield floors.map(p => fromMasterfile(p).copy(apartments = apartments_.filter(_.parent == p.id)))
  
    override def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = mfRepo.delete(p)  
    override def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] = mfRepo.deleteAll(p)

object  FloorRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & ApartmentRepository & MasterfileRepository, RepositoryError, FloorRepository] =
    ZLayer.fromFunction(new FloorRepositoryLive(_, _, _))

  