package com.kabasoft.iws.api

import com.kabasoft.iws.domain
import com.kabasoft.iws.domain.{BusinessPartner, Customer, Employee, Supplier}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.{CustomerRepository, EmployeeRepository, SupplierRepository}
import zio._

enum PartnerKind(
                  val modelid:   Int,
                  val urlPrefix: String,
                  val title:     String,
                  val hasSalary: Boolean
                ):
  case Customer extends PartnerKind(3,  "customer", "Customer", false)
  case Supplier extends PartnerKind(1,  "supplier", "Supplier", false)
  case Employee extends PartnerKind(33, "employee", "Employee", true)

object PartnerKind:
  val all: List[PartnerKind] = List(Customer, Supplier, Employee)

  def fromPrefix(s: String): Option[PartnerKind] = all.find(_.urlPrefix == s)
  def fromModelId(m: Int): Option[PartnerKind]    = all.find(_.modelid == m)

  type Env = CustomerRepository & SupplierRepository & EmployeeRepository

  def blank(kind: PartnerKind, company: String): BusinessPartner = kind match
    case Customer =>
      domain.Customer(
        id = "", name = "", description = "", street = "", zip = "", city = "",
        state = "", country = "", phone = "", email = "",
        account = "-1", oaccount = "-1", taxCode = "-1", vatCode = "-1",
        currency = "EUR", contact = "", company = company)
    case Supplier =>
      domain.Supplier(
        id = "", name = "", description = "", street = "", zip = "", city = "",
        state = "", country = "", phone = "", email = "",
        account = "-1", oaccount = "-1", taxCode = "-1", vatCode = "-1",
        currency = "EUR", contact = "", company = company)
    case Employee =>
      domain.Employee(
        id = "", name = "", description = "", street = "", zip = "", city = "",
        state = "", country = "", phone = "", email = "",
        account = "-1", oaccount = "-1", taxCode = "-1", vatCode = "-1",
        currency = "EUR", contact = "", company = company,
        salary = java.math.BigDecimal("0.00"))

  def create(kind: PartnerKind, p: BusinessPartner): ZIO[Env, RepositoryError, Int] =
    p match
      case c: Customer => CustomerRepository.create(c)
      case s: Supplier => SupplierRepository.create(s)
      case e: Employee => EmployeeRepository.create(e)
      case _ => ZIO.succeed(0)
      
  def loadAll(kind: PartnerKind, company: String)
  : ZIO[Env, RepositoryError, List[BusinessPartner]] =
    kind match
      case Customer =>
        CustomerRepository.all((kind.modelid, company)).map(_.map(c => (c: BusinessPartner)))
      case Supplier =>
        SupplierRepository.all((kind.modelid, company)).map(_.map(s => (s: BusinessPartner)))
      case Employee =>
        EmployeeRepository.all((kind.modelid, company)).map(_.map(e => (e: BusinessPartner)))

  def load(kind: PartnerKind, id: String, modelid: Int, company: String)
  : ZIO[Env, RepositoryError, BusinessPartner] =
    kind match
      case Customer => CustomerRepository.getById((id, modelid, company)).map(c => (c: BusinessPartner))
      case Supplier => SupplierRepository.getById((id, modelid, company)).map(s => (s: BusinessPartner))
      case Employee => EmployeeRepository.getById((id, modelid, company)).map(e => (e: BusinessPartner))

  def save(p: BusinessPartner): ZIO[Env, RepositoryError, Int] =
    p match
      case c: Customer => CustomerRepository.modify(c)
      case s: Supplier => SupplierRepository.modify(s)
      case e: Employee => EmployeeRepository.modify(e)
      case _           => ZIO.succeed(0)

  /** Rebuild the concrete case class from the form params, keeping all fields not in the form. */
  def applyUpdate(p: BusinessPartner, kind: PartnerKind, params: Map[String, String]): BusinessPartner =
    val name        = params.getOrElse("name",        p.name)
    val description = params.getOrElse("description", p.description)
    val street      = params.getOrElse("street",      p.street)
    val zip         = params.getOrElse("zip",         p.zip)
    val city        = params.getOrElse("city",        p.city)
    val state       = params.getOrElse("state",       p.state)
    val country     = params.getOrElse("country",     p.country)
    val phone       = params.getOrElse("phone",       p.phone)
    val email       = params.getOrElse("email",       p.email)
    val account     = params.getOrElse("account",     p.account)
    val oaccount    = params.getOrElse("oaccount",    p.oaccount)
    val taxCode     = params.getOrElse("taxCode",     p.taxCode)
    val vatCode     = params.getOrElse("vatCode",     p.vatCode)
    val currency    = params.getOrElse("currency",    p.currency)
    val contact     = params.getOrElse("contact",     p.contact)

    (kind, p) match
      case (Customer, c: Customer) =>
        c.copy(name = name, description = description, street = street, zip = zip,
          city = city, state = state, country = country, phone = phone,
          email = email, account = account, oaccount = oaccount,
          taxCode = taxCode, vatCode = vatCode, currency = currency, contact = contact)
      case (Supplier, s: Supplier) =>
        s.copy(name = name, description = description, street = street, zip = zip,
          city = city, state = state, country = country, phone = phone,
          email = email, account = account, oaccount = oaccount,
          taxCode = taxCode, vatCode = vatCode, currency = currency, contact = contact)
      case (Employee, e: Employee) =>
        val salary = java.math.BigDecimal(params.getOrElse("salary", e.salary.toString))
        e.copy(name = name, description = description, street = street, zip = zip,
          city = city, state = state, country = country, phone = phone,
          email = email, account = account, oaccount = oaccount,
          taxCode = taxCode, vatCode = vatCode, currency = currency, contact = contact,
          salary = salary)
      case _ => p