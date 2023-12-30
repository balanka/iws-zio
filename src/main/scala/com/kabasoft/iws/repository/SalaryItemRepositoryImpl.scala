package com.kabasoft.iws.repository
import com.kabasoft.iws.repository.Schema.salaryItemSchema
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.SalaryItem
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class SalaryItemRepositoryImpl(pool: ConnectionPool) extends SalaryItemRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val salaryItem = defineTable[SalaryItem]("salary_item")

  val (id, name, description, account, amount, percentile, enterdate, changedate, postingdate, modelid, company) = salaryItem.columns

  val SELECT                                                                           = select(id, name, description, account, amount, percentile, enterdate, changedate, postingdate, modelid, company).from(salaryItem)
  def whereClause(Id: String, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)
  override def create(c: SalaryItem): ZIO[Any, RepositoryError, SalaryItem]                        = create2(c)*>getBy((c.id, c.company))

  override def create(models: List[SalaryItem]): ZIO[Any, RepositoryError, List[SalaryItem]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[SalaryItem])
    }else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }

  override def create2(c: SalaryItem): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(salaryItem)(id, name, description, account, amount, percentile, enterdate, changedate, postingdate, modelid, company).values(SalaryItem.unapply(c).get)

    ZIO.logDebug(s"Query to insert SalaryItem is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[SalaryItem]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(SalaryItem.unapply(_).get)
    val query = insertInto(salaryItem)(id, name, description, account, amount, percentile, enterdate, changedate, postingdate, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert SalaryItem is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(idx: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(salaryItem).where((company === companyId) && (id === idx) )
    ZIO.logInfo(s"Delete salaryItem is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def modify(model: SalaryItem): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(salaryItem)
      .set(name, model.name)
      .set(description, model.description)
      .set(account, model.account)
      .set(amount, model.amount)
      .set(percentile, model.percentile)
      .where(whereClause( model.id,  model.company))
    ZIO.logDebug(s"Query Update salaryItem is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[SalaryItem]] =
    list(companyId).runCollect.map(_.toList)

  override def list(companyId: String): ZStream[Any, RepositoryError, SalaryItem]                   = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((SalaryItem.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(String,String)): ZIO[Any, RepositoryError, SalaryItem]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((SalaryItem.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[SalaryItem]] = for {
    salaryItems <- getBy_(ids, company).runCollect.map(_.toList)
  } yield salaryItems

  def getBy_(ids: List[String], company: String): ZStream[Any, RepositoryError, SalaryItem] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((SalaryItem.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[SalaryItem]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, SalaryItem] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((SalaryItem.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object SalaryItemRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, SalaryItemRepository] =
    ZLayer.fromFunction(new SalaryItemRepositoryImpl(_))
}
