package models

import org.scalatest.{EitherValues, FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.{JsString, Json}
import play.api.mvc.PathBindable

class CardIdSpec extends FreeSpec with MustMatchers with EitherValues with OptionValues {

  "CardID must" - {

    val validCardId = "A1"
    val invalidCardId = "$%^&*"

    val pathBindable = implicitly[PathBindable[CardId]]

    "serialise correct data" in {

      val cardId = CardId(validCardId)

      val expectedJson = JsString(validCardId)

      Json.toJson(cardId) mustEqual expectedJson
    }
    "deserialise correct data" in {

      val expectedCardId = CardId(
        _id = validCardId
      )

      val json = JsString(validCardId)

      json.as[CardId] mustEqual expectedCardId

    }
    "bind correct data" in {
      val result = CardId("A1")

      pathBindable.bind("", validCardId) mustBe Right(result)
    }
    "not bind invalid characters to CardId" in {
      val result = "Invalid Card ID"

      pathBindable.bind("", invalidCardId) mustBe Left(result)
    }
    "unbind correct data" in {
      val result = validCardId

      pathBindable.unbind("", CardId(validCardId)) mustBe validCardId
    }
  }
}
