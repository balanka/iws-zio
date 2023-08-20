package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{ BankStatement, BankStatement_ }
import com.kabasoft.iws.repository.Schema.{ bankStatementsSchema, bankStatementsSchema_ }
trait BankStatementTableDescription extends IWSTableDescriptionPostgres {

  val bankStatements      = defineTable[BankStatement]("bankstatement")
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
    modelid,
    period
  ) = bankStatements.columns

  val (
    depositor_bs,
    postingdatex_bs,
    valuedatex_bs,
    postingtextx_bs,
    purposex_bs,
    beneficiaryx_bs,
    accountnox_bs,
    bankCodex_bs,
    amountx_bs,
    currencyx_bs,
    infox_bs,
    companyx_bs,
    companyIbanx_bs,
    postedx_bs,
    modelidx_bs,
    periodx_bs
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
    c.modelid,
    c.period
  )

}
