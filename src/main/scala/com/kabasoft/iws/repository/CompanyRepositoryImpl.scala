package com.kabasoft.iws.repository

import com.kabasoft.iws.repository.Schema.{bankAccountSchema, company_Schema}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankAccount, Company, Company_}
import zio._
import zio.prelude.FlipOps
import zio.stream._
import zio.sql.ConnectionPool

import scala.annotation.nowarn

final class CompanyRepositoryImpl(pool: ConnectionPool) extends CompanyRepository with PostgresTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  private val company = defineTable[Company_]("company")
  private val bankAccount = defineTable[BankAccount]("bankaccount")
  val (iban_, bic, owner, company_, modelid_) = bankAccount.columns
  private  val SELECT_BANK_ACCOUNT = select(iban_, bic, owner, company_, modelid_).from(bankAccount)

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

  private def buildInsertBankAccount(ba: List[BankAccount]) =
    insertInto(bankAccount)(iban_, bic, owner, company_, modelid_).values(ba.map(BankAccount.unapply(_).get))

  private def buildUpdateBankAccount(model: BankAccount): Update[BankAccount] =
    update(bankAccount)
      .set(bic, model.bic)
      .set(owner, model.owner)
      .set(company_, model.company)
      .where(iban_ === model.id)
  private def buildDeleteBankAccount(ids: List[String]): List[Delete[BankAccount]] =
    ids.map(id => deleteFrom(bankAccount).where(iban_ === id))
  private def buildUpdate(model: Company_):Update[Company_]  =
    update(company)
      .set(name, model.name)
      .set(street, model.street)
      .set(zip, model.zip)
      .set(city, model.city)
      .set(state, model.state)
      .set(country, model.country)
      .set(phone, model.phone)
      .set(email, model.email)
      .set(bankAcc, model.bankAcc)
      .set(vatCode, model.vatCode)
      .set(taxCode, model.taxCode)
      .set(currency, model.currency)
      .set(locale, model.locale)
      .set(balanceSheetAcc, model.balanceSheetAcc)
      .set(incomeStmtAcc, model.incomeStmtAcc)
      .where(id === model.id)
  @nowarn
  override def modify(model: Company): ZIO[Any, RepositoryError, Int] = {
    val oldBankAccounts = model.bankaccounts.filter(_.modelid == -2).map(_.copy(modelid = 12))
    val newBankAccounts = model.bankaccounts.filter(_.modelid == -3).map(_.copy(modelid = 12))
    val deleteBankAccounts = model.bankaccounts.filter(_.modelid == -1).map(_.id)
    val update_ = buildUpdate(Company_(model))
    val result = for {
      insertedBankAccounts <- ZIO.when(newBankAccounts.nonEmpty)(buildInsertBankAccount(newBankAccounts).run)
      updatedBankAccounts <- ZIO.when(oldBankAccounts.nonEmpty)(oldBankAccounts.map(ba => buildUpdateBankAccount(ba).run).flip.map(_.sum))
      deletedBankAccounts <- ZIO.when(deleteBankAccounts.nonEmpty)(buildDeleteBankAccount(deleteBankAccounts).map(_.run).flip.map(_.sum))
      updated <- update_.run
      _ <- ZIO.logInfo(s"New bank accounts insert stmt ${renderInsert(buildInsertBankAccount(newBankAccounts))}") *>
        ZIO.logInfo(s" bank accounts  update stmt ${oldBankAccounts.map(ba => renderUpdate(buildUpdateBankAccount(ba)))}") *>
        ZIO.logInfo(s" update company stm ${renderUpdate(update_)}") *>
        ZIO.logInfo(s"bank accounts to delete ${buildDeleteBankAccount(deleteBankAccounts).map(renderDelete)}")
    } yield insertedBankAccounts.getOrElse(0) + updatedBankAccounts.getOrElse(0) + deletedBankAccounts.getOrElse(0) + updated
    transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
  }

  override def all(modelid:Int): ZIO[Any, RepositoryError, List[Company]] = for{
    //list.runCollect.map(_.toList)
    companies <- list(modelid).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount().runCollect.map(_.toList)
  } yield companies.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))


  def listBankAccount(): ZStream[Any, RepositoryError, BankAccount] = {
    val selectAll = SELECT_BANK_ACCOUNT
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def list(modelId:Int): ZStream[Any, RepositoryError, Company]          = {
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
      ).from(company) .where(modelid === modelId)

     ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
    execute(selectAll.to[Company](c => Company.apply(c)))
      .provideDriver(driverLayer)
  }
  override def getBy(Id: String): ZIO[Any, RepositoryError, Company] = {
    execute(SELECT.where(id===Id).to[Company](c => Company.apply(c)))
      .findFirst(driverLayer, Id)
  }

  def getBy(ids: List[String]): ZIO[Any, RepositoryError, List[Company]] = for {
    vats <- getBy_(ids).runCollect.map(_.toList)
  } yield vats

  def getBy_(ids: List[String]): ZStream[Any, RepositoryError, Company] = {
    val selectAll = SELECT.where( id  in ids)
    execute(selectAll.to[Company](c => Company.apply(c)))
      .provideDriver(driverLayer)
  }

}

object CompanyRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CompanyRepository] =
    ZLayer.fromFunction(new CompanyRepositoryImpl(_))
}
