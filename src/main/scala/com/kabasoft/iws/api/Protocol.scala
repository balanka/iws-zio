package com.kabasoft.iws.api

import com.kabasoft.iws.domain
import com.kabasoft.iws.domain.{Account, Account_, Bank, BankAccount, BankStatement, BankStatement_, Company, Costcenter, Customer, CustomerWithOrderDate, CustomerWithOrderNumber, Customer_, DerivedTransaction, FinancialsTransaction, FinancialsTransactionDetails, FinancialsTransactionDetails_, FinancialsTransaction_, FinancialsTransactionx, Journal, Journal_, LoginRequest, Order, OrderDetail, PeriodicAccountBalance, Role, Supplier, Supplier_, User, User_, Vat}
import zio._
import zio.json._
import zio.schema.DeriveSchema

object Protocol {
  implicit  val account_schema = DeriveSchema.gen[Account_]
  implicit  val bankschema = DeriveSchema.gen[Bank]
  implicit  val bankStatement_schema = DeriveSchema.gen[BankStatement_]
  implicit  val bankStatementschema = DeriveSchema.gen[BankStatement]
  implicit val costCenterSchema = DeriveSchema.gen[Costcenter]
  implicit val customer_Schema = DeriveSchema.gen[Customer_]
  implicit val customerSchema = DeriveSchema.gen[Customer]
  implicit val journalSchema = DeriveSchema.gen[Journal]
  implicit val journal_Schema = DeriveSchema.gen[Journal_]
  implicit val moduleSchema = DeriveSchema.gen[domain.Module]
  implicit val pacSchema = DeriveSchema.gen[PeriodicAccountBalance]
  implicit val supplierSchema = DeriveSchema.gen[Supplier_]
  implicit val bankAccountSchema = DeriveSchema.gen[BankAccount]
  implicit val transactionSchema = DeriveSchema.gen[FinancialsTransactionx]
  implicit val transactionSchema_ = DeriveSchema.gen[FinancialsTransaction_]
  implicit val derivedTransactionSchema = DeriveSchema.gen[DerivedTransaction]
  implicit val transactionDetailsSchema = DeriveSchema.gen[FinancialsTransactionDetails]
  implicit val transactionDetails_Schema = DeriveSchema.gen[FinancialsTransactionDetails_]
  implicit val userSchema = DeriveSchema.gen[User]
  implicit val userSchema_ = DeriveSchema.gen[User_]
  implicit val  vatSchema = DeriveSchema.gen[Vat]

  final case class Orders(orders: Chunk[Order])

  final case class Customers(customers: Chunk[Customer])

  final case class CustomerWrapper(customers: List[CustomerWithOrderDate])

  final case class CustomerCountWrapper(
      customers: List[CustomerWithOrderNumber]
  )

  implicit val bankAccountEncoder: JsonEncoder[BankAccount] = DeriveJsonEncoder.gen[BankAccount]
  implicit val bankAccountDecoder: JsonDecoder[BankAccount] = DeriveJsonDecoder.gen[BankAccount]

  implicit val customerEncoder: JsonEncoder[Customer] = DeriveJsonEncoder.gen[Customer]
  implicit val customerDecoder: JsonDecoder[Customer] = DeriveJsonDecoder.gen[Customer]

  implicit val orderEncoder: JsonEncoder[Order] = DeriveJsonEncoder.gen[Order]
  implicit val orderDecoder: JsonDecoder[Order] = DeriveJsonDecoder.gen[Order]
  
  implicit val ordersEncoder: JsonEncoder[Orders] =
    DeriveJsonEncoder.gen[Orders]
    
  implicit val orderDetailEncoder: JsonEncoder[OrderDetail] =
      DeriveJsonEncoder.gen[OrderDetail]

  implicit val customerWithOrderDateEncoder: JsonEncoder[CustomerWithOrderDate] =
    DeriveJsonEncoder.gen[CustomerWithOrderDate]

  implicit val customerWrapperEncoder: JsonEncoder[CustomerWrapper] =
    DeriveJsonEncoder.gen[CustomerWrapper]

  implicit val customerWithOrderNumberEncoder: JsonEncoder[CustomerWithOrderNumber] =
    DeriveJsonEncoder.gen[CustomerWithOrderNumber]

  implicit val customerCountWrapperEncoder: JsonEncoder[CustomerCountWrapper] =
    DeriveJsonEncoder.gen[CustomerCountWrapper]

  implicit val customersEncoder: JsonEncoder[Customers] =
    DeriveJsonEncoder.gen[Customers]


  implicit val moduleDecoder: JsonDecoder[domain.Module] = DeriveJsonDecoder.gen[domain.Module]
  implicit val moduleEncoder: JsonEncoder[domain.Module] = DeriveJsonEncoder.gen[domain.Module]


  //implicit val bankAccountsEncoder: JsonEncoder[BankAccounts] = DeriveJsonEncoder.gen[BankAccounts]
  //implicit val bankAccountsDecoder: JsonDecoder[BankAccounts] = DeriveJsonDecoder.gen[BankAccounts]
  implicit val supplierEncoder: JsonEncoder[Supplier] = DeriveJsonEncoder.gen[Supplier]
  implicit val supplierDecoder: JsonDecoder[Supplier] = DeriveJsonDecoder.gen[Supplier]

  implicit lazy val accountEncoder: JsonEncoder[Account] = DeriveJsonEncoder.gen[Account]
  implicit lazy val accountDecoder: JsonDecoder[Account] = DeriveJsonDecoder.gen[Account]
  //implicit lazy val accountsEncoder: JsonEncoder[Accounts] = DeriveJsonEncoder.gen[Accounts]
  //implicit lazy val accountsDecoder: JsonDecoder[Accounts] = DeriveJsonDecoder.gen[Accounts]

  implicit val userEncoder: JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  implicit val userDecoder: JsonDecoder[User] = DeriveJsonDecoder.gen[User]
  implicit val roleEncoder: JsonEncoder[Role] = DeriveJsonEncoder.gen[Role]
  implicit val roleDecoder: JsonDecoder[Role] = DeriveJsonDecoder.gen[Role]

  implicit val loginRequestEncoder: JsonEncoder[LoginRequest] = DeriveJsonEncoder.gen[LoginRequest]
  implicit val loginRequestDecoder: JsonDecoder[LoginRequest] = DeriveJsonDecoder.gen[LoginRequest]

  implicit val vatEncoder: JsonEncoder[Vat] = DeriveJsonEncoder.gen[Vat]
  implicit val vatDecoder: JsonDecoder[Vat] = DeriveJsonDecoder.gen[Vat]

  implicit val bankEncoder: JsonEncoder[Bank] = DeriveJsonEncoder.gen[Bank]
  implicit val bankDecoder: JsonDecoder[Bank] = DeriveJsonDecoder.gen[Bank]

  //implicit val banksEncoder: JsonEncoder[Banks] = DeriveJsonEncoder.gen[Banks]
  //implicit val banksDecoder: JsonDecoder[Banks] = DeriveJsonDecoder.gen[Banks]

  implicit val bankStatementEncoder: JsonEncoder[BankStatement] = DeriveJsonEncoder.gen[BankStatement]
  implicit val bankStatementDecoder: JsonDecoder[BankStatement] = DeriveJsonDecoder.gen[BankStatement]

  implicit val pacEncoder: JsonEncoder[PeriodicAccountBalance] = DeriveJsonEncoder.gen[PeriodicAccountBalance]
  implicit val pacDecoder: JsonDecoder[PeriodicAccountBalance] = DeriveJsonDecoder.gen[PeriodicAccountBalance]

  implicit val companyEncoder: JsonEncoder[Company] = DeriveJsonEncoder.gen[Company]
  implicit val companyDecoder: JsonDecoder[Company] = DeriveJsonDecoder.gen[Company]
  implicit val costcenterEncoder: JsonEncoder[Costcenter] = DeriveJsonEncoder.gen[Costcenter]
  implicit val costcenterDecoder: JsonDecoder[Costcenter] = DeriveJsonDecoder.gen[Costcenter]
  implicit val costcentersDecoder: JsonDecoder[List[Costcenter]] = DeriveJsonDecoder.gen[List[Costcenter]]
 // implicit val customerEncoder: JsonEncoder[Customer] = DeriveJsonEncoder.gen[Customer]
 // implicit val customerDecoder: JsonDecoder[Customer] = DeriveJsonDecoder.gen[Customer]

  implicit val financialsDerivedEncoder: JsonEncoder[DerivedTransaction] = DeriveJsonEncoder.gen[DerivedTransaction]
  implicit val financialsDerivedDecoder: JsonDecoder[DerivedTransaction] = DeriveJsonDecoder.gen[DerivedTransaction]

  implicit val financialsDetailsEncoder: JsonEncoder[FinancialsTransactionDetails] = DeriveJsonEncoder.gen[FinancialsTransactionDetails]
  implicit val financialsDetailsDecoder: JsonDecoder[FinancialsTransactionDetails] = DeriveJsonDecoder.gen[FinancialsTransactionDetails]

  implicit val financialsEncoder: JsonEncoder[FinancialsTransaction] = DeriveJsonEncoder.gen[FinancialsTransaction]
  implicit val financialsDecoder: JsonDecoder[FinancialsTransaction] = DeriveJsonDecoder.gen[FinancialsTransaction]

  implicit val journalEncoder: JsonEncoder[Journal] = DeriveJsonEncoder.gen[Journal]
  implicit val journalDecoder: JsonDecoder[Journal] = DeriveJsonDecoder.gen[Journal]

  //implicit val bankStatementsEncoder: JsonEncoder[BankStatements] = DeriveJsonEncoder.gen[BankStatements]

}
