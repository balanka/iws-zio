package com.kabasoft.iws.repository

import com.kabasoft.iws.domain._
import zio.schema.DeriveSchema

object Schema {
  implicit lazy val account_schema = DeriveSchema.gen[Account_]
  implicit val bankSchema = DeriveSchema.gen[Bank]
  implicit val bankStatementsSchema = DeriveSchema.gen[BankStatement]
  implicit val bankStatementsSchema_ = DeriveSchema.gen[BankStatement_]
  implicit val companySchema = DeriveSchema.gen[Company]
  implicit val costcenterSchema = DeriveSchema.gen[Costcenter]
  implicit val customerSchema = DeriveSchema.gen[Customer_]
  implicit val bankAccountSchema = DeriveSchema.gen[BankAccount]
  implicit val journalSchema = DeriveSchema.gen[Journal]
  implicit val journal_Schema = DeriveSchema.gen[Journal_]
  implicit val moduleSchema = DeriveSchema.gen[Module]
  implicit val pacSchema = DeriveSchema.gen[PeriodicAccountBalance]
  implicit val supplierSchema = DeriveSchema.gen[Supplier_]
  implicit val transactionSchema = DeriveSchema.gen[FinancialsTransactionx]
  implicit val transactionSchema_ = DeriveSchema.gen[FinancialsTransaction_]
  implicit val derivedTransactionSchema = DeriveSchema.gen[DerivedTransaction]
  implicit val transactionDetailsSchema = DeriveSchema.gen[FinancialsTransactionDetails]
  implicit val transactionDetails_Schema = DeriveSchema.gen[FinancialsTransactionDetails_]
  implicit val userSchema = DeriveSchema.gen[User]
  implicit val userSchema_ = DeriveSchema.gen[User_]
  implicit val  vatSchema = DeriveSchema.gen[Vat]
}
