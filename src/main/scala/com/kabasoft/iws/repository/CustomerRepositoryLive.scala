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
import com.kabasoft.iws.domain.{BankAccount, Customer}

import java.time.{Instant, LocalDateTime, ZoneId}

final case class CustomerRepositoryLive(postgres: Resource[Task, Session[Task]]
                                        , bankAccRepo:BankAccountRepository) extends CustomerRepository, MasterfileCRUD:
    import CustomerRepositorySQL._

    override def create(c: Customer, flag: Boolean):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, if (flag) upsert else insert, 1)
    override def create(list: List[Customer]):ZIO[Any, RepositoryError, Int] =
      executeWithTx(postgres, list.map(Customer.encodeIt), insertAll(list.size), list.size)
    override def modify(model: Customer):ZIO[Any, RepositoryError, Int] = create(model, true)
    override def modify(models: List[Customer]):ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.sum)
    override def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Customer]] = for {
                  customer <- list(Id)
                  bankAccounts_ <- bankAccRepo.bankAccout4All(BankAccount.MODEL_ID)
             } yield customer.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))
    
    private def list(p: (Int, String)): ZIO[Any, RepositoryError, List[Customer]] =  queryWithTx(postgres, p, ALL)
    override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Customer] = queryWithTxUnique(postgres, p, BY_ID)
    override def getByIban(p: (String, Int, String)): ZIO[Any, RepositoryError, Customer] = queryWithTxUnique(postgres, p, BY_IBAN)
    override def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Customer]] =
      queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
    def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

object CustomerRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & BankAccountRepository, RepositoryError, CustomerRepository] =
    ZLayer.fromFunction(new CustomerRepositoryLive(_, _))

private[repository] object CustomerRepositorySQL:

    private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
      localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

    private val mfCodec =
        (varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *:varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: int4 *:timestamp *: timestamp *: timestamp)

    val mfDecoder: Decoder[Customer] = mfCodec.map:
      case (id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company, modelid, enterdate, changedate, postingdate) =>
        Customer.apply(
          (id, name, description, street, zip, city, state, country, phone, email, account, oaccount,
             vatcode, company, modelid, toInstant(enterdate), toInstant(changedate), toInstant(postingdate)))
  
    val mfEncoder: Encoder[Customer] = mfCodec.values.contramap(Customer.encodeIt)
  
    def base =
      sql""" SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
             , modelid, enterdate, changedate, postingdate
             FROM   customer """

    def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Customer] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
             , modelid, enterdate, changedate, postingdate
             FROM   customer
             WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val BY_IBAN: Query[String *: Int *: String *: EmptyTuple, Customer] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
             , modelid, enterdate, changedate, postingdate
                 FROM   customer
                 WHERE iban = $varchar AND modelid = $int4 AND company = $varchar
                 """.query(mfDecoder)
  
    val BY_ID: Query[String *: Int *: String *: EmptyTuple, Customer] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
             , modelid, enterdate, changedate, postingdate
             FROM   customer
             WHERE id = $varchar AND modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val ALL: Query[Int *: String *: EmptyTuple, Customer] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
             , modelid, enterdate, changedate, postingdate
             FROM   customer
             WHERE  modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val insert: Command[Customer] = sql"""INSERT INTO customer VALUES $mfEncoder """.command

    def insertAll(n: Int): Command[List[Customer.TYPE2]] = sql"INSERT INTO customer VALUES ${mfCodec.values.list(n)}".command

    val upsert: Command[Customer] =
      sql"""INSERT INTO customer
             VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
             id                   = EXCLUDED.id,
             name                 = EXCLUDED.name,
             description          = EXCLUDED.description,
              account          = EXCLUDED.account,
              enterdate            = EXCLUDED.enterdate,
              changedate            = EXCLUDED.changedate,
              postingdate            = EXCLUDED.postingdate,
              company              = EXCLUDED.company,
              modelid              = EXCLUDED.modelid,
            """.command

    def DELETE: Command[(String, Int, String)] =
      sql"DELETE FROM customer WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command