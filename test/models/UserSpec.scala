package models

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json

class UserSpec extends FreeSpec with MustMatchers {

  "User model" - {

    val id = CardId("id")
    val employeeId = "AB001"
    val name = "name"
    val email = "email@mail.com"
    val mobileNumber = "0987654321"
    val userBalance = 125.55
    val pin = 1

    "must serialise into JSON" in {

      val user = User(
        _id = id,
        employeeId = employeeId,
        name = name,
        email = email,
        mobileNumber = mobileNumber,
        balance = userBalance,
        pinNumber = pin
      )

      val expectedJson = Json.obj(
        "_id" -> id,
        "employeeId" -> employeeId,
        "name" -> name,
        "email" -> email,
        "mobileNumber" -> mobileNumber,
        "balance" -> userBalance,
        "pinNumber" -> pin
      )

      Json.toJson(user) mustEqual expectedJson
    }

    "must deserialise from JSON" in {

      val json = Json.obj(
        "_id" -> id,
        "employeeId" -> employeeId,
        "name" -> name,
        "email" -> email,
        "mobileNumber" -> mobileNumber,
        "balance" -> userBalance,
        "pinNumber" -> pin
      )

      val expectedUser = User(
        _id = id,
        employeeId = employeeId,
        name = name,
        email = email,
        mobileNumber = mobileNumber,
        balance = userBalance,
        pinNumber = pin
      )

      json.as[User] mustEqual expectedUser
    }
  }

}
