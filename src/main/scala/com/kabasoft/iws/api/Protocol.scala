package com.kabasoft.iws.api

import com.kabasoft.iws.domain
import com.kabasoft.iws.domain.AppError. RepositoryError
import com.kabasoft.iws.domain.{Account, Bank, BankAccount, BankStatement, Company, Costcenter, Customer,
  DerivedTransaction, FinancialsTransaction, FinancialsTransactionDetails, Journal, LoginRequest,
  PeriodicAccountBalance, Role, Supplier, User, Vat}
import zio.json._

object Protocol {

  implicit val bankCodec: JsonCodec[Bank] = DeriveJsonCodec.gen[Bank]
  implicit val bankAccountCodec: JsonCodec[BankAccount] = DeriveJsonCodec.gen[BankAccount]
  implicit val customerCodec: JsonCodec[Customer] = DeriveJsonCodec.gen[Customer]

  implicit val moduleCodec: JsonCodec[domain.Module] = DeriveJsonCodec.gen[domain.Module]
  implicit val supplierCodec: JsonCodec[Supplier] = DeriveJsonCodec.gen[Supplier]
  implicit val suppliersDecoder: JsonDecoder[List[Supplier]] = DeriveJsonDecoder.gen[List[Supplier]]

  implicit lazy val accountCodec: JsonCodec[Account] = DeriveJsonCodec.gen[Account]
  implicit val userCodec: JsonCodec[User] = DeriveJsonCodec.gen[User]
  implicit val roleCodec: JsonCodec[Role] = DeriveJsonCodec.gen[Role]
  //implicit val loginRequestCodec: JsonCodec[LoginRequest] = DeriveJsonCodec.gen[LoginRequest]
  implicit val loginRequestEncoder: JsonEncoder[LoginRequest] = DeriveJsonEncoder.gen[LoginRequest]
  implicit val loginRequestDecoder: JsonDecoder[LoginRequest] = DeriveJsonDecoder.gen[LoginRequest]
  implicit val vatCodec: JsonCodec[Vat] = DeriveJsonCodec.gen[Vat]
  implicit val bankStatementCodec: JsonCodec[BankStatement] = DeriveJsonCodec.gen[BankStatement]
  implicit val pacCodec: JsonCodec[PeriodicAccountBalance] = DeriveJsonCodec.gen[PeriodicAccountBalance]

  implicit val companyCodec: JsonCodec[Company] = DeriveJsonCodec.gen[Company]

  implicit val costcenterCodec: JsonCodec[Costcenter] = DeriveJsonCodec.gen[Costcenter]
  implicit val financialsDerivedCodec: JsonCodec[DerivedTransaction] = DeriveJsonCodec.gen[DerivedTransaction]
  implicit val financialsDetailsCodec: JsonCodec[FinancialsTransactionDetails] = DeriveJsonCodec.gen[FinancialsTransactionDetails]
  implicit val financialsCodec: JsonCodec[FinancialsTransaction] = DeriveJsonCodec.gen[FinancialsTransaction]

  implicit val journalCodec: JsonCodec[Journal] = DeriveJsonCodec.gen[Journal]
  implicit val repoErrorCodec: JsonCodec[RepositoryError] = DeriveJsonCodec.gen[RepositoryError]


}
