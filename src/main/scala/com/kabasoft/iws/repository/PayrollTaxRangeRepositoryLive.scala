package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}

import com.kabasoft.iws.repository.Schema.{payrollTaxRangeSchema, repositoryErrorSchema}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.PayrollTaxRange
final case  class PayrollTaxRangeRepositoryLive(postgres: Resource[Task, Session[Task]]) extends PayrollTaxRangeRepository, MasterfileCRUD:

  import PayrollTaxRangeRepositorySQL._

  override def create(c: PayrollTaxRange, flag: Boolean):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, if (flag) upsert else insert, 1)
  override def create(list: List[PayrollTaxRange]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(PayrollTaxRange.encodeIt), insertAll(list.size), list.size)
  override def modify(model: PayrollTaxRange):ZIO[Any, RepositoryError, Int] = create(model, true)
  override def modify(models: List[PayrollTaxRange]):ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.sum)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[PayrollTaxRange]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, PayrollTaxRange] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[PayrollTaxRange]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))

  override def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

object PayrollTaxRangeRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], Throwable, PayrollTaxRangeRepository] =
    ZLayer.fromFunction(new PayrollTaxRangeRepositoryLive(_))

private[repository] object PayrollTaxRangeRepositorySQL:
  private val mfCodec =
    (varchar *: numeric(12,2) *: numeric(12,2) *:numeric(12,2) *:  varchar  *: varchar *: int4)

  val mfDecoder: Decoder[PayrollTaxRange] = mfCodec.map:
    case (id, fromAmount, toAmount, tax, taxClass, company, modelid) =>
      PayrollTaxRange(id, fromAmount.bigDecimal, toAmount.bigDecimal, tax.bigDecimal, taxClass, modelid, company)

  val mfEncoder: Encoder[PayrollTaxRange] = mfCodec.values.contramap(PayrollTaxRange.encodeIt)

  def base =
    sql""" SELECT id, from_amount, to_amount, tax, taxClass, company, modelid
            FROM   payroll_tax_range """

  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), PayrollTaxRange] =
    sql"""SELECT id, from_amount, to_amount, tax, tax_class, company, modelid
            FROM   payroll_tax_range
            WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
            """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, PayrollTaxRange] =
    sql"""SELECT id, from_amount, to_amount, tax, tax_class, company, modelid
            FROM   payroll_tax_range
            WHERE id = $varchar AND modelid = $int4 AND company = $varchar
            """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, PayrollTaxRange] =
    sql"""SELECT id, from_amount, to_amount, tax, tax_class, company, modelid
            FROM   payroll_tax_range
            WHERE  modelid = $int4 AND company = $varchar
            """.query(mfDecoder)

  val insert: Command[PayrollTaxRange] = sql"""INSERT INTO payroll_tax_range VALUES $mfEncoder """.command

  def insertAll(n: Int): Command[List[PayrollTaxRange.TYPE]] = sql"INSERT INTO payroll_tax_range VALUES ${mfCodec.values.list(n)}".command

  val upsert: Command[PayrollTaxRange] =
    sql"""INSERT INTO payroll_tax_range
            VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
            id                     = EXCLUDED.id,
            from_amount            = EXCLUDED.from_amount,
            to_amount              = EXCLUDED.to_amount,
             tax                   = EXCLUDED.tax,
             tax_class             = EXCLUDED.tax_class,
             company               = EXCLUDED.company,
             modelid               = EXCLUDED.modelid,
           """.command

  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM payroll_tax_range WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command  