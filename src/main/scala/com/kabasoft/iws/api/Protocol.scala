package com.kabasoft.iws.api

import com.kabasoft.iws.domain
import com.kabasoft.iws.domain.{Account, Bank, BankAccount, BankStatement, Company, Costcenter, Customer,
  DerivedTransaction, FinancialsTransaction, FinancialsTransactionDetails,
   Journal,  LoginRequest, PeriodicAccountBalance, Role, Supplier,  User, Vat}
import zio._
import zio.json._

object Protocol {

  final case class Customers(customers: Chunk[Customer])

  implicit val bankAccountEncoder: JsonEncoder[BankAccount] = DeriveJsonEncoder.gen[BankAccount]
  implicit val bankAccountDecoder: JsonDecoder[BankAccount] = DeriveJsonDecoder.gen[BankAccount]

  implicit val customerEncoder: JsonEncoder[Customer] = DeriveJsonEncoder.gen[Customer]
  implicit val customerDecoder: JsonDecoder[Customer] = DeriveJsonDecoder.gen[Customer]

  implicit val customersEncoder: JsonEncoder[Customers] =
    DeriveJsonEncoder.gen[Customers]


  implicit val moduleDecoder: JsonDecoder[domain.Module] = DeriveJsonDecoder.gen[domain.Module]
  implicit val moduleEncoder: JsonEncoder[domain.Module] = DeriveJsonEncoder.gen[domain.Module]

  implicit val supplierEncoder: JsonEncoder[Supplier] = DeriveJsonEncoder.gen[Supplier]
  implicit val supplierDecoder: JsonDecoder[Supplier] = DeriveJsonDecoder.gen[Supplier]
  implicit val suppliersDecoder: JsonDecoder[List[Supplier]] = DeriveJsonDecoder.gen[List[Supplier]]

  implicit lazy val accountEncoder: JsonEncoder[Account] = DeriveJsonEncoder.gen[Account]
  implicit lazy val accountDecoder: JsonDecoder[Account] = DeriveJsonDecoder.gen[Account]

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

  implicit val bankStatementEncoder: JsonEncoder[BankStatement] = DeriveJsonEncoder.gen[BankStatement]
  implicit val bankStatementDecoder: JsonDecoder[BankStatement] = DeriveJsonDecoder.gen[BankStatement]

  implicit val pacEncoder: JsonEncoder[PeriodicAccountBalance] = DeriveJsonEncoder.gen[PeriodicAccountBalance]
  implicit val pacDecoder: JsonDecoder[PeriodicAccountBalance] = DeriveJsonDecoder.gen[PeriodicAccountBalance]

  implicit val companyEncoder: JsonEncoder[Company] = DeriveJsonEncoder.gen[Company]
  implicit val companyDecoder: JsonDecoder[Company] = DeriveJsonDecoder.gen[Company]
  implicit val costcenterEncoder: JsonEncoder[Costcenter] = DeriveJsonEncoder.gen[Costcenter]
  implicit val costcenterDecoder: JsonDecoder[Costcenter] = DeriveJsonDecoder.gen[Costcenter]
  implicit val costcentersDecoder: JsonDecoder[List[Costcenter]] = DeriveJsonDecoder.gen[List[Costcenter]]

  implicit val financialsDerivedEncoder: JsonEncoder[DerivedTransaction] = DeriveJsonEncoder.gen[DerivedTransaction]
  implicit val financialsDerivedDecoder: JsonDecoder[DerivedTransaction] = DeriveJsonDecoder.gen[DerivedTransaction]

  implicit val financialsDetailsEncoder: JsonEncoder[FinancialsTransactionDetails] = DeriveJsonEncoder.gen[FinancialsTransactionDetails]
  implicit val financialsDetailsDecoder: JsonDecoder[FinancialsTransactionDetails] = DeriveJsonDecoder.gen[FinancialsTransactionDetails]

  implicit val financialsEncoder: JsonEncoder[FinancialsTransaction] = DeriveJsonEncoder.gen[FinancialsTransaction]
  implicit val financialsDecoder: JsonDecoder[FinancialsTransaction] = DeriveJsonDecoder.gen[FinancialsTransaction]

  implicit val journalEncoder: JsonEncoder[Journal] = DeriveJsonEncoder.gen[Journal]
  implicit val journalDecoder: JsonDecoder[Journal] = DeriveJsonDecoder.gen[Journal]

}
