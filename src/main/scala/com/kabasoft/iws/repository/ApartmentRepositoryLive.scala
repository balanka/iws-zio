package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats.*
import skunk.*
import zio.interop.catz.asyncInstance
import zio.{Task, ZIO, ZLayer}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Apartment, Masterfile, Room}


final case class ApartmentRepositoryLive(postgres: Resource[Task, Session[Task]], mfRepo:MasterfileRepository
                                        , roomRepo:RoomRepository ) extends ApartmentRepository, MasterfileCRUD:
    import MasterfileRepositorySQL._
    def toMasterfile (ap: Apartment): Masterfile = Masterfile(ap.id, ap.name, ap.description,  ap.parent, ap.enterdate, ap.changedate,
      ap.postingdate, ap.modelid, ap.company)
    
    def transact(s: Session[Task], newAparment: List[Apartment]): Task[Unit] =
        transact(s, newAparment.map(toMasterfile), newAparment.flatMap(_.rooms).filterNot(_.id.isEmpty)
          , insert, RoomRepositorySQL.insert)
  

    def transact(s: Session[Task], newCustomers: List[Apartment], newbankAccount: List[Room], oldCustomers: List[Apartment]
                 , oldbankAcc2Update: List[Room], bankAcc2Delete: List[Room]): Task[Unit] =
      transact(s, newCustomers.map(toMasterfile), newbankAccount, oldCustomers.map(toMasterfile).map(Masterfile.encodeIt2)
         ,  oldbankAcc2Update.map(Room.encodeIt2),  bankAcc2Delete.map(Room.encodeIt3)
         , insert, RoomRepositorySQL.insert, MasterfileRepositorySQL.UPDATE, RoomRepositorySQL.UPDATE
         , RoomRepositorySQL.DELETE)
  
    override def create(c: Apartment): ZIO[Any, RepositoryError, Int] = create(List(c))
    override def create(models: List[Apartment]):ZIO[Any, RepositoryError, Int] =
      (postgres
        .use:
            session =>
              transact(session, models))
                .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.rooms).size + models.size )
  
    override def modify(model: Apartment):ZIO[Any, RepositoryError, Int] = modify(List(model))
     
    override def modify(models: List[Apartment]):ZIO[Any, RepositoryError, Int] = {
      val oldLines2Update = models.flatMap(_.rooms).filter(bankAccount => bankAccount.modelid>0 
          && bankAccount.company.contains("-"))
        .map(bankAccount =>bankAccount.copy(company = bankAccount.company.replace("-","")))
      val newLine2Insert = models.flatMap(_.rooms).filter(bankAccount =>bankAccount.modelid === -1
                                && bankAccount.company.contains("-") && bankAccount.id.nonEmpty)
                                  .map(bankAccount => bankAccount.copy(modelid = Room.MODEL_ID,
                                             company = bankAccount.company.replace("-", "")))
      val oldLine2Delete = models.flatMap(_.rooms).filter(_.modelid === -2)
        .map(bankAccount => bankAccount.copy(company = bankAccount.company.replace("-", "")))
        ZIO.logInfo(s"models ${models}") *>
          ZIO.logInfo(s"oldLines2Update ${oldLines2Update}") *>
          ZIO.logInfo(s"newLine2Insert ${newLine2Insert}")*>
          ZIO.logInfo(s"oldLine2Delete ${oldLine2Delete}")*>
      postgres
        .use:
          session =>
            transact(session, List.empty, newLine2Insert, models, oldLines2Update,  oldLine2Delete)
        .mapBoth(e => RepositoryError(e.getMessage), _ => 
          models.size+newLine2Insert.size+oldLines2Update.size+oldLine2Delete.size)
    }
 
    override def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Apartment]] = for {
                  apartments <- mfRepo.all(Id)
                  rooms_ <- roomRepo.all(Room.MODEL_ID, Id._2)
             } yield apartments.map(p => Apartment.apply(p).copy(rooms = rooms_.filter(_.parent == p.id)))
  
    override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Apartment] = for {
      ap <- mfRepo.getById(p)
      rooms_ <- roomRepo.getByParent(ap.id, Room.MODEL_ID, p._3)
    } yield Apartment.apply(ap).copy(rooms = rooms_.filter(_.parent == ap.id))

    override def getByParent(parent: String, modelid: Int, company: String): ZIO[Any, RepositoryError, List[Apartment]] =for {
      mf <-mfRepo.getByParent (parent, modelid, company)
     }yield mf.map(Apartment.apply)

    override def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Apartment]] = for {
      apartments <- mfRepo.getBy(ids, modelid, company)
      rooms_ <-roomRepo.all(Room.MODEL_ID, company)
    } yield apartments.map(p => Apartment.apply(p).copy(rooms = rooms_.filter(_.parent == p.id)))
  
    override def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = mfRepo.delete(p)  
    override def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] = mfRepo.deleteAll(p)

object  ApartmentRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & RoomRepository & MasterfileRepository, RepositoryError, ApartmentRepository] =
    ZLayer.fromFunction(new ApartmentRepositoryLive(_, _, _))

  