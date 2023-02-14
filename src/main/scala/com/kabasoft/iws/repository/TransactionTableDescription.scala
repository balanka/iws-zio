package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{DerivedTransaction, FinancialsTransaction, FinancialsTransactionDetails, FinancialsTransactionDetails_, FinancialsTransaction_, FinancialsTransactionx}
import com.kabasoft.iws.repository.Schema.{derivedTransactionSchema, transactionDetailsSchema, transactionDetails_Schema, transactionSchema, transactionSchema_}

trait TransactionTableDescription extends IWSTableDescriptionPostgres {

  val transaction = defineTable[FinancialsTransactionx]("master_compta")
  val transaction2 = defineTable[FinancialsTransaction_]("master_compta")
  val transactionDetails = defineTable[FinancialsTransactionDetails]("details_compta")
  val transactionDetails_ = defineTable[FinancialsTransactionDetails_]("details_compta")
  val financialstransaction = defineTable[DerivedTransaction]("financialstransaction")

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
  ) = transaction2.columns

  val (
    id_,
    oid_,
    costcenter,
    account_,
    transdate_,
    enterdate_,
    postingdate_,
    period_,
    posted_,
    modelid_,
    company_,
    text_,
    type_journal,
    file_content_
    ) = transaction.columns
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
  ) = transactionDetails_.columns

  val (
    id,
    oid,
    account,
    transdate,
    enterdate,
    postingdate,
    period,
    posted,
    modelid,
    company,
    text,
    file,
    lid,
    side,
    oaccount,
    amount,
    currency,
    terms
  ) = financialstransaction.columns

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

  def toTuple(c: FinancialsTransactionDetails) = FinancialsTransactionDetails.unapply(c).get

  def toTuple(c: DerivedTransaction) = DerivedTransaction.unapply(c).get

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
