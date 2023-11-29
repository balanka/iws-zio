package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{Account, FinancialsTransaction, FinancialsTransactionDetails, FinancialsTransactionDetails_, FinancialsTransaction_, FinancialsTransactionx, common}
import com.kabasoft.iws.repository.Schema.{/*transactionDetailsSchema,*/ financialsRransactionDetailsSchema, transactionDetails_Schema, transactionSchema_, transactionSchemax}
import zio.ZIO
import zio.prelude.FlipOps

import java.time.Instant

trait TransactionTableDescription extends IWSTableDescriptionPostgres {

  val transactions           = defineTable[FinancialsTransactionx]("master_compta")
  val transactionInsert     = defineTable[FinancialsTransaction_]("master_compta")
  val ftransactionDetails    = defineTable[FinancialsTransactionDetails]("details_compta")
  val ftransactionDetailsInsert   = defineTable[FinancialsTransactionDetails_]("details_compta")

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
  ) = ftransactionDetails.columns

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
  ) = ftransactionDetailsInsert.columns

  private def toTupleC(c: FinancialsTransaction) = (
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

  private  def toTuple(c: FinancialsTransactionDetails) = (
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
   def buildInsertNewLine(model_ : FinancialsTransactionDetails, accounts:List[Account]): Insert[FinancialsTransactionDetails_, (Long, TableName, Boolean, TableName, java.math.BigDecimal, Instant, TableName, TableName, TableName, TableName)] = {
     val acc = accounts.find(acc => acc.id == model_.account)
     val oacc = accounts.find(acc => acc.id == model_.oaccount)
     println(s"model_ ${model_} ")
     println(s"account ${acc} oaccount ${oacc} size ${accounts.size}")
     println(s"accounts >>>>> ${accounts}")
     val model = model_.copy(accountName = acc.fold(model_.accountName)(acc=>acc.name),
                             oaccountName = oacc.fold(model_.oaccountName)(acc=>acc.name))
    val insertStmt = insertInto(ftransactionDetailsInsert)(transidx, laccountx, sidex, oaccountx, amountx, duedatex, ltextx, currencyx__, accountNamex, oaccountNamex)
      .values(toTuple(model))
     println(s"renderInsert(insertStmt ${renderInsert(insertStmt)}")
     insertStmt
   }

  def buildInsertNewLines(models: List[FinancialsTransactionDetails], accounts:List[Account]): List[Insert[FinancialsTransactionDetails_, (Long, TableName, Boolean, TableName, java.math.BigDecimal, Instant, TableName, TableName, TableName, TableName)]] =
     models.map( model =>buildInsertNewLine(model, accounts))//.map(_.run).flip.map(_.size)
     //insertInto(transactionDetailsInsert)(transidx, laccountx, sidex, oaccountx, amountx, duedatex, ltextx, currencyx__, accountNamex, oaccountNamex)
     // .values(models.map(toTuple))

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

  def buildId1(transactions: List[FinancialsTransaction]): List[FinancialsTransaction] =
     transactions.zipWithIndex.map { case (ftr, i) =>
      val idx = newCreate() + i.toLong
      ftr.copy(id1 = idx, lines = ftr.lines.map(_.copy(transid = idx)), period = common.getPeriod(ftr.transdate))
    }
  def create2s(models: List[FinancialsTransaction], accounts:List[Account]): ZIO[SqlTransaction, Exception, Int] = {
//    val models = transactions.zipWithIndex.map { case (ftr, i) =>
//      val idx = newCreate()+i.toLong
//      ftr.copy(id1 = idx, lines = ftr.lines.map(_.copy(transid = idx )), period=common.getPeriod(ftr.transdate))
//    }
    val allLines = models.flatMap(_.lines)
    //val ids = transactions.flatMap(tr=>tr.lines.map(_.account))++transactions.flatMap(tr=>tr.lines.map(_.oaccount))
    val result = for {
      _ <- ZIO.logInfo(s"Create transaction stmt models      ${models}")
      //_ <-ZIO.logInfo(s"Create line transaction stmt   ${allLines.map(buildInsertNewLine).map(renderInsert)} ")
      insertNewLines_ = buildInsertNewLines(allLines, accounts).map(_.run).flip.map(_.size)
      x <- buildInsertQuery(models).run
      y <- insertNewLines_
      _ <- ZIO.logInfo(s"Create transaction stmt       ${renderInsert(buildInsertQuery(models))} ")
      //*>ZIO.logInfo(s"Create line transaction stmt   ${insertNewLines_.map(renderInsert)} ")
    } yield x + y
    result.mapError(e => new Exception(e))
  }

}
