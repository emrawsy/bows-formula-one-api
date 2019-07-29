package models

import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import play.api.mvc.PathBindable

class CardIdSpec extends FreeSpec with MustMatchers {

  "CardID must" - {

//    val validCardId = "A1"
//    val invalidCardId = "@/.)("
//
//    "bind correct data" in {
//
//      val cardId = CardId(
//        _id = validCardId
//      )
//
//      val expected = Json.obj(
//        "_id" -> validCardId
//      )
//
//      PathBindable.bindableString(cardId)

//    }

    "unbind correct data" in {

    }

    "not bind invalid data" in {

    }

    "not unbind invalid data" in {

    }

  }

}
