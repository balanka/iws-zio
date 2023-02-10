package com.kabasoft.iws.api

import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import zio.json.EncoderOps
import com.kabasoft.iws.domain.{Bank, CustomerBuilder}
import com.kabasoft.iws.repository.{ BankRepository, BankRepositoryImpl, CostcenterRepository, CostcenterRepositoryImpl, CustomerRepository, CustomerRepositoryImpl, ModuleRepository, ModuleRepositoryImpl, SupplierRepository, SupplierRepositoryImpl, UserRepository, UserRepositoryImpl, VatRepository, VatRepositoryImpl}
import com.kabasoft.iws.api.CostcenterEndpoint.ccByIdAPI
import zio.http.api.HttpCodec.literal
import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.api.CustomerEndpoint.custByIdAPI
import com.kabasoft.iws.api.ModuleEndpoint.moduleByIdAPI
import com.kabasoft.iws.api.SupplierEndpoint.supByIdAPI
import com.kabasoft.iws.api.UserEndpoint.userByUserNameAPI
import com.kabasoft.iws.api.VatEndpoint.vatByIdAPI
//import com.kabasoft.iws.domain.AccountBuilder.acc
import com.kabasoft.iws.domain.CostcenterBuilder.cc
import com.kabasoft.iws.domain.ModuleBuilder.m
import com.kabasoft.iws.domain.SupplierBuilder.sup
import com.kabasoft.iws.domain.UserBuilder.user
import com.kabasoft.iws.domain.VatBuilder.vat1
import zio._
import zio.http.api.RouteCodec._
import zio.http.api.{EndpointSpec, Endpoints}
import zio.http.{Request, URL}
import zio.schema.DeriveSchema.gen
import zio.sql.ConnectionPool
import zio.test._

import java.time.Instant


object ApiSpec extends ZIOSpecDefault {
  val company = "1000"
  val bankId = "COLSDE33"
  val bankName = "SPARKASSE KOELN-BONN"
  val accId = "9900"
  val accName = "Bilanz"
  val bank = Bank(bankId, bankName, bankName, instantFromStr("2018-01-01T00:00:00.00Z"),
    instantFromStr("2018-01-01T00:00:00.00Z"), instantFromStr("2018-01-01T00:00:00.00Z"),11,company)


  def instantFromStr(str:String)=Instant.parse(str)
    def spec = suite("APISpec")(
      suite("handler")(
/*        test("Account  integration test ") {
          val accountById = EndpointSpec.get(literal("acc") / string("id")).out[Account]
            .implement { id => AccountRepository.getBy(id, "1000") }
        val accAll = EndpointSpec.get(literal("acc") ).out[Int]
                     .implement(_ => AccountRepository.all("1000").map(_.size))
          val testRoutes = testApi(accountById ++ accAll) _
          testRoutes("/acc", "56") && testRoutes("/acc/00000", acc.toJson)
        },*/
        test("Bank integration test") {
          val bankById = EndpointSpec.get(literal("bank") / string("id")).out[Bank]
            .implement { id => BankRepository.getBy(id, "1000") }
          val bankAll = EndpointSpec.get(literal("bank")).out[Int]
            .implement(_ => BankRepository.all("1000").map(_.size))
          val testRoutes = testApi(bankAll ++ bankById) _
          testRoutes("/bank", "56") && testRoutes("/bank/COLSDE33", bank.toJson)
        },
         test("Customer integration test") {
          val custAllEndpoint = EndpointSpec.get[Unit](literal("cust")).out[Int]
            .implement(_ => CustomerRepository.all("1000").map(_.size))
          val custByIdEndpoint = custByIdAPI.implement(id => CustomerRepository.getBy(id, "1000"))
          val testRoutes = testApi(custByIdEndpoint ++ custAllEndpoint) _
          testRoutes("/cust", "19") && testRoutes("/cust/5222", CustomerBuilder.dummy.toJson)
        },
        test("Supplier integration test") {
          val supAllEndpoint = EndpointSpec.get[Unit](literal("sup")).out[Int]
            .implement(_ => SupplierRepository.all("1000").map(_.size))
          val supByIdEndpoint = supByIdAPI.implement(id => SupplierRepository.getBy(id, "1000"))
          val testRoutes = testApi(supByIdEndpoint ++ supAllEndpoint) _
          testRoutes("/sup", "67") && testRoutes("/sup/70000", sup.toJson)
        },
        test("Cost center integration test") {
          val ccAllEndpoint = EndpointSpec.get(literal("cc")).out[Int].implement(_ => CostcenterRepository.all("1000").map(_.size))
          val ccByIdEndpoint = ccByIdAPI.implement(id => CostcenterRepository.getBy(id, "1000"))
          val testRoutes = testApi(ccByIdEndpoint ++ ccAllEndpoint) _
          testRoutes("/cc", "13") && testRoutes("/cc/300", cc.toJson)
        },
          test("Module integration test") {
          val moduleAllEndpoint = EndpointSpec.get(literal("module")).out[Int].implement(_ => ModuleRepository.all("1000").map(_.size))
          val moduleByIdEndpoint = moduleByIdAPI.implement(id => ModuleRepository.getBy(id, "1000"))
          val testRoutes = testApi(moduleByIdEndpoint ++ moduleAllEndpoint) _
          testRoutes("/module", "14") && testRoutes("/module/0000", m.toJson)
        },
           test("User integration test") {
          val userAllEndpoint = EndpointSpec.get(literal("user")).out[Int].implement(_ => UserRepository.all("1000").map(_.size))
          val userByUserNameEndpoint = userByUserNameAPI.implement(userName => UserRepository.getByUserName(userName, "1000"))
          val testRoutes = testApi(userByUserNameEndpoint ++ userAllEndpoint) _
          testRoutes("/user", "4") && testRoutes("/user/jdegoes011", user.toJson)
        },
        test("Vat integration test") {
          val vatAllEndpoint = EndpointSpec.get(literal("vat")).out[Int].implement(_ => VatRepository.all("1000").map(_.size))
          val vatByIdEndpoint = vatByIdAPI.implement(id => VatRepository.getBy(id, "1000"))
          val testRoutes = testApi(vatByIdEndpoint ++ vatAllEndpoint) _
          testRoutes("/vat", "11") && testRoutes("/vat/v101", vat1.toJson)
        }
    ).provide(ConnectionPool.live, connectionPoolConfig, DbConfig.layer, BankRepositoryImpl.live,
        CostcenterRepositoryImpl.live, CustomerRepositoryImpl.live, SupplierRepositoryImpl.live, VatRepositoryImpl.live,
        ModuleRepositoryImpl.live, UserRepositoryImpl.live)
    )
  def testApi[R, E](service: Endpoints[R, E, _])(
    url: String, expected: String): ZIO[R, E, TestResult] = {
    val request = Request.get(url = URL.fromString(url).toOption.get)
    for {
      response <- service.toHttpRoute.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }

}