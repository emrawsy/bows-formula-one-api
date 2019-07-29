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
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.commands.UpdateWriteResult
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

  "UserIdController" - {
    "when presentCard method is called should" - {
      "return ok and delete session if one already exists" in {

        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User("testId", "testEmployeeId", "testName", "testEmail", "testMobile", 12.99))))

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
          .thenReturn(Future.successful(Some(User("testId", "testEmployeeId", "testName", "testEmail", "testMobile", 12.99))))

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
      "return Not Found if player does not exist" in {

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

    "when findUser method is called" - {
      "return OK on a get with a valid ID" in {
        when(mockUserRepo.get(any()))
          .thenReturn(Future.successful(Some(User("testId", "testEmployeeId", "testName", "testEmail", "testMobile", 12.99))))

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
      "return OK on a get with a valid ID and delete data" in {
        ???
      }
      "return a Not Found if user isn't in database" in {
        ???
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
          .thenReturn(Future.successful(Some(User("testId", "testEmployeeId", "testName", "testEmail", "testMobile", 12.99))))

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
  }
}




