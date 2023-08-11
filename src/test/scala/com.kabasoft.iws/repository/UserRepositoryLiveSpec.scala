package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.User
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.ZLayer
import zio.sql.ConnectionPool
import zio.test.TestAspect._
import zio.test._

object UserRepositoryLiveSpec extends ZIOSpecDefault {

 val company ="1000"
  val id1 = 4710
  val id2 = 4712
  val id = 4711
  val name = "User1"
  val name2 = "User2"
  val userName = "myUserName"


  val users = List(
    User(id1, name,"User1", "User1", "hash1", "nophone", "user1@user.com",  "admin", "menuUser1",  company, 111),
    User(id2, name2,"User2", "User2", "hash2", "nophone", "user2@user.com",  "admin", "menuUser2",  company, 111)
  )

  val testLayer = ZLayer.make[UserRepository](
    UserRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec =
    suite("User repository test with postgres test container")(
      test("insert two new users and select them") {
        for {
          oneRow <- UserRepository.create2(users)
          count <- UserRepository.list(company).runCount
        } yield assertTrue(count==4L)  && assertTrue(oneRow==2)
      },
      test("get a User by  userName") {
        for {
          stmt <- UserRepository.getByUserName(userName,company)
        } yield   assertTrue(stmt.userName==userName)
      }
    ).provideLayerShared(testLayer.orDie) @@ sequential
}
