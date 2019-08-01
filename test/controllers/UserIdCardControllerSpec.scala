package controllers

import java.time.LocalDateTime

import models.{CardId, User, UserSession}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.BSONDocument
import reactivemongo.core.errors.DatabaseException
import repositories.{SessionRepository, UserRepository}

import scala.concurrent.Future

class UserIdCardControllerSpec extends FreeSpec with MustMatchers with MockitoSugar with ScalaFutures {

  val mockUserRepo: UserRepository = mock[UserRepository]
  val mockSessionRepo: SessionRepository = mock[SessionRepository]

  private lazy val builder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder().overrides(
      bind[UserRepository].toInstance(mockUserRepo),
      bind[SessionRepository].toInstance(mockSessionRepo)
    )

  val cardId = CardId("testId")

  "UserIdController" - {
    "when presentCard method is called should" - {
      "return ok and delete session if one already exists" in {

        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))
        when(mockSessionRepo.get(any()))
          .thenReturn(Future.successful(Some(UserSession("testId", LocalDateTime.now))))
        when(mockSessionRepo.delete(any()))
          .thenReturn(Future.successful(UpdateWriteResult.apply(ok = true, 1, 1, Seq.empty, Seq.empty, None, None, None)))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.present(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe "Goodbye testName"

        app.stop
      }
      "return ok and create new session if none exist" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        when(mockSessionRepo.get(any()))
          .thenReturn(Future.successful(None))

        when(mockSessionRepo.create(any()))
          .thenReturn(Future.successful(UpdateWriteResult.apply(ok = true, 1, 1, Seq.empty, Seq.empty, None, None, None)))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.present(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe "Welcome testName"

        app.stop
      }
      "return Not Found if user does not exist" in {

        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(None))

        val app: Application = builder.build()
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.present(CardId("testId")).url)

        val result: Future[Result] = route(app, request).value

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe "Please register for an ID"

        app.stop
      }

      "return BadRequest if data in mongo is invalid" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(JsResultException(Seq.empty)))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.present(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe s"Could not parse Json to User model. Incorrect data!"

        app.stop
      }

      "return BadRequest if something else has failed" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(new Exception))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.present(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Failed with the following exception: java.lang.Exception"

        app.stop
      }
    }

    "when updateUser method is called" - {
      "return Ok on a post with valid data" in {
        when(mockUserRepo.update(any(), any(), any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.updateUser(CardId("testId"), "name", "newName").url)
        val result: Future[Result] = route(app, request).value



        status(result) mustBe OK
        contentAsString(result) mustBe "Successfully updated user with card ID testId's name to newName"

        app.stop
      }
      "return Not Found when invalid Id is sent" in {
        when(mockUserRepo.update(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.updateUser(CardId("testId"), "name", "newName").url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe "No user with that ID has been found"

        app.stop
      }
      "return BadRequest if something has failed" in {
        when(mockUserRepo.update(any(), any(), any()))
          .thenReturn(Future.failed(new Exception))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.updateUser(CardId("testId"), "name", "newName").url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Failed with the following exception: java.lang.Exception"

        app.stop
      }
    }

    "when findUser method is called" - {
      "return OK on a get with a valid ID" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.findUser(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK
        contentAsString(result) must contain
        Json.obj("_id" -> "testId",
          "employeeId" -> "testEmployeeId",
          "name" -> "testName",
          "email" -> "testEmail",
          "mobileNumber" -> "testMobile",
          "balance" -> 12.99)

        app.stop

      }
      "return a Not Found if user isn't in database" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(None))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.findUser(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe "Please register for an ID"

        app.stop
      }
      "return BadRequest if data in mongo is invalid" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(JsResultException(Seq.empty)))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.findUser(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe s"Could not parse Json to User model."

        app.stop
      }

      "return BadRequest if something else has failed" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(new Exception))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.findUser(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Failed with the following exception: java.lang.Exception"

        app.stop
      }
    }

    "when deleteUser is called" - {
      "return OK on a DELETE with a valid ID and delete data" in {
        when(mockUserRepo.delete(any()))
          .thenReturn(Future.successful(Some(Json.obj(
            "_id" -> "testId",
            "employeeId" -> "testEmployeeId",
            "name" -> "testName",
            "email" -> "testEmail",
            "mobileNumber" -> "testMobile",
            "balance" -> 12.99
          ))))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(DELETE, routes.UserIdCardController.deleteUser(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe s"Successfully deleted user with card ID: testId"

        app.stop
      }

      "return a Not Found if user isn't in database" in {
        when(mockUserRepo.delete(any())).thenReturn(Future.successful(None))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(DELETE, routes.UserIdCardController.deleteUser(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe "No user found with that card ID"

        app.stop
      }
      "return BadRequest if something else has failed" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(new Exception))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.deleteUser(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Failed with the following exception: java.lang.Exception"

        app.stop
      }
    }

    "when findUserBalance method is called" - {
      "return OK on a get with a valid ID" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.findUserBalance(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe "12.99"

        app.stop
      }

      "return a Not Found if user isn't in database" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(None))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.findUserBalance(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe "User with this ID has not been found"

        app.stop
      }

      "return BadRequest if data in mongo is invalid" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(JsResultException(Seq.empty)))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.findUserBalance(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe s"Could not parse Json to User model."

        app.stop
      }

      "return BadRequest if something else has failed" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(new Exception))

        val app: Application = builder.build()

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.UserIdCardController.findUserBalance(CardId("testId")).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Failed with the following exception: java.lang.Exception"

        app.stop
      }
    }

    "when postNewUser method is called" - {
      "return OK on a post with valid json" in {
        when(mockUserRepo.put(any()))
          .thenReturn(Future.successful(UpdateWriteResult.apply(ok = true, 1, 1, Seq.empty, Seq.empty, None, None, None)))

        val userJson: JsValue = Json.toJson(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.postNewUser().url).withBody(userJson)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe "New user has been added to database!"

        app.stop
      }

      "return BadRequest if data in mongo is invalid" in {
        val userJson: JsValue = Json.toJson("Invalid Json")

        val app: Application = builder.build()

        val request =
          FakeRequest(POST, routes.UserIdCardController.postNewUser().url).withBody(userJson)

        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Could not parse Json to User model."

        app.stop
      }
      "return BAD_REQUEST and correct error message when duplicate data is input" in {

        when(mockUserRepo.put(any()))
          .thenReturn(Future.failed(new DatabaseException {
            override def originalDocument: Option[BSONDocument] = None

            override def code: Option[Int] = None

            override def message: String = "Duplicate key"
          }))

        val userJson: JsValue = Json.toJson(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))

        val app: Application = builder.build()

        val request =
          FakeRequest(POST, routes.UserIdCardController.postNewUser().url).withBody(userJson)

        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Could not parse Json to User model. Duplicate key error!"

        app.stop

      }

      "return BadRequest if something else has failed" in {
        when(mockUserRepo.put(any()))
          .thenReturn(Future.failed(new Exception))

        val userJson: JsValue = Json.toJson(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))

        val app: Application = builder.build()

        val request =
          FakeRequest(POST, routes.UserIdCardController.postNewUser().url).withBody(userJson)

        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Failed with the following exception: java.lang.Exception"

        app.stop
      }
    }

    "when topUpBalance method is called" - {
      "return OK on a post with valid data" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        when(mockUserRepo.increase(any(), any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.topUpBalance(CardId("testId"), 10).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe "Top-up complete: balance is now: 22.99"

        app.stop
      }
      "return BadRequest if amount is less than zero" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        when(mockUserRepo.increase(any(), any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.topUpBalance(CardId("testId"), -8).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Minimum increase must be greater than zero"

        app.stop
      }

      "return NOT FOUND on a invalid id" in {

        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(None))

        when(mockUserRepo.increase(any(), any()))
          .thenReturn(Future.successful(None))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.topUpBalance(CardId("testId"), 10).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe "User not found"

        app.stop


      }

      "return BadRequest if data in mongo is invalid" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(JsResultException(Seq.empty)))

        val app: Application = builder.build()

        val request =
          FakeRequest(POST, routes.UserIdCardController.topUpBalance(CardId("testId"), 10).url)

        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Could not parse Json to User model."

        app.stop
      }

      "return BadRequest if something else has failed" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(new Exception))

        val app: Application = builder.build()

        val request =
          FakeRequest(POST, routes.UserIdCardController.topUpBalance(CardId("testId"), 10).url)

        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Failed with the following exception: java.lang.Exception"

        app.stop
      }
    }
    "when transaction method is called" - {
      "return OK on a post with valid data" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        when(mockUserRepo.decrease(any(), any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.transaction(CardId("testId"), 10).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe "Transaction complete: balance is now: 2.99"

        app.stop
      }

      "return BadRequest if transaction amount is less than zero" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        when(mockUserRepo.decrease(any(), any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.transaction(CardId("testId"), -8).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Must not be less than zero"

        app.stop
      }

      "return BadRequest if balance is less than transaction amount" in {

        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        when(mockUserRepo.decrease(any(), any()))
          .thenReturn(Future.successful(Some(User(cardId, "testEmployeeId", "testName", "testEmail", "testMobile", 12.99, 1))))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.transaction(CardId("testId"), 20).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "You do not have enough money for this transaction"

        app.stop
      }

      "return NOT FOUND on a invalid id" in {

        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(None))

        when(mockUserRepo.decrease(any(), any()))
          .thenReturn(Future.successful(None))

        val app: Application = builder.build()

        val request = FakeRequest(POST, routes.UserIdCardController.transaction(CardId("testId"), 10).url)
        val result: Future[Result] = route(app, request).value

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe "User not found"
        app.stop
      }

      "return BadRequest if data in mongo is invalid" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(JsResultException(Seq.empty)))

        val app: Application = builder.build()

        val request =
          FakeRequest(POST, routes.UserIdCardController.transaction(CardId("testId"), 10).url)

        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Could not parse Json to User model."

        app.stop
      }

      "return BadRequest if something else has failed" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.failed(new Exception))

        val app: Application = builder.build()

        val request =
          FakeRequest(POST, routes.UserIdCardController.transaction(CardId("testId"), 10).url)

        val result: Future[Result] = route(app, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Failed with the following exception: java.lang.Exception"

        app.stop
      }
    }
  }
}




