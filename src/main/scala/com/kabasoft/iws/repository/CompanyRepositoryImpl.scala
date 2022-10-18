package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError._
import com.kabasoft.iws.domain._
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class CompanyRepositoryImpl(pool: ConnectionPool) extends CompanyRepository with IWSTableDescriptionPostgres {
  import ColumnSet._

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

   val company =
    (string("id") ++ string("name")  ++ string("street") ++ string("zip") ++ string("city") ++ string("state")
   ++ string("phone") ++ string("fax") ++ string("email") ++ string("partner") ++ string("locale") ++ string("bankacc") ++ string(
      "taxcode"
    ) ++ string("vatcode") ++ string("currency") ++ instant("enterdate") ++ string("balancesheetacc") ++ string("incomestmtacc") ++ int("modelid"))
      .table("company")
  // ++ string("purchasingClearingAcc")
  // ++ string ("pageHeaderText") ++ string("pageFooterText") ++ string("headerText") ++ string("footerText") ++ string("logoName")
  // ++ string ("logoContent") ++ string("contentType") ++
  // ++ string("salesClearingAcc")++ string("paymentClearingAcc")++ string("settlementClearingAcc")++ string("balanceSheetAcc")
  // ++ string("incomeStmtAcc")++ string("cashAcc")
  val (
    id,
    name,
    street,
    zip,
    city,
    state,
    phone,
    fax,
    email,
    partner,
    locale,
    bankacc,
    taxcode,
    vatcode,
    currency,
    enterdate,
    balancesheetacc,
    incomestmtacc,
    modelid
  ) = company.columns

  val X =
    id ++ name  ++ street ++ zip ++ city ++ state  ++ phone ++ fax ++ email ++ partner ++ locale ++ bankacc ++ taxcode ++ vatcode ++ currency ++ enterdate ++ balancesheetacc ++ incomestmtacc ++ modelid

  val SELECT              = select(X).from(company)
  def toTuple(c: Company) = (
    c.id,
    c.name,
    c.street,
    c.zip,
    c.city,
    c.state,
    c.phone,
    c.fax,
    c.email,
    c.partner,
    c.locale,
    c.bankAcc,
    c.taxCode,
    c.vatCode,
    c.currency,
    c.enterdate,
    c.balanceSheetAcc,
    c.incomeStmtAcc,
    c.modelid
  )

  override def create(c: Company): ZIO[Any, RepositoryError, Unit]           = {
    val query = insertInto(company)(X).values(toTuple(c))

    ZIO.logInfo(s"Query to insert Company is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Company]): ZIO[Any, RepositoryError, Int] = {
    val data  = models.map(toTuple(_))
    val query = insertInto(company)(X).values(data)

    ZIO.logInfo(s"Query to insert Company is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String): ZIO[Any, RepositoryError, Int]          =
    execute(deleteFrom(company).where((id === item.toLong)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Company): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(company)
      .set(name, model.name)
      .where((id === model.id))
    ZIO.logInfo(s"Query Update Company is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def list(companyId: String): ZStream[Any, RepositoryError, Company] = {
    val selectAll = SELECT

    /* ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>

     */
    execute(selectAll.to[Company](c => Company(c)))
      .provideDriver(driverLayer)
  }
  override def getBy(Id: String): ZIO[Any, RepositoryError, Company]           = {
    val selectAll = SELECT.where(id === Id)

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Company](c => Company(c)))
        .findFirst(driverLayer, Id)
  }

}

object CompanyRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CompanyRepository] =
    ZLayer.fromFunction(new CompanyRepositoryImpl(_))
}
