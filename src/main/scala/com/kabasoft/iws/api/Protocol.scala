package com.kabasoft.iws.api

import com.kabasoft.iws.domain
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Account, Article, Asset, BankAccount, BankStatement, Bom, Company,
  Customer, Employee, EmployeeSalaryItemDTO, FinancialsTransaction, FinancialsTransactionDetails, Fmodule, ImportFile,
  Journal, LoginRequest, Masterfile, PayrollTaxRange, PeriodicAccountBalance, Permission, Role, SalaryItem,
  Supplier, TransactionDetails, User, UserRight, Vat}
import com.kabasoft.iws.domain.{Article, Bom, FinancialsTransaction, FinancialsTransactionDetails, Journal, Stock, Store, 
  Transaction, TransactionDetails}
import zio.json._

object Protocol:
  given bankAccountCodec: JsonCodec[BankAccount] = DeriveJsonCodec.gen[BankAccount]
  given masterfileCodec: JsonCodec[Masterfile] = DeriveJsonCodec.gen[Masterfile]
  given assetCodec: JsonCodec[Asset] = DeriveJsonCodec.gen[Asset]
  given customerCodec: JsonCodec[Customer] = DeriveJsonCodec.gen[Customer]
  given salaryItemCodec: JsonCodec[SalaryItem] = DeriveJsonCodec.gen[SalaryItem]
  given employeeSalaryItemDTOCodec: JsonCodec[EmployeeSalaryItemDTO] = DeriveJsonCodec.gen[EmployeeSalaryItemDTO]
  given employeeCodec: JsonCodec[Employee] = DeriveJsonCodec.gen[Employee]
  given moduleCodec: JsonCodec[domain.Module] = DeriveJsonCodec.gen[domain.Module]
  given supplierCodec: JsonCodec[Supplier] = DeriveJsonCodec.gen[Supplier]
  given suppliersDecoder: JsonDecoder[List[Supplier]] = DeriveJsonDecoder.gen[List[Supplier]]
  given roleCodec: JsonCodec[Role] = DeriveJsonCodec.gen[Role]
  given accountCodec: JsonCodec[Account] = DeriveJsonCodec.gen[Account]
  given PayrollTaxRangeCodec: JsonCodec[PayrollTaxRange] = DeriveJsonCodec.gen[PayrollTaxRange]
  //given user_Codec: JsonCodec[User_] = DeriveJsonCodec.gen[User_]
  given userCodec: JsonCodec[User] = DeriveJsonCodec.gen[User]
  given userRightCodec: JsonCodec[UserRight] = DeriveJsonCodec.gen[UserRight]
  given importFileCodec: JsonCodec[ImportFile] = DeriveJsonCodec.gen[ImportFile]
  given fmoduleCodec: JsonCodec[Fmodule] = DeriveJsonCodec.gen[Fmodule]
  given loginRequestCodec: JsonCodec[LoginRequest] = DeriveJsonCodec.gen[LoginRequest]
  given vatCodec: JsonCodec[Vat] = DeriveJsonCodec.gen[Vat]
  given bankStatementCodec: JsonCodec[BankStatement] = DeriveJsonCodec.gen[BankStatement]
  given pacCodec: JsonCodec[PeriodicAccountBalance] = DeriveJsonCodec.gen[PeriodicAccountBalance]
  given permissionCodec: JsonCodec[Permission] = DeriveJsonCodec.gen[Permission]
  given companyCodec: JsonCodec[Company] = DeriveJsonCodec.gen[Company]
  given transactionDetailsCodec: JsonCodec[TransactionDetails] = DeriveJsonCodec.gen[TransactionDetails]
  given transactionCodec: JsonCodec[Transaction] = DeriveJsonCodec.gen[Transaction]
  given stockCodec: JsonCodec[Stock] = DeriveJsonCodec.gen[Stock]
  given storeCodec: JsonCodec[Store] = DeriveJsonCodec.gen[Store]
  given financialsDetailsCodec: JsonCodec[FinancialsTransactionDetails] = DeriveJsonCodec.gen[FinancialsTransactionDetails]
  given financialsCodec: JsonCodec[FinancialsTransaction] = DeriveJsonCodec.gen[FinancialsTransaction]
  given journalCodec: JsonCodec[Journal] = DeriveJsonCodec.gen[Journal]
  given repoErrorCodec: JsonCodec[RepositoryError] = DeriveJsonCodec.gen[RepositoryError]
  given articleCodec: JsonCodec[Article] = DeriveJsonCodec.gen[Article]
  given bomCodec: JsonCodec[Bom] = DeriveJsonCodec.gen[Bom]