package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Bank
import com.kabasoft.iws.repository.Schema.bankSchema
import zio._
import zio.sql.ConnectionPool
import zio.stream._
final class BankRepositoryImpl(pool: ConnectionPool) extends BankRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val bank = defineTable[Bank]("bank")

  val (id, name, description, enterdate, changedate, postingdate, modelid, company) = bank.columns

  val SELECT                                                                           = select(id, name, description, enterdate, changedate, postingdate, modelid, company).from(bank)


  def whereClause(Id: String, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)
  override def create(c: Bank): ZIO[Any, RepositoryError, Bank]                        = create2(c)*>getBy((c.id, c.company))

  override def create(models: List[Bank]): ZIO[Any, RepositoryError, List[Bank]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Bank])
    }else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }

  override def create2(c: Bank): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(bank)(id, name, description, enterdate, changedate, postingdate, modelid, company).values(Bank.unapply(c).get)

    ZIO.logDebug(s"Query to insert Bank is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Bank]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Bank.unapply(_).get)
    val query = insertInto(bank)(id, name, description, enterdate, changedate, postingdate, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert Bank is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(bank).where(whereClause (item, companyId))
    ZIO.logInfo(s"Delete Bank is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def modify(model: Bank): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(bank)
      .set(name, model.name)
      .set(description, model.description)
      .where(whereClause( model.id,  model.company))
    ZIO.logDebug(s"Query Update Bank is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Bank]] =
    list(companyId).runCollect.map(_.toList)

  override def list(companyId: String): ZStream[Any, RepositoryError, Bank]                   = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Bank.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(String,String)): ZIO[Any, RepositoryError, Bank]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Bank.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Bank]] = for {
    banks <- getBy_(ids, company).runCollect.map(_.toList)
  } yield banks

  def getBy_(ids: List[String], company: String): ZStream[Any, RepositoryError, Bank] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((Bank.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Bank]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Bank] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((Bank.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object BankRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, BankRepository] =
    ZLayer.fromFunction(new BankRepositoryImpl(_))
}
