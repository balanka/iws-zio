package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository.{AccountRepository, CompanyRepository, EmployeeRepository,
        FinancialsTransactionRepository}
import zio._

import java.time.Instant
import java.math.RoundingMode

final class EmployeeServiceLive(empRepo: EmployeeRepository
                                , accountRepo: AccountRepository
                                , companyRepo: CompanyRepository
                                //, ptrRepo: PayrollTaxRangeRepository
                                , ftrRepo: FinancialsTransactionRepository) 
                   extends EmployeeService:

  override def generate(period: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    _<- ZIO.logDebug(s" Posting transaction for the company ${company}")
    transactions <- build(period, company, ModelId.PAYROLL.modelid).debug("transactions")
    nr          <- ftrRepo.create(transactions) //ZIO.succeed(transactions).map(_.size)
  }yield nr

  private def build(period: Int, companyId: String, modelid:Int): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    company <- companyRepo.getById((companyId, ModelId.COMPANY.modelid)).debug("company")
    employee <- empRepo.all((ModelId.EMPLOYEE.modelid, companyId)).debug("employee")
    accounts<- accountRepo.all((ModelId.ACCOUNT.modelid, companyId))//.debug("accounts")
    //ptr <- ptrRepo.all((PayrollTaxRange.MODELID, companyId))//.debug("payroll gross salary to tax map ")
  }yield employee.map(emp => buildTransaction(period, emp,
          buildTransactionDetails (emp, emp.salaryItems.map(EmployeeSalaryItem.apply), accounts, company, modelid) ))

  private def buildTransactionDetails(emp:Employee, salaryItems: List[EmployeeSalaryItem], accounts:List[Account]
                                            , company: Company, modelid:Int) = {
    salaryItems.map(item => FinancialsTransactionDetails(-1L, -1L, emp.oaccount, side = true, item.account
      , emp.salary.multiply(item.percentage).setScale(6, RoundingMode.HALF_UP)
      , Instant.now(), item.text, company.currency, company.id, getName(accounts, emp.oaccount), getName(accounts, item.account), modelid))
  }
  def getName (accounts:List[Account], id:String): String =
    accounts.find(_.id == id).fold(s"Account with id ${id} not found!!!")(_.name)

  private def buildTransaction(period: Int, emp:Employee,  lines: List[FinancialsTransactionDetails]) = {
    val date = Instant.now()
   // val period = common.getPeriod(date)
    FinancialsTransaction(
      -1L, "", "", "100", emp.account, date, date, date, period, posted = false,
      ModelId.PAYROLL.modelid, emp.company,
      "Salary "+period, "", 0, lines)
  }

object EmployeeServiceLive:
  val live: ZLayer[EmployeeRepository &AccountRepository&FinancialsTransactionRepository&
                  CompanyRepository //& PayrollTaxRangeRepository
                    ,  RepositoryError, EmployeeService] =
       ZLayer.fromFunction(new EmployeeServiceLive(_, _, _, _))


