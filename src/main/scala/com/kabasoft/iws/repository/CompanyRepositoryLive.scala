package com.kabasoft.iws.repository
import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.interop.catz.*
import zio.prelude.FlipOps
import zio.stream.ZStream
import zio.stream.interop.fs2z.*
import zio.{ Chunk, Task, UIO, ZIO, ZLayer }
import java.time.LocalDate
import com.kabasoft.iws.domain.{Company, BankAccount}
import com.kabasoft.iws.domain.AppError.RepositoryError


import java.time.{ Instant, LocalDateTime, ZoneId, ZoneOffset }

final case class CompanyRepositoryLive(postgres: Resource[Task, Session[Task]]
                                       , bankAccRepo:BankAccountRepository) extends CompanyRepository:

  import CompanyRepositorySQL._

  override def create(c: Company, flag: Boolean):ZIO[Any, RepositoryError, Int]=
    postgres
      .use: session =>
        session
          .prepare(if (flag) upsert else insert)
          .flatMap: cmd =>
            cmd.execute(c).void
      .mapBoth(e => RepositoryError(e.getMessage), _ => 1)

  override def create(list: List[Company]):ZIO[Any, RepositoryError, Int]=
    postgres
      .use: session =>
        session
          .prepare(insertAll(list.size))
          .flatMap: cmd =>
            cmd.execute(list.map(encodeIt)).void
      .mapBoth(e => RepositoryError(e.getMessage), _ => list.size)


  override def modify(model: Company):ZIO[Any, RepositoryError, Int] = create(model, true)

  override def modify(models: List[Company]):ZIO[Any, RepositoryError, Int]= models.map(modify).flip.map(_.sum)

   def list(p: Int): ZIO[Any, RepositoryError, List[Company]] =
    postgres
      .use: session =>
        session
          .prepare(ALL)
          .flatMap: ps =>
            ps.stream(p, 1024).compile.toList
      .mapBoth(e => RepositoryError(e.getMessage), list => list)

  override def all(modelid: Int): ZIO[Any, RepositoryError, List[Company]] = for {
    companies <- list(modelid).map(_.toList)
    bankAccounts_ <- bankAccRepo.bankAccout4All(BankAccount.MODEL_ID)
  } yield companies.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))


  override def getById(p: (String, Int)): ZIO[Any, RepositoryError, Company] =
    postgres
      .use: session =>
        session
          .prepare(BY_ID)
          .flatMap(ps => ps.unique(p._1, p._2))
      .mapBoth(e => RepositoryError(e.getMessage), a => a)

  override def getBy(ids: List[String], modelid: Int):ZIO[Any, RepositoryError, List[Company]] =
    postgres
      .use: session =>
        session
          .prepare(ALL_BY_ID(ids.length))
          .flatMap: ps =>
            ps.stream((ids, modelid), chunkSize = 1024)
              .compile
              .toList
      .mapBoth(e => RepositoryError(e.getMessage), a => a)

  def delete(p: (String, Int)):ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
        session
          .prepare(DELETE)
          .flatMap: cmd =>
            cmd.execute(p._1, p._2).void
      .mapBoth(e => RepositoryError(e.getMessage), _=> 1)


object CompanyRepositoryLive:

  val live: ZLayer[Resource[Task, Session[Task]] & BankAccountRepository, RepositoryError, CompanyRepository] =
    ZLayer.fromFunction(new CompanyRepositoryLive(_, _))

private[repository] object CompanyRepositorySQL:

  type TYPE = (String, String, String, String, String, String, String, String, String, String, String, String, String,
    String, String, String, String, String, String, String, Int)
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (varchar *: varchar *: varchar *: varchar *: varchar  *: varchar *: varchar *: varchar *: varchar *: varchar  *:
     varchar *: varchar *: varchar *: varchar *: varchar  *: varchar *: varchar *: varchar *: varchar *: varchar *: int4)

  private[repository] def encodeIt(st: Company): TYPE =
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

  val insert: Command[Company] = sql"INSERT INTO company VALUES $mfEncoder".command
  def insertAll(n:Int):Command[List[TYPE]]= sql"INSERT INTO company VALUES ${mfCodec.values.list(n)}".command
  val upsert: Command[Company] =
    sql"""INSERT INTO Company
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
            name                    = EXCLUDED.name,
            street                  = EXCLUDED.street,
            zip                     = EXCLUDED.zip,
            city                    = EXCLUDED.city,
            state                   = EXCLUDED.state,
            country                 = EXCLUDED.country,
            email                   = EXCLUDED.email,
            partner                 = EXCLUDED.partner,
            phone                   = EXCLUDED.phone,
            bank_acc                = EXCLUDED.bank_acc,
            iban                    = EXCLUDED.iban,
            tax_code                = EXCLUDED.tax_code,
            vat_code                = EXCLUDED.vat_code,
            currency                = EXCLUDED.currency,
            locale                  = EXCLUDED.locale,
            balance_sheet_acc       = EXCLUDED.balanceSheetAcc,
            income_stmt_acc         = EXCLUDED.income_stmt_acc,
            purchasing_clearing_acc = EXCLUDED.purchasing_clearing_acc,
            sales_clearing_acc      = EXCLUDED.sales_clearing_acc
          """.command

  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(String, Int)] = sql"DELETE FROM Company WHERE id = $varchar AND modelid = $int4".command