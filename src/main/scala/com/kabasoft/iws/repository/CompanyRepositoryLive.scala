package com.kabasoft.iws.repository
import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankAccount, Company, ModelId}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.interop.catz.*
import zio.prelude.FlipOps
import zio.{Task, ZIO, ZLayer}

import java.time.{Instant, LocalDateTime, ZoneId}

final case class CompanyRepositoryLive(postgres: Resource[Task, Session[Task]]
                                       , bankAccRepo:BankAccountRepository) extends CompanyRepository, MasterfileCRUD:

  import CompanyRepositorySQL._

  def transact(s: Session[Task], newCustomers: List[Company]): Task[Unit] =
    transact(s, newCustomers, newCustomers.flatMap(_.bankaccounts).filterNot(_.id.isEmpty)
      , insert, BankAccountRepositorySQL.insert)


  def transact(s: Session[Task], newCustomers: List[Company], newbankAccount: List[BankAccount], oldCustomers: List[Company]
               , oldbankAcc2Update: List[BankAccount], bankAcc2Delete: List[BankAccount]): Task[Unit] =
    transact(s, newCustomers, newbankAccount, oldCustomers.map(Company.encodeIt2)
      , oldbankAcc2Update.map(BankAccount.encodeIt2), bankAcc2Delete.map(BankAccount.encodeIt3)
      , insert, BankAccountRepositorySQL.insert, CompanyRepositorySQL.UPDATE, BankAccountRepositorySQL.UPDATE_BANK_ACCOUNT
      , BankAccountRepositorySQL.DELETE_BANK_ACCOUNT)


  override def create(c: Company): ZIO[Any, RepositoryError, Int] = create(List(c))
  override def create(models: List[Company]): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          transact(session, models))
      .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.bankaccounts).size + models.size)

  override def modify(model: Company): ZIO[Any, RepositoryError, Int] = modify(List(model))

  override def modify(models: List[Company]): ZIO[Any, RepositoryError, Int] = {
    val oldLines2Update = models.flatMap(_.bankaccounts).filter(bankAccount => bankAccount.modelid > 0
        && bankAccount.company.contains("-"))
      .map(bankAccount => bankAccount.copy(company = bankAccount.company.replace("-", "")))
    val newLine2Insert = models.flatMap(_.bankaccounts).filter(bankAccount => bankAccount.modelid === -1
        && bankAccount.company.contains("-") && bankAccount.id.nonEmpty)
      .map(bankAccount => bankAccount.copy(modelid = ModelId.BANK_ACCOUNT.modelid,
        company = bankAccount.company.replace("-", "")))
    val oldLine2Delete = models.flatMap(_.bankaccounts).filter(_.modelid === -2)
      .map(bankAccount => bankAccount.copy(company = bankAccount.company.replace("-", "")))
    ZIO.logInfo(s"models ${models}") *>
      ZIO.logInfo(s"oldLines2Update ${oldLines2Update}") *>
      ZIO.logInfo(s"newLine2Insert ${newLine2Insert}") *>
      ZIO.logInfo(s"oldLine2Delete ${oldLine2Delete}") *>
      postgres
        .use:
          session =>
            transact(session, List.empty, newLine2Insert, models, oldLines2Update, oldLine2Delete)
        .mapBoth(e => RepositoryError(e.getMessage), _ =>
          models.size + newLine2Insert.size + oldLines2Update.size + oldLine2Delete.size)
  }


  def list(p: Int): ZIO[Any, RepositoryError, List[Company]] =  queryWithTx(postgres, p, ALL)
  
  override def all(modelid: Int): ZIO[Any, RepositoryError, List[Company]] = for {
    companies <- list(modelid).map(_.toList)
    bankAccounts_ <- bankAccRepo.bankAccout4All(ModelId.BANK_ACCOUNT.modelid)
  } yield companies.map(c => c.copy(bankaccounts = bankAccounts_.filter( bac => bac.owner == c.id & bac.company == c.id)))
  
  override def getById(p: (String, Int)): ZIO[Any, RepositoryError, Company] =  for {
    comp <- queryWithTxUnique(postgres, p, BY_ID)
    bankAccounts_ <- bankAccRepo.getByOwner(comp.id, ModelId.BANK_ACCOUNT.modelid, comp.id)
  }yield comp.copy(bankaccounts =bankAccounts_)
  override def getBy(ids: List[String], modelid: Int):ZIO[Any, RepositoryError, List[Company]] =
    queryWithTx(postgres, (ids, modelid), ALL_BY_ID(ids.length))

  override def delete(p: (String, Int)):ZIO[Any, RepositoryError, Int] =  executeWithTx(postgres, p, DELETE, 1)
  override def deleteAll(p: List[(String, Int)]): ZIO[Any, RepositoryError, Int] = 
    p.map(l => executeWithTx(postgres, l, DELETE, 1)).flip.map(_.size)


object CompanyRepositoryLive:

  val live: ZLayer[Resource[Task, Session[Task]] & BankAccountRepository, RepositoryError, CompanyRepository] =
    ZLayer.fromFunction(new CompanyRepositoryLive(_, _))

private[repository] object CompanyRepositorySQL:
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (varchar *: varchar *: varchar *: varchar *: varchar  *: varchar *: varchar *: varchar *: varchar *: varchar  *:
     varchar *: varchar *: varchar *:
     varchar *: varchar *: varchar *: varchar *: varchar  *: varchar *: varchar *: varchar *: varchar *: varchar *: int4)

  private[repository] def encodeIt(st: Company): Company.TYPE2 =
    (st.id, st.name, st.street, st.zip, st.city, st.state, st.country, st.email, st.contact, st.phone, st.bankAcc,
      st.iban, st.taxCode, st.vatCode, st.currency, st.locale, st.account, st.oaccount, st.balanceSheetAcc
      , st.incomeStmtAcc, st.purchasingClearingAcc, st.salesClearingAcc, st.cashAcc, st.modelid)

  val mfDecoder: Decoder[Company] = mfCodec.map:
    case ( id, name, street, zip, city, state, country, email, contact, phone, bankAcc, iban, taxCode, vatCode
      , currency, locale, account, oaccount, balanceSheetAcc, incomeStmtAcc, purchasingClearingAcc, salesClearingAcc
      , cashAcc, modelid) => Company(id, name, street, zip, city, state, country, email, contact, phone, bankAcc, iban
      , taxCode, vatCode, currency, locale, account, oaccount, balanceSheetAcc, incomeStmtAcc, purchasingClearingAcc
      , salesClearingAcc, cashAcc, modelid)
  
  val mfEncoder: Encoder[Company] = mfCodec.values.contramap(encodeIt)

  def base =
    sql""" id, name, street, zip, city, state, country, email, contact, phone, bank_acc, iban, tax_code, vat_code
          , currency, locale, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc, sales_clearing_acc
          , cash_acc, modelid
           FROM   company ORDER BY id ASC"""

  def ALL_BY_ID(nr: Int): Query[(List[String], Int), Company] =
    sql"""SELECT id, name, street, zip, city, state, country, email, contact, phone, bank_acc, iban, tax_code, vat_code
          , currency, locale, account, oaccount, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc
          , sales_clearing_acc, cash_acc, modelid
           FROM   company
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4
           ORDER BY id ASC""".query(mfDecoder)

  val BY_ID: Query[String *: Int *: EmptyTuple, Company] =
    sql"""SELECT id, name, street, zip, city, state, country, email, contact, phone, bank_acc, iban, tax_code, vat_code
          , currency, locale, account, oaccount, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc
          , sales_clearing_acc, cash_acc, modelid
           FROM   company
           WHERE id = $varchar AND modelid = $int4
           ORDER BY id ASC""".query(mfDecoder)

  val ALL: Query[Int, Company] =
     sql"""SELECT id, name, street, zip, city, state, country, email, contact, phone, bank_acc, iban, tax_code, vat_code
          , currency, locale, account, oaccount, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc
          , sales_clearing_acc, cash_acc, modelid
           FROM   company
           WHERE  modelid = $int4
           ORDER BY id ASC""".query(mfDecoder)

  val insert: Command[Company] = sql"""INSERT INTO company 
    (id, name, street, zip, city, state, country, email, contact, phone, bank_acc, iban, tax_code, vat_code,
      currency, locale, account, oaccount, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc
      , sales_clearing_acc, cash_acc, modelid)
    VALUES $mfEncoder""".command
  
   def insertAll(n:Int):Command[List[Company.TYPE2]]=
     sql"""INSERT INTO company 
           (id, name, street, zip, city, state, country, email, contact, phone, bank_acc, iban, tax_code, vat_code,
             currency, locale, account, oaccount, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc
             , sales_clearing_acc, cash_acc, modelid)
         VALUES ${mfCodec.values.list(n)}""".command
  
  val UPDATE:Command[Company.TYPE2]=
    sql"""UPDATE company set name =$varchar, street =$varchar, zip =$varchar, city =$varchar, state =$varchar
          , country =$varchar, email =$varchar, contact =$varchar, phone =$varchar, bank_acc =$varchar, iban =$varchar
          , tax_code =$varchar, vat_code =$varchar, currency =$varchar, locale =$varchar, balance_sheet_acc =$varchar 
          , income_stmt_acc =$varchar, purchasing_clearing_acc =$varchar, sales_clearing_acc =$varchar,  cash_acc=$varchar
          , account=$varchar, oaccount=$varchar 
          WHERE id=$varchar and modelid=$int4""".command
  
  def DELETE: Command[(String, Int)] = sql"DELETE FROM Company WHERE id = $varchar AND modelid = $int4".command