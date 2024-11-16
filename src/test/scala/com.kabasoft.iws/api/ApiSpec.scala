package com.kabasoft.iws.api

/*
import zio.json.EncoderOps
import com.kabasoft.iws.repository.{AccountRepositoryLive, CustomerRepositoryLive, FinancialsTransactionRepository, 
  FinancialsTransactionRepositoryLive, MasterfileRepositoryLive,  ModuleRepositoryLive, SupplierRepositoryLive,
  UserRepository, UserRepositoryLive, VatRepositoryLive}
import com.kabasoft.iws.api.Protocol.*
import com.kabasoft.iws.domain.BankBuilder.{bank, bankx}
import com.kabasoft.iws.api.AccountEndpoint.{accountAllRoute, accountModifyRoute, accountByIdRoute, accountCreateRoute}
import com.kabasoft.iws.api.CustomerEndpoint.{customerCreateRoute, customerByIdRoute, customerDeleteRoute}
import com.kabasoft.iws.api.FinancialsEndpoint.financialsRoutes
import com.kabasoft.iws.api.MasterfileEndpoint.{masterfileAllRoute, masterfileByIdRoute, masterfileCreateRoute, masterfileDeleteRoute}
import com.kabasoft.iws.api.ModuleEndpoint.{moduleByIdEndpoint, moduleCreateEndpoint, moduleDeleteEndpoint}
import com.kabasoft.iws.api.SupplierEndpoint.{supByIdEndpoint, supCreateEndpoint, supDeleteEndpoint}
import com.kabasoft.iws.api.UserEndpoint.{userByUserNameEndpoint, userDeleteEndpoint}
import com.kabasoft.iws.api.VatEndpoint.{vatByIdEndpoint, vatCreateEndpoint, vatDeleteEndpoint}
import com.kabasoft.iws.domain.AccountBuilder.{acc, accx}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.CostcenterBuilder.ccx
import com.kabasoft.iws.domain.CustomerBuilder.{cust, custx}
import com.kabasoft.iws.domain.ModuleBuilder.mx
import com.kabasoft.iws.domain.SupplierBuilder.supx
import com.kabasoft.iws.domain.UserBuilder.userx
import com.kabasoft.iws.domain.VatBuilder.vat1x
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.http.{Body, Response, Routes}
import com.kabasoft.iws.domain.CostcenterBuilder.cc
import com.kabasoft.iws.domain.ModuleBuilder.m
import com.kabasoft.iws.domain.SupplierBuilder.sup
import com.kabasoft.iws.domain.User
import com.kabasoft.iws.domain.UserBuilder.user
import com.kabasoft.iws.domain.VatBuilder.vat1
import zio.*
import zio.http.*
import zio.http.endpoint.Endpoint
import zio.http.codec.HttpCodec._
import com.kabasoft.iws.repository.Schema.*
import zio.http.Status
import zio.http.{Request, URL}
import zio.schema.DeriveSchema.gen
//import zio.sql.ConnectionPool
import zio.test.*

object ApiSpec extends ZIOSpecDefault {

    def spec = suite("APISpec")(
      suite("handler")(
        test("Account  integration test ") {
        //val accAll = Endpoint.get(("acc")/int("modelid")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
        //             .implement(Id => AccountCache.all(Id).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(accountAllRoute ++accountByIdRoute) _
          val deleteRoutes = testDeleteApi(accountDeleteRoute) _
          val postRoutes = testPostApi(accountCreateRoute) _
            testRoutes("/acc/"+acc.modelid+"/" +acc.company, "22") && //testRoutes("/acc/"+acc.id+"/"+acc.company, acc.toJson)&&
            deleteRoutes("/acc/"+acc.id+"/"+acc.company, "1") && postRoutes("/acc",  accx.toJson,  accx.toJson)

        },

        test("financials integration test") {
          val ftrByModelId = Endpoint.get("ftr1" / string("company") / int("modelid")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement(p => ZIO.logInfo(s"Find  Transaction by modelId  ${p}") *>
              FinancialsTransactionCache.getByModelId((p._2, p._1)).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val ftrByTransId = Endpoint.get("ftr2" / string("company") / int("transid")).out[BigDecimal].outError[RepositoryError](Status.InternalServerError)
            .implement(p => ZIO.logInfo(s"Find  Transaction by TransId  ${p}") *>
              FinancialsTransactionRepository.getByTransId((p._2.toLong, p._1)).mapBoth(e => RepositoryError(e.getMessage), _.total))

         // val testRoutes1 = testPostApi(ftrCreateEndpoint) _
          val testRoutes = testApi(ftrByModelId ++ ftrByTransId ++ ftrModifyEndpoint) _
          //val testRoutes2 = testPutApi( ftrModifyEndpoint) _
         // val payload = ftr5.toJson
         // val result = ftr5.toJson//.toString.replaceAll("terms","modified")

          //val deleteRoutes = testDeleteApi(bankDeleteEndpoint) _

          //testRoutes1("/ftr", FTR, "2") &&
          testRoutes("/ftr2/1000/1" , "100.00") && testRoutes("/ftr1/1000/"+124, "1") //&&
          //testRoutes1("/ftr", ftr4.toJson, ftr4.toJson.replaceAll("id:-1", "id:2"))
          //testRoutes2("/ftr", payload, payload)
          // deleteRoutes("/bank/" + bank.id + "/" + bank.company, "1") && testRoutes1("/bank", bankx.to)Json, "1")
        },
        test("Bank integration test") {
          val bankAll = Endpoint.get("mf"/int("modelid")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement ( p => MasterfileCache.all(p).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(masterfileAllRoute ++ masterfileByIdRoute++masterfileCreateRoute) _
          val deleteRoutes = testDeleteApi(masterfileDeleteRoute) _
          val testRoutes1 = testPostApi(masterfileByIdRoute) _
          testRoutes("/mf/"+bank.modelid+"/"+bank.company, "2") && testRoutes("/mf/"+bank.id+"/"+bank.modelid+"/"+bank.company, bank.toJson) &&
            deleteRoutes("/mf/"+bank.id+"/"+bank.modelid+"/"+bank.company, "2")&& testRoutes1("/mf", bankx.toJson, bankx.toJson)
        },
         test("Customer integration test") {
          val custAllEndpoint = Endpoint.get("cust"/int("modelid")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement(Id => CustomerCache.all(Id).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(custByIdEndpoint ++ custAllEndpoint) _
           val deleteRoutes = testDeleteApi(custDeleteEndpoint) _
          val testRoutes1 = testPostApi(custCreateEndpoint) _
          //testRoutes("/cust/"+cust.modelid+"/"+cust.company, "3") &&
            testRoutes("/cust/"+cust.id+"/"+cust.company, cust.toJson)&&
          deleteRoutes("/cust/"+cust.id+"/"+cust.company, "1")&&testRoutes1("/cust", custx.toJson, custx.toJson)
        },
        test("Supplier integration test") {
          val supAllEndpoint = Endpoint.get("sup"/int("modelid")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement(p => SupplierCache.all(p).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(supByIdEndpoint ++ supAllEndpoint) _
          val deleteRoutes = testDeleteApi(supDeleteEndpoint) _
          val testRoutes1 = testPostApi(supCreateEndpoint) _
          //testRoutes("/sup/"+sup.modelid+"/"+sup.company, "6") &&
            testRoutes("/sup/"+sup.id+"/"+sup.company, sup.toJson)&&
          deleteRoutes("/sup/"+sup.id+"/"+sup.company, "1")&&testRoutes1("/sup", supx.toJson, supx.toJson)
        },
        test("Cost center integration test") {
          val ccAllEndpoint = Endpoint.get("mf"/int("company")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement(p => MasterfileCache.all(p).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(mByIdEndpoint ++ ccAllEndpoint) _
          val deleteRoutes = testDeleteApi(mDeleteEndpoint) _
          val testRoutes1 = testPostApi(mCreateEndpoint) _
          testRoutes("/mf/"+cc.modelid+"/"+cc.company, "2") && testRoutes("/mf/"+cc.id+"/"+cc.modelid+"/"+cc.company, cc.toJson)&&
            deleteRoutes("/mf/"+cc.id+"/"+cc.modelid+"/"+cc.company, "2")&&testRoutes1("/mf", ccx.toJson, ccx.toJson)
        },
          test("Module integration test") {
          val moduleAllEndpoint = Endpoint.get("module"/int("modelid")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement(p => ModuleCache.all(p).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(moduleByIdEndpoint ++ moduleAllEndpoint) _
            val deleteRoutes = testDeleteApi(moduleDeleteEndpoint) _
          val testRoutes1 = testPostApi(moduleCreateEndpoint) _
          //testRoutes("/module/"+m.modelid+"/"++m.company, "1") &&
            testRoutes("/module/"+m.id+"/"+m.company, m.toJson)&&
            deleteRoutes("/module/"+m.id+"/"+m.company, "1")&&testRoutes1("/module", mx.toJson, mx.toJson)
        },
        test("Vat integration test") {
          val vatAllEndpoint = Endpoint.get("vat"/int("modelid")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement(p => VatCache.all(p).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(vatByIdEndpoint ++ vatAllEndpoint) _
          val deleteRoutes = testDeleteApi(vatDeleteEndpoint) _
          val testRoutes1 = testPostApi(vatCreateEndpoint) _
          //testRoutes("/vat/"+vat1.modelid+"/"+vat1.company,  "2") &&
          testRoutes("/vat/"+vat1.id+"/"+vat1.company, vat1.toJson) &&
          deleteRoutes("/vat/"+vat1.id+"/"+vat1.company, "1")&&testRoutes1("/vat", vat1x.toJson, vat1x.toJson)
        },
        test("User integration test") {
          val userAllEndpoint = Endpoint.get("user" / string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
            .implement((company: String) => UserRepository.all((User.MODELID, company)).mapBoth(e => RepositoryError(e.getMessage), _.size))
          val testRoutes = testApi(userByUserNameEndpoint ++ userAllEndpoint) _
          val deleteRoutes = testDeleteApi(userDeleteEndpoint) _
          //val testRoutes1 = testPostApi(userCreateEndpoint) _
          testRoutes("/user/1000", "2") && testRoutes("/user/" + user.userName + "/" + user.company, user.toJson) &&
            deleteRoutes("/user/" + userx.id + "/" + userx.company, "1") //&& testRoutes1("/user", userx.toJson, "1")
        }
    ).provideShared(ConnectionPool.live,  AccountRepositoryImpl.live, AccountCacheImpl.live, CustomerRepositoryLive.live,
        CustomerCacheImpl.live, SupplierRepositoryImpl.live, SupplierCacheImpl.live, VatRepositoryImpl.live, VatCacheImpl.live,
        ModuleRepositoryImpl.live, ModuleCacheImpl.live, UserRepositoryImpl.live, PostgresContainer.connectionPoolConfigLayer,
        FinancialsTransactionRepositoryImpl.live, FinancialsTransactionCacheImpl.live,  MasterfileRepositoryImpl.live,
        MasterfileCacheImpl.live, PostgresContainer.createContainer)
    )

  def testApi[R, E](service: Routes[R, E, None])(
    url: String, expected: String): ZIO[R, Response, TestResult] = {
    val request = Request.get(url = URL.decode(url).toOption.get)
    for {
      response <- service.toApp.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }

  def testDeleteApi[R, E](service: Routes[R, E, None])(
    url: String, expected: String): ZIO[R, Response, TestResult] = {
    val request = Request.delete(url = URL.decode(url).toOption.get)
    for {
      response <- service.toApp.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }

  def testPostApi[R, E](routes: Routes[R, E, None])(
    url: String, body: String, expected: String): ZIO[R, Response, TestResult] = {
    val request = Request.post(Body.fromString(body), URL.decode(url).toOption.get)
    for {
      response <- routes.toApp.runZIO(request).mapError(_.get)
      body_ <- response.body.asString.orDie
    } yield assertTrue(body_ == expected)
  }
  def testPutApi[R, E](routes:  Routes[R, E, None] )(
    url: String, body:String, expected: String): ZIO[R, Response, TestResult] = {
    val request = Request.put(Body.fromString(body), URL.decode(url).toOption.get)
    for {
      response <- routes.toApp.runZIO(request).mapError(_.get)
      body <- response.body.asString.orDie
    } yield assertTrue(body == expected)
  }
}
 */

