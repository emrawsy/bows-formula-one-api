package models

sealed trait UpsertAction

object UpsertAction {

  case object Inserted extends UpsertAction
  case object Updated extends UpsertAction
}
