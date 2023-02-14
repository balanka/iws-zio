package com.kabasoft.iws.domain

object UserBuilder {
  val userId = 1
  val fname = "John"
  val lname = "dgoes"
  val userName ="jdegoes011"
  val hash = "$21a$10$0IZtq3wGiRQSMIuoIgNKrePjQfmGFRgkpnHwyY9RcbUhZxU9Ha1mCX"
  val phone = "11-4711-0123"
  val email = "whatYourGrandma1Name@gmail.com"
  val department ="Admin"
  val companyId ="1000"
  val modelid = 111
  val menu ="1300,9,1120,3,14,1000,18,1,112,106,11,6,10"

  val user = User(userId, userName, fname, lname, hash, phone , email, department, menu,  companyId, modelid)


}

