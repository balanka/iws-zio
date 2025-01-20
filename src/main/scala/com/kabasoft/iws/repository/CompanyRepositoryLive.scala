package com.kabasoft.iws.repository
import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankAccount, Company}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.interop.catz.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}
import zio.prelude.FlipOps
import java.time.{Instant, LocalDateTime, ZoneId}

final case class CompanyRepositoryLive(postgres: Resource[Task, Session[Task]]
                                       , bankAccRepo:BankAccountRepository) extends CompanyRepository, MasterfileCRUD:

  import CompanyRepositorySQL.*

  def transact(s: Session[Task], newCustomers: List[Company]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciCustomer =>
        s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
          tryExec(xa, pciCustomer, pciBankAcc, newCustomers, newCustomers.flatMap(_.bankaccounts))

  def transact(s: Session[Task], newCustomers: List[Company], oldCustomers: List[Company]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciCustomer =>
        s.prepareR(CompanyRepositorySQL.UPDATE).use: pcuCustomer =>
          s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
            s.prepareR(BankAccountRepositorySQL.UPDATE).use: pcuBankAcc =>
              tryExec(xa, pciCustomer, pciBankAcc, pcuCustomer, pcuBankAcc, newCustomers
                , newCustomers.flatMap(_.bankaccounts), oldCustomers.map(Company.encodeIt2)
                , oldCustomers.flatMap(_.bankaccounts).map(BankAccount.encodeIt2))

  override def create(c: Company): ZIO[Any, RepositoryError, Int] = create(List(c))
  override def create(models: List[Company]): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          transact(session, models))
      .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.bankaccounts).size + models.size)

  override def modify(model: Company): ZIO[Any, RepositoryError, Int] = modify(List(model))

  override def modify(models: List[Company]): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          transact(session, List.empty, models))
      .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.bankaccounts).size + models.size)

  def list(p: Int): ZIO[Any, RepositoryError, List[Company]] =  queryWithTx(postgres, p, ALL)
  
  override def all(modelid: Int): ZIO[Any, RepositoryError, List[Company]] = for {
    companies <- list(modelid).map(_.toList)
    bankAccounts_ <- bankAccRepo.bankAccout4All(BankAccount.MODEL_ID)
  } yield companies.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))
  
  override def getById(p: (String, Int)): ZIO[Any, RepositoryError, Company] =  queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int):ZIO[Any, RepositoryError, List[Company]] =
    queryWithTx(postgres, (ids, modelid), ALL_BY_ID(ids.length))

  override def delete(p: (String, Int)):ZIO[Any, RepositoryError, Int] =  executeWithTx(postgres, p, DELETE, 1)
  override def deleteAll(p: List[(String, Int)]): ZIO[Any, RepositoryError, Int] = p.map(l => executeWithTx(postgres, l, DELETE, 1)).flip.map(_.size)


object CompanyRepositoryLive:

  val live: ZLayer[Resource[Task, Session[Task]] & BankAccountRepository, RepositoryError, CompanyRepository] =
    ZLayer.fromFunction(new CompanyRepositoryLive(_, _))

private[repository] object CompanyRepositorySQL:


  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (varchar *: varchar *: varchar *: varchar *: varchar  *: varchar *: varchar *: varchar *: varchar *: varchar  *:
     varchar *: varchar *: varchar *: varchar *: varchar  *: varchar *: varchar *: varchar *: varchar *: varchar *: int4)

  private[repository] def encodeIt(st: Company): Company.TYPE2 =
    (st.id, st.name, st.street, st.zip, st.city, st.state, st.country, st.email, st.partner, st.phone, st.bankAcc,
      st.iban, st.taxCode, st.vatCode, st.currency, st.locale, st.balanceSheetAcc, st.incomeStmtAcc, st.purchasingClearingAcc,
      st.salesClearingAcc, st.modelid)

  val mfDecoder: Decoder[Company] = mfCodec.map:
    case ( id, name, street, zip, city, state, country, email, partner, phone, bankAcc, iban, taxCode, vatCode,
    currency, locale, balanceSheetAcc, incomeStmtAcc, purchasingClearingAcc, salesClearingAcc, modelid) =>
      Company(id, name, street, zip, city, state, country, email, partner, phone, bankAcc, iban, taxCode, vatCode,
        currency, locale, balanceSheetAcc, incomeStmtAcc, purchasingClearingAcc, salesClearingAcc, modelid)


  val mfEncoder: Encoder[Company] = mfCodec.values.contramap(encodeIt)

  def base =
    sql""" id, name, street, zip, city, state, country, email, partner, phone, bank_acc, iban, tax_code, vat_code
          , currency, locale, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc, sales_clearing_acc, modelid
           FROM   company """

  def ALL_BY_ID(nr: Int): Query[(List[String], Int), Company] =
    sql"""SELECT id, name, street, zip, city, state, country, email, partner, phone, bank_acc, iban, tax_code, vat_code
          , currency, locale, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc, sales_clearing_acc, modelid
           FROM   company
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: EmptyTuple, Company] =
    sql"""SELECT id, name, street, zip, city, state, country, email, partner, phone, bank_acc, iban, tax_code, vat_code
          , currency, locale, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc, sales_clearing_acc, modelid
           FROM   company
           WHERE id = $varchar AND modelid = $int4
           """.query(mfDecoder)

  val ALL: Query[Int, Company] =
     sql"""SELECT id, name, street, zip, city, state, country, email, partner, phone, bank_acc, iban, tax_code, vat_code
          , currency, locale, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc, sales_clearing_acc, modelid
           FROM   company
           WHERE  modelid = $int4
           """.query(mfDecoder)

  val insert: Command[Company] = sql"""INSERT INTO company 
    (id, name, street, zip, city, state, country, email, partner, phone, bank_acc, iban, tax_code, vat_code,
      currency, locale, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc, sales_clearing_acc, modelid)
    VALUES $mfEncoder""".command
  
   def insertAll(n:Int):Command[List[Company.TYPE2]]=
     sql"""INSERT INTO company 
           (id, name, street, zip, city, state, country, email, partner, phone, bank_acc, iban, tax_code, vat_code,
             currency, locale, balance_sheet_acc, income_stmt_acc, purchasing_clearing_acc, sales_clearing_acc, modelid)
         VALUES ${mfCodec.values.list(n)}""".command
     
  val UPDATE:Command[Company.TYPE2]=
    sql"""UPDATE company set name =$varchar, street =$varchar, zip =$varchar, city =$varchar, state =$varchar
          , country =$varchar, email =$varchar, partner =$varchar, phone =$varchar, bank_acc =$varchar, iban =$varchar
          , tax_code =$varchar, vat_code =$varchar, currency =$varchar, locale =$varchar, balance_sheet_acc =$varchar
          , income_stmt_acc =$varchar, purchasing_clearing_acc =$varchar, sales_clearing_acc =$varchar
          WHERE id=$varchar and modelid=$int4""".command
  
  def DELETE: Command[(String, Int)] = sql"DELETE FROM Company WHERE id = $varchar AND modelid = $int4".command