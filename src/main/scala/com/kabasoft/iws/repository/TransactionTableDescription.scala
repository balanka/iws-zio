package com.kabasoft.iws.repository

import com.kabasoft.iws.domain._

trait TransactionTableDescription extends IWSTableDescriptionPostgres {

  import ColumnSet._

  val transaction =
    (long("id") ++ long("oid") ++ string("account") ++ string("costcenter") ++ instant("transdate") ++ instant(
      "enterdate"
    )
      ++ instant("postingdate") ++ int("period") ++ boolean("posted") ++ int("modelid") ++ string("company") ++ string(
        "headertext"
      ) ++ int("typeJournal") ++ int("file_content"))
      .table("master_compta")

  val transactionDetails =
    (long("id") ++ long("transid") ++ string("account") ++ boolean("side") ++ string("oaccount") ++ bigDecimal(
      "amount"
    ) ++ instant("duedate")
      ++ string("text") ++ string("currency")) // ++string("terms")++boolean("posted")++string("company"))
      .table("details_compta")

  val financialstransaction =
    (long("id") ++ long("oid") ++ string("account") ++ instant("transdate") ++ instant("enterdate") ++ instant(
      "postingdate"
    )
      ++ int("period") ++ boolean("posted") ++ int("modelid") ++ string("company") ++ string("text") ++ int(
        "file"
      ) ++ long("lid") ++ boolean("side")
      ++ string("oaccount") ++ bigDecimal("amount") ++ string("currency") ++ string("terms"))
      .table("financialstransaction")

  val (
    tid_,
    oid_,
    account_,
    costcenter,
    transdate_,
    enterdate_,
    postingdate_,
    period_,
    posted_,
    modelid_,
    company_,
    text_,
    typeJournal_,
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
    c.tid,
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

  // def toTuple(c:DerivedTransaction)= DerivedTransaction.unapply(c).get
  def toTuple(c: DerivedTransaction) = (
    c.id,
    c.oid,
    c.account,
    c.transdate,
    c.enterdate,
    c.postingdate,
    c.period,
    c.modelid,
    c.posted,
    c.company,
    c.text,
    c.file,
    c.lid,
    c.side,
    c.oaccount,
    c.amount,
    c.currency,
    c.terms
  )

}
