package models

import play.api.libs.json.{Json, OFormat}

case class User(_id: String,
           employeeId: String,
           name: String,
           email: String,
           mobileNumber: String,
           balance: BigDecimal)

object User {
  implicit lazy val format: OFormat[User] = Json.format

}
