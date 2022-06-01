package com.kabasoft.iws.repository

import com.kabasoft.iws.domain._

trait BankStatementTableDescription extends IWSTableDescriptionPostgres {

  import ColumnSet._

  val bankStatements = (long("id") ++ string("depositor") ++ instant("postingdate") ++ instant("valuedate")
    ++ string("postingtext") ++ string("purpose") ++ string("beneficiary") ++ string("accountno")
    ++ string("bankcode") ++ bigDecimal("amount") ++ string("currency") ++ string("info") ++ string(
      "company"
    ) ++ string("companyiban") ++ boolean("posted") ++ int("modelid"))
    .table("bankstatement")

  val bankStatementInsert = (string("depositor") ++ instant("postingdate") ++ instant("valuedate")
    ++ string("postingtext") ++ string("purpose") ++ string("beneficiary") ++ string("accountno")
    ++ string("bankcode") ++ bigDecimal("amount") ++ string("currency") ++ string("info") ++ string(
      "company"
    ) ++ string("companyiban") ++ boolean("posted") ++ int("modelid"))
    .table("bankstatement")

  val (
    id,
    depositor,
    postingdate,
    valuedate,
    postingtext,
    purpose,
    beneficiary,
    accountno,
    bankCode,
    amount,
    currency,
    info,
    company,
    companyIban,
    posted,
    modelid
  ) = bankStatements.columns

  val (
    depositor_,
    postingdate_,
    valuedate_,
    postingtext_,
    purpose_,
    beneficiary_,
    accountno_,
    bankCode_,
    amount_,
    currency_,
    info_,
    company_,
    companyIban_,
    posted_,
    modelid_
  ) = bankStatementInsert.columns

  def toTuple2(c: BankStatement) = (
    c.depositor,
    c.postingdate,
    c.valuedate,
    c.postingtext,
    c.purpose,
    c.beneficiary,
    c.accountno,
    c.bankCode,
    c.amount,
    c.currency,
    c.info,
    c.company,
    c.companyIban,
    c.posted,
    c.modelid
  )

}
