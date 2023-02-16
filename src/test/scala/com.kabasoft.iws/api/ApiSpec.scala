package com.kabasoft.iws.api

import com.kabasoft.iws.api.BankEndpoint.bankByIdEndpoint
import zio.json.EncoderOps
import com.kabasoft.iws.domain.CustomerBuilder
import com.kabasoft.iws.repository.{BankRepository, BankRepositoryImpl, CostcenterRepository, CostcenterRepositoryImpl, CustomerRepository, CustomerRepositoryImpl, ModuleRepository, ModuleRepositoryImpl, SupplierRepository, SupplierRepositoryImpl, UserRepository, UserRepositoryImpl, VatRepository, VatRepositoryImpl}
import com.kabasoft.iws.api.CostcenterEndpoint.ccByIdEndpoint
import zio.http.api.HttpCodec.literal
import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.BankBuilder.bank
import com.kabasoft.iws.api.CustomerEndpoint.custByIdEndpoint
import com.kabasoft.iws.api.ModuleEndpoint.moduleByIdEndpoint
import com.kabasoft.iws.api.SupplierEndpoint.supByIdEndpoint
import com.kabasoft.iws.api.UserEndpoint.userByUserNameEndpoint
import com.kabasoft.iws.api.VatEndpoint.vatByIdEndpoint
import com.kabasoft.iws.repository.container.PostgresContainer
//import com.kabasoft.iws.domain.AccountBuilder.acc
import com.kabasoft.iws.domain.CostcenterBuilder.cc
import com.kabasoft.iws.domain.ModuleBuilder.m
import com.kabasoft.iws.domain.SupplierBuilder.sup
import com.kabasoft.iws.domain.UserBuilder.user
import com.kabasoft.iws.domain.VatBuilder.vat1
import zio._
import zio.http.api.{EndpointSpec, Endpoints}
import zio.http.{Request, URL}
import zio.schema.DeriveSchema.gen
import zio.sql.ConnectionPool
import zio.test._

object ApiSpec extends ZIOSpecDefault {

    def spec = suite("APISpec")(
      suite("handler")(
        /*test("Account  integration test ") {
          val accountById = EndpointSpec.get(literal("acc") / string("id")).out[Account]
            .implement { id => AccountRepository.getBy(id, "1000") }
        val accAll = EndpointSpec.get(literal("acc") ).out[Int]
                     .implement(_ => AccountRepository.all("1000").map(_.size))
          val testRoutes = testApi(accountById ++ accAll) _
          testRoutes("/acc", "56") && testRoutes("/acc/00000", acc.toJson)
        },*/
        test("Bank integration test") {
          val bankAll = EndpointSpec.get[Unit](literal("bank")).out[Int]
            .implement ( _ => BankRepository.all("1000").map(_.size))
          val testRoutes = testApi(bankAll ++ bankByIdEndpoint) _
          testRoutes("/bank", "2") && testRoutes("/bank/COLSDE33", bank.toJson)
        },
         test("Customer integration test") {
          val custAllEndpoint = EndpointSpec.get[Unit](literal("cust")).out[Int]
            .implement(_ => CustomerRepository.all("1000").map(_.size))
          val testRoutes = testApi(custByIdEndpoint ++ custAllEndpoint) _
          testRoutes("/cust", "3") && testRoutes("/cust/5222", CustomerBuilder.dummy.toJson)
        },
        test("Supplier integration test") {
          val supAllEndpoint = EndpointSpec.get[Unit](literal("sup")).out[Int]
            .implement(_ => SupplierRepository.all("1000").map(_.size))
          val testRoutes = testApi(supByIdEndpoint ++ supAllEndpoint) _
          testRoutes("/sup", "6") && testRoutes("/sup/70000", sup.toJson)
        },
        test("Cost center integration test") {
          val ccAllEndpoint = EndpointSpec.get(literal("cc")).out[Int].implement(_ => CostcenterRepository.all("1000").map(_.size))
          val testRoutes = testApi(ccByIdEndpoint ++ ccAllEndpoint) _
          testRoutes("/cc", "2") && testRoutes("/cc/300", cc.toJson)
        },
          test("Module integration test") {
          val moduleAllEndpoint = EndpointSpec.get(literal("module")).out[Int].implement(_ => ModuleRepository.all("1000").map(_.size))
          val testRoutes = testApi(moduleByIdEndpoint ++ moduleAllEndpoint) _
          testRoutes("/module", "1") && testRoutes("/module/0000", m.toJson)
        },
           test("User integration test") {
          val userAllEndpoint = EndpointSpec.get(literal("user")).out[Int].implement(_ => UserRepository.all("1000").map(_.size))
          val testRoutes = testApi(userByUserNameEndpoint ++ userAllEndpoint) _
          testRoutes("/user", "2") && testRoutes("/user/jdegoes011", user.toJson)
        },
        test("Vat integration test") {
          val vatAllEndpoint = EndpointSpec.get(literal("vat")).out[Int].implement(_ => VatRepository.all("1000").map(_.size))
          val testRoutes = testApi(vatByIdEndpoint ++ vatAllEndpoint) _
          testRoutes("/vat", "2") && testRoutes("/vat/v101", vat1.toJson)
        }
    ).provide(ConnectionPool.live,  BankRepositoryImpl.live,
        CostcenterRepositoryImpl.live, CustomerRepositoryImpl.live, SupplierRepositoryImpl.live, VatRepositoryImpl.live,
        ModuleRepositoryImpl.live, UserRepositoryImpl.live, PostgresContainer.connectionPoolConfigLayer, PostgresContainer.createContainer)
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

