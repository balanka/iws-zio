package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, FinancialsTransactionDetails_, FinancialsTransaction_, FinancialsTransactionx}
import com.kabasoft.iws.repository.Schema.{transactionDetailsSchema, transactionDetails_Schema, transactionSchema_, transactionSchemax}
import zio.ZIO

import java.time.Instant

trait TransactionTableDescription extends IWSTableDescriptionPostgres {

  val transactions           = defineTable[FinancialsTransactionx]("master_compta")
  val transactionInsert     = defineTable[FinancialsTransaction_]("master_compta")
  val transactionDetails    = defineTable[FinancialsTransactionDetails]("details_compta")
  val transactionDetailsInsert   = defineTable[FinancialsTransactionDetails_]("details_compta")


  val (
    oidx,
    id1,
    costcenterx,
    accountx,
    transdatex,
    enterdatex,
    postingdatex,
    periodx_,
    postedx_,
    modelidx_,
    companyx_,
    textx,
    type_journalx,
    file_contentx
  ) = transactionInsert.columns

  val (
    id_,
    oid_,
    id1_,
    costcenter_,
    account_,
    transdate_,
    enterdate_,
    postingdate_,
    period_,
    posted_,
    modelid_,
    company_,
    text_,
    type_journal_,
    file_content_
  ) = transactions.columns
  val (
    lid_,
    transid,
    laccount_,
    side_,
    oaccount_,
    amount_,
    duedate_,
    ltext_,
    currency_,
    accountName_,
    oaccountName_
  ) = transactionDetails.columns

  val (transidx,
    laccountx,
    sidex,
    oaccountx,
    amountx,
    duedatex,
    ltextx,
    currencyx__,
  accountNamex,
  oaccountNamex
  ) = transactionDetailsInsert.columns




  def toTupleF(c: FinancialsTransaction) = (
    c.id,
    c.oid,
    c.id1,
    c.costcenter,
    c.account,
    c.transdate,
    c.enterdate,
    c.postingdate,
    c.period,
    c.posted,
    c.modelid,
    c.company,
    c.text,
    c.typeJournal,
    c.file_content
  )

  def toTupleC(c: FinancialsTransaction) = (
    c.oid,
    c.id1,
    c.costcenter,
    c.account,
    c.transdate,
    c.enterdate,
    c.postingdate,
    c.period,
    c.posted,
    c.modelid,
    c.company,
    c.text,
    c.typeJournal,
    c.file_content
  )

  def toTuple(c: FinancialsTransactionDetails) = (
    c.transid,
    c.account,
    c.side,
    c.oaccount,
    c.amount,
    c.duedate,
    c.text,
    c.currency,
    c.accountName,
    c.oaccountName
  )

  def toTuple2(c: FinancialsTransactionDetails_) = (
    c.transid,
    c.account,
    c.side,
    c.oaccount,
    c.amount,
    c.duedate,
    c.text,
    c.currency,
    c.accountName,
    c.oaccountName)

   def buildInsertNewLines(models: List[FinancialsTransactionDetails]): Insert[FinancialsTransactionDetails_, (Long, String, Boolean, String, java.math.BigDecimal, Instant, String, String, String, String)] =
    insertInto(transactionDetailsInsert)(transidx, laccountx, sidex, oaccountx, amountx, duedatex, ltextx, currencyx__, accountNamex, oaccountNamex)
      .values(models.map(toTuple))

   def buildInsertQuery(models: List[FinancialsTransaction]): Insert[FinancialsTransaction_, (Long, Long, String, String, Instant, Instant, Instant, Int, Boolean, Int, String, String, Int, Int)] =
    insertInto(transactionInsert)(
      oidx,
      id1,
      costcenterx,
      accountx,
      transdatex,
      enterdatex,
      postingdatex,
      periodx_,
      postedx_,
      modelidx_,
      companyx_,
      textx,
      type_journalx,
      file_contentx
    ).values(models.map(toTupleC))

  private def newCreate(): Long = {
    var time = Instant.now().getEpochSecond
    time *= 1000000000L //convert to nanoseconds
    val transid1x = time & ~9223372036854251520L
    transid1x
  }
  def create2s(transactions: List[FinancialsTransaction]): ZIO[SqlTransaction, Exception, Int] = {
    val models = transactions.zipWithIndex.map { case (ftr, i) =>
      val idx = newCreate()+i.toLong
      ftr.copy(id1 = idx  , lines = ftr.lines.map(_.copy(transid = idx )))
    }
    val allLines = models.flatMap(_.lines)
    val insertNewLines_ = buildInsertNewLines(allLines)
    val result = for {
      _ <- ZIO.logInfo(s"Create transaction stmt models      ${models}")
      _ <-ZIO.logInfo(s"Create line transaction stmt   ${renderInsert(insertNewLines_)} ")
      y <- insertNewLines_.run
      x <- buildInsertQuery(models).run

      _ <- ZIO.logInfo(s"Create transaction stmt       ${renderInsert(buildInsertQuery(models))} ") *>
        ZIO.logInfo(s"Create line transaction stmt   ${renderInsert(insertNewLines_)} ")
    } yield x + y
    result
  }

}
