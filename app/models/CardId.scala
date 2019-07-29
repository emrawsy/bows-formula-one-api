package models

import play.api.mvc.PathBindable

case class CardId(_id: String)

object CardId {

  implicit val pathBindable: PathBindable[CardId] = {
    new PathBindable[CardId] {
      override def bind(key: String, value: String): Either[String, CardId] = {
        if (value.matches("^[a-zA-Z0-9]+$")) {
          Right(CardId(value))
        } else {
          Left("Invalid Card ID")
        }
      }

      override def unbind(key: String, value: CardId): String = {
        value._id
      }
    }
  }
}
