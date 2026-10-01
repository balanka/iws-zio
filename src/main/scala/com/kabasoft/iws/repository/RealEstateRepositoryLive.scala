package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Apartment, Floor, Masterfile, ModelId, RealEstate}
import skunk.*
import zio.interop.catz.asyncInstance
import zio.{Task, ZIO, ZLayer}


final case class RealEstateRepositoryLive(postgres: Resource[Task, Session[Task]], mfRepo:MasterfileRepository
                                        , aptRepo:ApartmentRepository ) extends RealEstateRepository, MasterfileCRUD:
    import MasterfileRepositorySQL.*
    def toMasterfile (ap: RealEstate): Masterfile = Masterfile(ap.id, ap.name, ap.description, "-1", ap.enterdate, ap.changedate,
      ap.postingdate, ap.modelid, ap.company)
    
    def transact(s: Session[Task], newAparment: List[RealEstate]): Task[Unit] =
        transact(s, newAparment.map(toMasterfile), newAparment.flatMap(_.apartments).map(Apartment.toMasterfile).filterNot(_.id.isEmpty)
          , insert, MasterfileRepositorySQL.insert)
  

    def transact(s: Session[Task], newCustomers: List[RealEstate], newbankAccount: List[Apartment], oldCustomers: List[RealEstate]
                 , oldbankAcc2Update: List[Apartment], bankAcc2Delete: List[Apartment]): Task[Unit] =
      transact(s, newCustomers.map(toMasterfile), newbankAccount.map(Apartment.toMasterfile), oldCustomers.map(toMasterfile).map(Masterfile.encodeIt2)
         ,  oldbankAcc2Update.map(Apartment.toMasterfile).map(Masterfile.encodeIt2),  bankAcc2Delete.map(Apartment.toMasterfile).map(Masterfile.encodeIt3)
         , insert, MasterfileRepositorySQL.insert, MasterfileRepositorySQL.UPDATE, MasterfileRepositorySQL.UPDATE
         , RoomRepositorySQL.DELETE)
  
    override def create(c: RealEstate): ZIO[Any, RepositoryError, Int] = create(List(c))
    override def create(models: List[RealEstate]):ZIO[Any, RepositoryError, Int] =
      (postgres
        .use:
            session =>
              transact(session, models))
                .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.apartments).size + models.size )
  
    override def modify(model: RealEstate):ZIO[Any, RepositoryError, Int] = modify(List(model))
     
    override def modify(models: List[RealEstate]):ZIO[Any, RepositoryError, Int] = {
      val oldLines2Update = models.flatMap(_.apartments).filter(bankAccount => bankAccount.modelid>0 
          && bankAccount.company.contains("-"))
        .map(bankAccount =>bankAccount.copy(company = bankAccount.company.replace("-","")))
      val newLine2Insert = models.flatMap(_.apartments).filter(bankAccount =>bankAccount.modelid === -1
                                && bankAccount.company.contains("-") && bankAccount.id.nonEmpty)
                                  .map(bankAccount => bankAccount.copy(modelid = ModelId.APARTMENT.modelid,
                                             company = bankAccount.company.replace("-", "")))
      val oldLine2Delete = models.flatMap(_.apartments).filter(_.modelid === -2)
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
 
    override def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[RealEstate]] = for {
                  realEstates <- mfRepo.all(Id)
                  floors_ <- mfRepo.all(ModelId.FLOOR.modelid, Id._2)
                  apartments_ <- aptRepo.all(ModelId.APARTMENT.modelid, Id._2)
             } yield realEstates.map(p => RealEstate.apply(p).copy(apartments = apartments_.filter(_.parent == p.id)
                             , floors = floors_.map(Floor.apply1).filter(_.parent == p.id)))
  
    override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, RealEstate] = for {
      mf <- mfRepo.getById(p)
      floors_ <- mfRepo.all(ModelId.FLOOR.modelid, p._3)
      apartments_ <- aptRepo.getByParent(mf.id, ModelId.APARTMENT.modelid, p._3)
    } yield RealEstate.apply(mf).copy(apartments = apartments_, floors = floors_.map(Floor.apply1).filter(_.parent == mf.id))
    
    override def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[RealEstate]] = for {
      realEstates <- mfRepo.getBy(ids, modelid, company)
      floors_ <- mfRepo.all(ModelId.FLOOR.modelid, company)
      apartments_ <-aptRepo.all(ModelId.APARTMENT.modelid, company)
    }yield realEstates.map(p => RealEstate.apply(p).copy(apartments = apartments_.filter(_.parent == p.id)
                                  , floors = floors_.map(Floor.apply1).filter(_.parent == p.id)))
  
    override def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = mfRepo.delete(p)  
    override def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] = mfRepo.deleteAll(p)

object  RealEstateRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & ApartmentRepository & MasterfileRepository, RepositoryError, RealEstateRepository] =
    ZLayer.fromFunction(new RealEstateRepositoryLive(_, _, _))

  