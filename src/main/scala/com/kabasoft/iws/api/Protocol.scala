package com.kabasoft.iws.api

import com.kabasoft.iws.domain
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Account, Article, Asset, Bank, BankAccount, BankStatement, Bom, Company, Costcenter, Customer, Employee, FinancialsTransaction, FinancialsTransactionDetails, Fmodule, ImportFile, Journal, LoginRequest, PeriodicAccountBalance, Permission, Role, SalaryItem, Stock, Store, Supplier, TransactionDetails, User, UserRight, User_, Vat}
import zio.json._

object Protocol {

  implicit val bankCodec: JsonCodec[Bank] = DeriveJsonCodec.gen[Bank]
  implicit val bankAccountCodec: JsonCodec[BankAccount] = DeriveJsonCodec.gen[BankAccount]
  implicit val assetCodec: JsonCodec[Asset] = DeriveJsonCodec.gen[Asset]
  implicit val customerCodec: JsonCodec[Customer] = DeriveJsonCodec.gen[Customer]
  implicit  val salaryItemCodec: JsonCodec[SalaryItem] = DeriveJsonCodec.gen[SalaryItem]
  implicit lazy val employeeCodec: JsonCodec[Employee] = DeriveJsonCodec.gen[Employee]
  implicit val moduleCodec: JsonCodec[domain.Module] = DeriveJsonCodec.gen[domain.Module]
  implicit val supplierCodec: JsonCodec[Supplier] = DeriveJsonCodec.gen[Supplier]
  implicit val suppliersDecoder: JsonDecoder[List[Supplier]] = DeriveJsonDecoder.gen[List[Supplier]]
  //implicit val userRightCodec: JsonCodec[UserRight] = DeriveJsonCodec.gen[UserRight]
  implicit val roleCodec: JsonCodec[Role] = DeriveJsonCodec.gen[Role]
  implicit lazy val accountCodec: JsonCodec[Account] = DeriveJsonCodec.gen[Account]

  implicit lazy val user_Codec: JsonCodec[User_] = DeriveJsonCodec.gen[User_]
  implicit lazy val userCodec: JsonCodec[User] = DeriveJsonCodec.gen[User]
  implicit lazy val userRightCodec: JsonCodec[UserRight] = DeriveJsonCodec.gen[UserRight]
  implicit lazy val importFileCodec: JsonCodec[ImportFile] = DeriveJsonCodec.gen[ImportFile]
  implicit val fmoduleCodec: JsonCodec[Fmodule] = DeriveJsonCodec.gen[Fmodule]
  implicit val loginRequestEncoder: JsonEncoder[LoginRequest] = DeriveJsonEncoder.gen[LoginRequest]
  implicit val loginRequestDecoder: JsonDecoder[LoginRequest] = DeriveJsonDecoder.gen[LoginRequest]
  implicit val vatCodec: JsonCodec[Vat] = DeriveJsonCodec.gen[Vat]
  implicit val bankStatementCodec: JsonCodec[BankStatement] = DeriveJsonCodec.gen[BankStatement]
  implicit val pacCodec: JsonCodec[PeriodicAccountBalance] = DeriveJsonCodec.gen[PeriodicAccountBalance]
  implicit val permissionCodec: JsonCodec[Permission] = DeriveJsonCodec.gen[Permission]
  implicit val companyCodec: JsonCodec[Company] = DeriveJsonCodec.gen[Company]

  implicit val costcenterCodec: JsonCodec[Costcenter] = DeriveJsonCodec.gen[Costcenter]
  implicit val transactionDetailsCodec: JsonCodec[TransactionDetails] = DeriveJsonCodec.gen[TransactionDetails]
  implicit val financialsDetailsCodec: JsonCodec[FinancialsTransactionDetails] = DeriveJsonCodec.gen[FinancialsTransactionDetails]
  implicit val financialsCodec: JsonCodec[FinancialsTransaction] = DeriveJsonCodec.gen[FinancialsTransaction]
  implicit val journalCodec: JsonCodec[Journal] = DeriveJsonCodec.gen[Journal]
  implicit val repoErrorCodec: JsonCodec[RepositoryError] = DeriveJsonCodec.gen[RepositoryError]
  implicit lazy val articleCodec: JsonCodec[Article] = DeriveJsonCodec.gen[Article]
  implicit lazy val bomCodec: JsonCodec[Bom] = DeriveJsonCodec.gen[Bom]
  implicit val stockCodec: JsonCodec[Stock] = DeriveJsonCodec.gen[Stock]
  implicit val storeCodec: JsonCodec[Store] = DeriveJsonCodec.gen[Store]

}
