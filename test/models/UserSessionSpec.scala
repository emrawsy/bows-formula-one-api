package models


import java.time.{LocalDateTime, ZoneOffset}

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import repositories.MongoDateTimeFormats

class UserSessionSpec extends FreeSpec with MustMatchers with MongoDateTimeFormats {

  "UserSession model" - {

    val id = "A1"
    val time = LocalDateTime.now

    "must serialise intp JSON" in {

      val userSession = UserSession(
        _id = id,
        lastUpdated = time
      )

      val expectedJson = Json.obj(
        "_id" -> id,
        "lastUpdated" -> time
      )

      Json.toJson(userSession) mustEqual expectedJson
    }

    "must deserialise from JSON" in {

      val json = Json.obj(
        "_id" -> id,
        "lastUpdated" -> time
      )

      val expectedUser = UserSession(
        _id = id,
        lastUpdated = time.minusHours(1)
      )

      json.as[UserSession] mustEqual expectedUser
    }
  }

}
