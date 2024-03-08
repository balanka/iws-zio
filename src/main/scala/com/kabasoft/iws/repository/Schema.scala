package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio.schema.DeriveSchema

object Schema {
  implicit val accountSchema             = DeriveSchema.gen[Account]
  implicit lazy val account_schema       = DeriveSchema.gen[Account_]
  implicit val assetSchema               = DeriveSchema.gen[Asset]
  implicit val bankSchema                = DeriveSchema.gen[Bank]
  implicit val masterfileSchema          = DeriveSchema.gen[Masterfile]
  implicit val bankStatementsSchema      = DeriveSchema.gen[BankStatement]
  implicit val bankStatementsSchema_     = DeriveSchema.gen[BankStatement_]
  implicit val companySchema             = DeriveSchema.gen[Company]
  implicit val company_Schema            = DeriveSchema.gen[Company_]
  implicit val costcenterSchema          = DeriveSchema.gen[Costcenter]
  implicit val importFileSchema          = DeriveSchema.gen[ImportFile]
  implicit val customerSchema            = DeriveSchema.gen[Customer]
  implicit val customer_Schema           = DeriveSchema.gen[Customer_]
  implicit val employeeSchema            = DeriveSchema.gen[Employee]
  implicit val employee_Schema           = DeriveSchema.gen[Employee_]
  implicit val bankAccountSchema         = DeriveSchema.gen[BankAccount]
  implicit val journalSchema             = DeriveSchema.gen[Journal]
  implicit val journal_Schema            = DeriveSchema.gen[Journal_]
  implicit val moduleSchema              = DeriveSchema.gen[Module]
  implicit val permissionSchema          = DeriveSchema.gen[Permission]
  implicit val role_Schema               = DeriveSchema.gen[Role_]
  implicit val roleSchema                = DeriveSchema.gen[Role]
  implicit val userRoleSchema            = DeriveSchema.gen[UserRole]
  implicit val userRightSchema           = DeriveSchema.gen[UserRight]
  implicit val fmoduleSchema             = DeriveSchema.gen[Fmodule]
  implicit val pacSchema                 = DeriveSchema.gen[PeriodicAccountBalance]
  implicit val supplierschema            = DeriveSchema.gen[Supplier]
  implicit val supplier_Schema           = DeriveSchema.gen[Supplier_]
  implicit val transactionSchema         = DeriveSchema.gen[Transaction]
  implicit val transactionSchemax        = DeriveSchema.gen[Transactionx]
  implicit val transactionSchema_        = DeriveSchema.gen[Transaction_]
  implicit val ftransactionSchema         = DeriveSchema.gen[FinancialsTransaction]
  implicit val ftransactionxSchema        = DeriveSchema.gen[FinancialsTransactionx]
  implicit val ftransactionSchema_        = DeriveSchema.gen[FinancialsTransaction_]
  implicit val transactionDetailsSchema  = DeriveSchema.gen[TransactionDetails]
  implicit val transactionDetailsSchema_  = DeriveSchema.gen[TransactionDetails_]
  implicit val ftransactionDetailsSchema  = DeriveSchema.gen[FinancialsTransactionDetails]
  implicit val ftransactionDetails_Schema = DeriveSchema.gen[FinancialsTransactionDetails_]
  implicit val repositoryErrorSchema     = DeriveSchema.gen[RepositoryError]
  implicit val articleSchema             = DeriveSchema.gen[Article]
  implicit val article_Schema            = DeriveSchema.gen[Article_]
  implicit val bomSchema                 = DeriveSchema.gen[Bom]
  implicit val salaryItemSchema          = DeriveSchema.gen[SalaryItem]
  implicit val employeeSalaryItemSchema  = DeriveSchema.gen[EmployeeSalaryItem]
  implicit val employeeSalaryItemDTOSchema  = DeriveSchema.gen[EmployeeSalaryItemDTO]
  implicit val storeSchema               = DeriveSchema.gen[Store]
  implicit val stockSchema               = DeriveSchema.gen[Stock]
  implicit val userSchema                = DeriveSchema.gen[User]
  implicit val userSchema_               = DeriveSchema.gen[User_]
  implicit val userSchemax               = DeriveSchema.gen[Userx]
  implicit val vatSchema                 = DeriveSchema.gen[Vat]
  implicit val payrollTaxRangeSchema     = DeriveSchema.gen[PayrollTaxRange]
  implicit val loginRequestSchema        = DeriveSchema.gen[LoginRequest]

}
