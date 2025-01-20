package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.interop.catz.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.BankAccount
import com.kabasoft.iws.repository.MasterfileCRUD.InsertBatch


final case class BankAccountRepositoryLive(postgres: Resource[Task, Session[Task]]) extends BankAccountRepository, MasterfileCRUD:

  import BankAccountRepositorySQL.*

  override def create(c: BankAccount):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, insert, 1)
  override def create(list: List[BankAccount]):ZIO[Any, RepositoryError, Int] = {
    val insertBankAccountsCmd = BankAccountRepositorySQL.insertAll(list.size)
    //val insertBankAccountsCmd = BankAccountRepositorySQL.insertAll(c.bankaccounts.size)
    val createBankAccountsCmd = InsertBatch(list, BankAccount.encodeIt, insertBankAccountsCmd)
    //val createCustomerCmd = InsertBatch(List., Customer.encodeIt, insertCustomerCmd)
    postgres.use: session =>
      executeBatchWithTZ(session, List.empty, List(createBankAccountsCmd))
    .mapBoth(e => RepositoryError(e.getMessage), r1 => r1)
  
  //executeBatchWithTZ(postgres, List(createCustomerCmd),  List(createBankAccountsCmd))
  //executeBatchWithTx3(postgres, List.empty, List(createCustomerCmd), List.empty, List(createBankAccountsCmd), List.empty)
 // ZIO.succeed(models.size + models.flatMap(_.bankaccounts).size)
}  
  //executeWithTx(postgres, list.map(BankAccount.encodeIt), insertAll(list.size), list.size)
  override def modify(model: BankAccount):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, model, BankAccount.encodeIt2, UPDATE, 1)
  override def modify(models: List[BankAccount]):ZIO[Any, RepositoryError, Int]= executeBatchWithTxK(postgres, models, UPDATE, BankAccount.encodeIt2)
  override def bankAccout4All(p: Int): ZIO[Any, RepositoryError, List[BankAccount]] = queryWithTx(postgres, p, BANK_ACCOUNT_4_ALL)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[BankAccount]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, BankAccount] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[BankAccount]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))

  override def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
  override def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] = p.map(l => executeWithTx(postgres, l, DELETE, 1)).flip.map(_.size)


object BankAccountRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, BankAccountRepository] =
    ZLayer.fromFunction(new BankAccountRepositoryLive(_))

object BankAccountRepositorySQL:
  
  private val mfCodec = (varchar *: varchar *: varchar *: varchar  *: int4)
  val mfDecoder: Decoder[BankAccount] = mfCodec.map:
    case (id, bic, owner, company, modelid) => BankAccount(id, bic, owner, company, modelid)

  val mfEncoder: Encoder[BankAccount] = mfCodec.values.contramap(BankAccount.encodeIt) 

  def base =
    sql""" SELECT id, bic, owner, company, modelid
           FROM   bankaccount """
  
  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), BankAccount] =
    sql"""
           SELECT id, bic, owner, company, modelid
           FROM   bankaccount
           WHERE id  IN ( ${varchar.list(nr)}) AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, BankAccount] =
    sql"""
           SELECT id, bic, owner, company, modelid
           FROM   bankaccount
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BANK_ACCOUNT_4_ALL: Query[Int, BankAccount] =
    sql"""SELECT id, bic, owner, company, modelid
           FROM   bankaccount
           WHERE  modelid = $int4
           """.query(mfDecoder)
    
  val ALL: Query[Int *: String *: EmptyTuple, BankAccount] =
    sql"""SELECT id, bic, owner, company, modelid
           FROM   bankaccount
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[BankAccount] = sql"""INSERT INTO bankaccount (id, bic, owner, company, modelid) VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[BankAccount.TYPE]] =
    sql"INSERT INTO bankaccount (id, owner, bic, company, modelid) VALUES ${mfCodec.values.list(n)}".command

  val UPDATE: Command[BankAccount.TYPE2] =
    sql"""UPDATE bankaccount SET bic = $varchar, owner = $varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command
  
  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM bankaccount WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
