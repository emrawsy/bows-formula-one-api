package repositories

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import javax.inject.Inject
import models.{CardId, User}
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.WriteConcern
import reactivemongo.api.commands.{FindAndModifyCommand, WriteResult}
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class UserRepository @Inject()(mongo: ReactiveMongoApi)(implicit ec: ExecutionContext) {

  private def findAndUpdate(collection: JSONCollection, selection: JsObject,
                            modifier: JsObject): Future[FindAndModifyCommand.Result[collection.pack.type]] = {
    collection.findAndUpdate(
      selector = selection,
      update = modifier,
      fetchNewObject = true,
      upsert = false,
      sort = None,
      fields = None,
      bypassDocumentValidation = false,
      writeConcern = WriteConcern.Default,
      maxTime = None,
      collation = None,
      arrayFilters = Seq.empty
    )
  }

  //create the collection in MongoDB
  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("users"))

  //find a user
  def get(card: CardId): Future[Option[User]] =
    collection.flatMap(_.find(
      Json.obj("_id" -> card._id),
      None
    ).one[User])

  //update a users details
  def update(card: CardId, key: String, value: String): Future[Option[User]] = {
    collection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> card._id)
        val modifier: JsObject = Json.obj("$set" -> Json.obj(key -> value))
        findAndUpdate(result, selector, modifier).map(_.result[User])
    }
  }

  //create a new user
  def put(user: User): Future[WriteResult] = {
    collection.flatMap(_.insert.one(user))
  }

  //deletes a user by ID
  def delete(card: CardId) = {
    collection.flatMap(
      _.findAndRemove(Json.obj("_id" -> card._id), None, None, WriteConcern.Default, None, None, Seq.empty).map(
        _.value
      )
    )
  }
//decreases balance
  def decrease(cardId: CardId, value: BigDecimal): Future[Option[User]] = {
    collection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> cardId._id)
        val modifier: JsObject = Json.obj("$inc" -> Json.obj("balance" -> -value))
        findAndUpdate(result, selector, modifier).map(_.result[User])
    }
  }
//increases balance
  def increase(cardId: CardId, value: BigDecimal): Future[Option[User]] = {
    collection.flatMap {
      result =>
        val selector: JsObject = Json.obj("_id" -> cardId._id)
        val modifier: JsObject = Json.obj("$inc" -> Json.obj("balance" -> value))
        findAndUpdate(result, selector, modifier).map(_.result[User])
    }
  }
}
