package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{Account, FinancialsTransaction, FinancialsTransactionDetails, FinancialsTransactionDetails_, FinancialsTransaction_, FinancialsTransactionx, Transaction, TransactionDetails, TransactionDetails_, Transaction_, Transactionx, common}
import com.kabasoft.iws.repository.Schema.{transactionDetailsSchema, transactionDetailsSchema_, transactionSchema_, transactionSchemax}
import zio.ZIO
import zio.prelude.FlipOps

import java.time.Instant

trait TransactionTableDescription extends IWSTableDescriptionPostgres {

  val transactions           = defineTable[Transactionx]("transaction")
  val transactionInsert     = defineTable[Transaction_]("transaction")
  val transactionDetails    = defineTable[TransactionDetails]("transaction_details")
  val transactionDetailsInsert   = defineTable[TransactionDetails_]("transaction_details")

  val (
    oidx,
    id1,
    storex,
    accountx,
    transdatex,
    enterdatex,
    postingdatex,
    periodx_,
    postedx_,
    modelidx_,
    companyx_,
    textx_,
  ) = transactionInsert.columns

  val (
    id_,
    oid_,
    id1_,
    store_,
    account_,
    transdate_,
    enterdate_,
    postingdate_,
    period_,
    posted_,
    modelid_,
    company_,
    text_
  ) = transactions.columns


  val (
    lid_,
    transid,
    article_,
    quantity_,
    unit_,
    price_,
    currency_,
    duedate_,
    ltext_
  ) = transactionDetails.columns

  val (transidx,
     articlex,
     quantityx,
     unitx,
     pricex,
     currencyx,
     duedatex,
     textx
  ) = transactionDetailsInsert.columns

  private def toTupleC(c: Transaction) = (
    c.oid,
    c.id1,
    c.store,
    c.account,
    c.transdate,
    c.enterdate,
    c.postingdate,
    c.period,
    c.posted,
    c.modelid,
    c.company,
    c.text)

  private  def toTuple(c: TransactionDetails) = (
    c.transid,
    c.article,
    c.quantity,
    c.unit,
    c.price,
    c.currency,
    c.duedate,
    c.text
  )
   def buildInsertNewLine(model_ : TransactionDetails): Insert[TransactionDetails_, (Long, TableName, java.math.BigDecimal, TableName, java.math.BigDecimal, TableName, Instant, TableName)] = {
     println(s"model_ ${model_} ")
    val insertStmt = insertInto(transactionDetailsInsert)(transidx, articlex, quantityx, unitx, pricex, currencyx, duedatex, textx)
      .values(toTuple(model_))
     println(s"renderInsert(insertStmt ${renderInsert(insertStmt)}")
     insertStmt
   }

  def buildInsertNewLines(models: List[TransactionDetails]): List[Insert[TransactionDetails_, (Long, TableName, java.math.BigDecimal, TableName, java.math.BigDecimal, TableName, Instant, TableName)]] =
     models.map( model =>buildInsertNewLine(model))

   def buildInsertQuery(models: List[Transaction]): Insert[Transaction_, (Long, Long, TableName, TableName, Instant, Instant, Instant, Int, Boolean, Int, TableName, TableName)] =
    insertInto(transactionInsert)(
      oidx,
      id1,
      storex,
      accountx,
      transdatex,
      enterdatex,
      postingdatex,
      periodx_,
      postedx_,
      modelidx_,
      companyx_,
      textx_
    ).values(models.map(toTupleC))

  private def newCreate(): Long = {
    var time = Instant.now().getEpochSecond
    time *= 1000000000L //convert to nanoseconds
    val transid1x = time & ~9223372036854251520L
    transid1x
  }

  def buildId1(transactions: List[Transaction]): List[Transaction] =
     transactions.zipWithIndex.map { case (ftr, i) =>
      val idx = newCreate() + i.toLong
      ftr.copy(id1 = idx, lines = ftr.lines.map(_.copy(transid = idx)), period = common.getPeriod(ftr.transdate))
    }
  def create2s(models: List[Transaction]): ZIO[SqlTransaction, Exception, Int] = {
    val allLines = models.flatMap(_.lines)

    val result = for {
      _ <- ZIO.logInfo(s"Create transaction stmt models      ${models}")
      newLines = buildInsertNewLines(allLines)
      insertNewLines_ = newLines.map(_.run).flip.map(_.size)
      x <- buildInsertQuery(models).run
      y <- insertNewLines_
      _ <- ZIO.logInfo(s"Create transaction stmt       ${renderInsert(buildInsertQuery(models))} "+
         s"Create line transaction stmt   ${newLines.map(l=>renderInsert(l))} " +
        s" x ==   ${x}  y ==${y}")
    } yield x + y
    result.mapError(e => new Exception(e))
  }

}
