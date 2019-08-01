package controllers

import java.time.LocalDateTime

import javax.inject.Inject
import models.{CardId, User, UserSession}
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc.{AbstractController, ControllerComponents}
import reactivemongo.core.errors.DatabaseException
import repositories.{SessionRepository, UserRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class UserIdCardController @Inject()(cc: ControllerComponents,
                                     userRepo: UserRepository,
                                     sessionRepo: SessionRepository)
                                    (implicit ec: ExecutionContext) extends AbstractController(cc) {


  //find user by id, find session
  def present(cardId: CardId) = Action.async {
    implicit request =>
      userRepo.get(cardId).flatMap {
        case Some(user) =>
          sessionRepo.get(cardId).flatMap {
            case Some(_) => sessionRepo.delete(cardId).map(_ => Ok(s"Goodbye ${user.name}"))
            case None => sessionRepo.create(UserSession(cardId._id, LocalDateTime.now)).map(_ => Ok(s"Welcome ${user.name}"))
          }
        case None => Future.successful(NotFound("Please register for an ID"))
        case _ => Future.successful(BadRequest("Failed request."))
      } recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to User model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }

  //create User
  def postNewUser() = Action.async(parse.json) {
    implicit request =>
      (for {
        user <- Future.fromTry(Try {
          request.body.as[User]
        })
        result <- userRepo.put(user)
      } yield Ok("New user has been added to database!")).recoverWith {
        case e: JsResultException => Future.successful(BadRequest("Could not parse Json to User model."))
        case e: DatabaseException => Future.successful(BadRequest(s"Could not parse Json to User model. Duplicate key error!"))
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
        case _ => Future.successful(BadRequest("Failed request."))
      }
  }

  //Retrieve User
  def findUser(cardId: CardId) = Action.async {
    implicit request =>
      userRepo.get(cardId).map {
        case Some(user) => Ok(Json.toJson(user))
        case None => NotFound("Please register for an ID")
        case _ => BadRequest("Failed request.")
      } recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to User model."))
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }

  //delete User by cardID
  def deleteUser(cardId: CardId) = Action.async {
    implicit request =>
      userRepo.delete(cardId).map {
        case Some(_) => Ok(s"Successfully deleted user with card ID: ${cardId._id}")
        case _ => NotFound("No user found with that card ID")
      } recoverWith {
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }

  //update user field with new data
  def updateUser(cardId: CardId, key: String, data: String) = Action.async {
    implicit request =>
      userRepo.update(cardId, key, data).map {
        case Some(user) => Ok(s"Successfully updated user with card ID ${user._id._id}'s $key to $data")
        case _ => NotFound("No user with that ID has been found")
      } recoverWith {
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }

  //retrieve user balance only
  def findUserBalance(cardId: CardId) = Action.async {
    implicit request =>
      userRepo.get(cardId).map {
        case Some(user) => Ok(Json.toJson(user.balance))
        case None => NotFound("User with this ID has not been found")
      } recoverWith {
        case e: JsResultException => Future.successful(BadRequest(s"Could not parse Json to User model."))
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }


  def topUpBalance(cardId: CardId, topUp: BigDecimal) = Action.async {
    implicit request =>
      userRepo.get(cardId).flatMap {
        case Some(user) =>
          topUp match {
            case x if x <= 0 => Future.successful(BadRequest("Minimum increase must be greater than zero"))
            case _ =>
              userRepo.get(cardId).flatMap {
                case Some(_) => userRepo.increase(cardId, topUp)
                  .map { _ => Ok(s"Top-up complete: balance is now: ${user.balance + topUp}") }
              }
          }
        case None => Future.successful(NotFound("User not found"))
      } recoverWith {
        case e: JsResultException => Future.successful(BadRequest(s"Could not parse Json to User model."))
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }

  def transaction(cardId: CardId, transaction: BigDecimal) = Action.async {
    implicit request =>
      userRepo.get(cardId).flatMap {
        case Some(user) =>
          transaction match {
            case x if x <= 0 => Future.successful(BadRequest("Must not be less than zero"))
            case x if x > user.balance => Future.successful(BadRequest("You do not have enough money for this transaction"))
            case _ =>
              userRepo.get(cardId).flatMap {
                case Some(user) =>
                  userRepo.decrease(cardId, transaction).map {
                    case Some(_) => Ok(s"Transaction complete: balance is now: ${user.balance - transaction}")
                  }
              }
          }
        case None => Future.successful(NotFound("User not found"))
      } recoverWith {
        case e: JsResultException => Future.successful(BadRequest(s"Could not parse Json to User model."))
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }
}
