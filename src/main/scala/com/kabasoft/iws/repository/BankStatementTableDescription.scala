package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{BankStatement, BankStatement_}
import com.kabasoft.iws.api.Protocol.bankStatementschema
import com.kabasoft.iws.api.Protocol.bankStatement_schema


trait BankStatementTableDescription extends IWSTableDescriptionPostgres {


  val bankStatements = defineTable[BankStatement]("bankstatement")
  val bankStatementInsert = defineTable[BankStatement_]("bankstatement")

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

  def toTuple2(c: BankStatement_) = (
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
