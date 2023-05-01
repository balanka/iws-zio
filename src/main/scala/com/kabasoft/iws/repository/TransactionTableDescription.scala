package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{
  FinancialsTransaction,
  FinancialsTransactionDetails,
  FinancialsTransactionDetails_,
  FinancialsTransaction_,
  FinancialsTransactionx
}
import com.kabasoft.iws.repository.Schema.{
  transactionDetailsSchema,
  transactionDetails_Schema,
  transactionSchema_,
  transactionSchemax
}

trait TransactionTableDescription extends IWSTableDescriptionPostgres {

  val transactions           = defineTable[FinancialsTransactionx]("master_compta")
  val transactionInsert     = defineTable[FinancialsTransaction_]("master_compta")
  val transactionDetails    = defineTable[FinancialsTransactionDetails]("details_compta")
  val transactionDetailsInsert   = defineTable[FinancialsTransactionDetails_]("details_compta")


  val (
    oidx,
    costcenterx,
    accountx,
    transdatex,
    enterdatex,
    postingdatex,
    periodx,
    postedx,
    modelidx,
    companyx,
    textx,
    type_journalx,
    file_contentx
  ) = transactionInsert.columns

  val (
    id_,
    oid_,
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
    currency_ /*, terms_, postedx, comapnyx*/
  ) = transactionDetails.columns

  val (
    transidx,
    laccountx,
    sidex,
    oaccountx,
    amountx,
    duedatex,
    ltextx,
    currencyx /*, terms_, postedx, comapnyx*/
  ) = transactionDetailsInsert.columns


  def toTupleF(c: FinancialsTransaction) = (
    c.id,
    c.oid,
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
    c.currency)

  def toTupleC(c: FinancialsTransactionDetails) = (
    c.transid,
    c.account,
    c.side,
    c.oaccount,
    c.amount,
    c.duedate,
    c.text,
    c.currency /*, terms_, postedx, comapnyx*/
  )
}
