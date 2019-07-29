package controllers

import java.time.LocalDateTime

import javax.inject.Inject
import models.{CardId, User, UserSession}
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc.{AbstractController, ControllerComponents}
import repositories.{SessionRepository, UserRepository}

import scala.concurrent.{ExecutionContext, Future}

class UserIdCardController @Inject()(cc: ControllerComponents,
                                     userRepo: UserRepository,
                                     sessionRepo: SessionRepository)
                                    (implicit ec: ExecutionContext) extends AbstractController(cc) {


  //Read and delete
  def present(cardId: CardId) = Action.async {
    implicit request =>
      userRepo.get(cardId).flatMap {
        case Some(user) =>
          sessionRepo.get(cardId).flatMap {
            case Some(_) => sessionRepo.delete(cardId).map(_ => Ok(s"Goodbye ${user.name}"))
            case None => sessionRepo.create(UserSession(cardId._id, LocalDateTime.now)).map(_ => Ok(s"Welcome ${user.name}"))
          }
        case None => Future.successful(NotFound("Please register for an ID"))
      } recoverWith {
        case e: JsResultException => Future.successful(BadRequest(s"Could not parse Json to User model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }

  //Retrieve User
  def findUser(cardId: CardId) = Action.async {
    implicit request =>
      userRepo.get(cardId).map {
        case Some(user) => Ok(Json.toJson(user))
        case None => NotFound("Please register for an ID")
      } recoverWith {
        case e: JsResultException => Future.successful(BadRequest(s"Could not parse Json to User model."))
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }

  //create User
  def postNewUser() = Action.async(parse.json) {
    implicit request =>
      userRepo.put(request.body.as[User]).map(
        _ => Ok("New user has been added to database!")
      ) recoverWith {
        case e: JsResultException => Future.successful(BadRequest(s"Could not parse Json to User model: ${e.getMessage}"))
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }

  //delete User by cardID
  def deleteUser(cardId: CardId) = Action.async {
    implicit request =>
      userRepo.delete(cardId).map(
        result =>
          result.value match {
            case Some(_) => Ok(s"Successfully deleted user with card ID: ${cardId._id}")
            case _ => NotFound("No user found with that card ID")
          }) recoverWith {
        case e => Future.successful(BadRequest(s"Failed with the following exception: $e"))
      }
  }

  //update user field with new data
  def updateUser(cardId: CardId, key: String, data: String) = Action.async {
    implicit request =>
      userRepo.update(cardId, key, data).map {
        case Some(user) => Ok(s"Successfully updated user with card ID ${user._id}'s $key to $data")
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


  def transaction(cardId: CardId, transaction: BigDecimal) = Action.async {
    implicit request =>
      userRepo.get(cardId).flatMap {
        case Some(user) if (user.balance < transaction) => Future.successful(BadRequest("Balance is not high enough"))
        case Some(user) if (user.balance >= transaction)=>
          userRepo.decrease(cardId, user.balance, transaction).map {
            case Some(_) => Ok(s"Transaction complete: balance is now ${user.balance-transaction}")
            case None => NotFound("No balance for user")
          }
        case _ => Future.successful(NotFound("User not found"))
      } recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to User model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
      }
  }

  def topUpBalance(cardId: CardId, topUp: BigDecimal) = Action.async {
    implicit request =>
      userRepo.get(cardId).flatMap {
        case Some(user) if (user.balance < 0) => Future.successful(BadRequest("Should not be minus"))
        case Some(user) if (user.balance >= 0)=>
          userRepo.increase(cardId, user.balance, topUp).map {
            case Some(_) => Ok(s"Transaction complete: balance is now ${user.balance+topUp}")
            case None    => NotFound("No balance for user")
          }
        case _ => Future.successful(NotFound("User not found"))
      } recoverWith {
        case _: JsResultException => Future.successful(BadRequest(s"Could not parse Json to Player model. Incorrect data!"))
        case e => Future.successful(BadRequest(s"Something has gone wrong with the following exception: $e"))
      }
  }
}
