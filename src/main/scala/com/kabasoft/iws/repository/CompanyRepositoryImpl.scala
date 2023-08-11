package com.kabasoft.iws.repository

import com.kabasoft.iws.repository.Schema.companySchema
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Company
import zio._
import zio.stream._
import zio.sql.ConnectionPool

final class CompanyRepositoryImpl(pool: ConnectionPool) extends CompanyRepository with PostgresTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val company = defineTable[Company]("company")

  val (
    id,
    name,
    street,
    zip,
    city,
    state,
    country,
    email,
    partner,
    phone,
    bankAcc,
    iban,
    taxCode,
    vatCode,
    currency,
    locale,
    balanceSheetAcc,
    incomeStmtAcc,
    modelid
  ) = company.columns

  val SELECT                                                                 = select(
    id,
    name,
    street,
    zip,
    city,
    state,
    country,
    email,
    partner,
    phone,
    bankAcc,
    iban,
    taxCode,
    vatCode,
    currency,
    locale,
    balanceSheetAcc,
    incomeStmtAcc,
    modelid
  )
    .from(company)
  def toTuple(c: Company)                                                    = (
    c.id,
    c.name,
    c.street,
    c.zip,
    c.city,
    c.state,
    c.country,
    c.email,
    c.partner,
    c.phone,
    c.bankAcc,
    c.iban,
    c.taxCode,
    c.vatCode,
    c.currency,
    c.locale,
    c.balanceSheetAcc,
    c.incomeStmtAcc,
    c.modelid
  )

  private def buildInsertQuery(companies: List[Company]) =
    insertInto(company)(
      id,
      name,
      street,
      zip,
      city,
      state,
      country,
      email,
      partner,
      phone,
      bankAcc,
      iban,
      taxCode,
      vatCode,
      currency,
      locale,
      balanceSheetAcc,
      incomeStmtAcc,
      modelid
    ).values(companies.map(toTuple))

  override def create2(c: Company): ZIO[Any, RepositoryError, Unit]           = {
    val query = buildInsertQuery(List(c))

    ZIO.logDebug(s"Query to insert Company is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Company]): ZIO[Any, RepositoryError, Int] = {
   val query = buildInsertQuery(models)
    ZIO.logDebug(s"Query to insert Company is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }

  override def create(c: Company): ZIO[Any, RepositoryError, Company] =
    create2(c) *> getBy((c.id))

  override def create(models: List[Company]): ZIO[Any, RepositoryError, List[Company]] =
    if (models.isEmpty) {
      ZIO.succeed(List.empty[Company])
    } else {
      create2(models) *> getBy(models.map(_.id))
    }
  override def delete(item: String): ZIO[Any, RepositoryError, Int]          =
    execute(deleteFrom(company).where((id === item)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def modify(model: Company): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(company)
      .set(name, model.name)
      .where((id === model.id))
    ZIO.logDebug(s"Query Update Company is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all: ZIO[Any, RepositoryError, List[Company]] =
    list.runCollect.map(_.toList)
  /* for {
    companies <- list.runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(companyId).runCollect.map(_.toList)
  } yield companies.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

   */

  override def list: ZStream[Any, RepositoryError, Company]          = {
    val selectAll =
      select(
        id,
        name,
        street,
        zip,
        city,
        state,
        country,
        email,
        partner,
        phone,
        bankAcc,
        iban,
        taxCode,
        vatCode,
        currency,
        locale,
        balanceSheetAcc,
        incomeStmtAcc,
        modelid
      ).from(company) // .where(id === companyId)

    /* ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
     */
    execute(selectAll.to((Company.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getBy(Id: String): ZIO[Any, RepositoryError, Company] = {
    execute(SELECT.where(id===Id).to((Company.apply _).tupled))
      .findFirst(driverLayer, Id)
  }

  def getBy(ids: List[String]): ZIO[Any, RepositoryError, List[Company]] = for {
    vats <- getBy_(ids).runCollect.map(_.toList)
  } yield vats

  def getBy_(ids: List[String]): ZStream[Any, RepositoryError, Company] = {
    val selectAll = SELECT.where( id  in ids)
    execute(selectAll.to((Company.apply _).tupled))
      .provideDriver(driverLayer)
  }

}

object CompanyRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CompanyRepository] =
    ZLayer.fromFunction(new CompanyRepositoryImpl(_))
}
