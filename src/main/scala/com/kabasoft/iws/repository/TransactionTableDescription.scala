package com.kabasoft.iws.repository

import com.kabasoft.iws.domain._

trait TransactionTableDescription extends IWSTableDescriptionPostgres {

  import ColumnSet._

  val master_compta_sequence  = (long("last_value")).table("master_compta_id_seq")

  val transaction =
    (long("id") ++ long("oid") ++ string("costcenter") ++ string("account") ++ instant("transdate") ++ instant(
      "enterdate"
    ) ++ instant("postingdate") ++ int("period") ++ boolean("posted") ++ int("modelid")
      ++ string("company") ++ string("headertext") ++ int("type_journal") ++ int("file_content")).table("master_compta")
  val transaction2 =
    ( long("oid") ++ string("costcenter") ++ string("account") ++ instant("transdate") ++ instant(
      "enterdate"
    ) ++ instant("postingdate") ++ int("period") ++ boolean("posted") ++ int("modelid")
      ++ string("company") ++ string("headertext") ++ int("type_journal") ++ int("file_content"))
      .table("master_compta")

  val transactionDetails =
    (long("id") ++ long("transid") ++ string("account") ++ boolean("side") ++ string("oaccount") ++ bigDecimal(
      "amount"
    ) ++ instant("duedate") ++ string("text") ++ string(
      "currency"
    )) // ++string("terms")++boolean("posted")++string("company"))
      .table("details_compta")

  val transactionDetailsx =
    ( long("transid") ++ string("account") ++ boolean("side") ++ string("oaccount") ++ bigDecimal(
      "amount") ++ instant("duedate") ++ string("text") ++ string(
      "currency")
      ).table("details_compta")

  val financialstransaction =
    (long("id") ++ long("oid") ++ string("account") ++ instant("transdate") ++ instant("enterdate") ++ instant(
      "postingdate"
    )
      ++ int("period") ++ boolean("posted") ++ int("modelid") ++ string("company") ++ string("text") ++ int(
        "file"
      ) ++ long("lid") ++ boolean("side")
      ++ string("oaccount") ++ bigDecimal("amount") ++ string("currency") ++ string("terms"))
      .table("financialstransaction")

 val (lastTransid) = master_compta_sequence.columns
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
    tid_,
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
  ) = transactionDetailsx.columns

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
