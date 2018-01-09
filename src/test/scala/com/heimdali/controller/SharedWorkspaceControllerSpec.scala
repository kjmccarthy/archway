package com.heimdali.controller

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.heimdali.models.ViewModel.SharedWorkspace
import com.heimdali.rest.{AuthService, WorkspaceController}
import com.heimdali.services._
import com.heimdali.test.fixtures._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.extras.Configuration
import io.circe.parser._
import org.apache.http.util.EntityUtils
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.Future

class SharedWorkspaceControllerSpec
  extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with MockFactory
    with ScalatestRouteTest
    with FailFastCirceSupport
    with BeforeAndAfterEach {

  implicit val configuration: Configuration = Configuration.default.withDefaults.withSnakeCaseKeys

  import io.circe.java8.time._
  import io.circe.generic.extras.auto._

  behavior of "ProjectController"

  it should "create a project" in {
    val Right(json) = parse(
      """
        | {
        |   "name": "Sesame",
        |   "purpose": "to do something cool",
        |   "compliance": {
        |     "pii_data": false,
        |     "phi_data": false,
        |     "pci_data": false
        |   },
        |   "hdfs": {
        |     "requested_size_in_gb": 0.2
        |   },
        |   "yarn": {
        |     "max_cores": 1,
        |     "max_memory_in_gb": 0.2
        |   }
        | }
      """.stripMargin)

    println(json)

    val workspaceService = mock[WorkspaceService]
    (workspaceService.create _).expects(*).returning(Future(TestProject()))
    val authService = mock[AuthService]
    (authService.validateToken _).expects(*).returning(Future(Some(User("", ""))))
    val restApi = new WorkspaceController(authService, workspaceService)

    Post("/workspaces", json) ~> addCredentials(OAuth2BearerToken("AbCdEf123456")) ~> restApi.route ~> check {
      responseEntity.toString
      status should be(StatusCodes.Created)
      val response = responseAs[SharedWorkspace]

      val id = response.id
      response.name should be(TestProject.name)
      response.purpose should be(TestProject.purpose)
      response.systemName should be(TestProject.systemName)

      response.compliance.piiData should be(false)
      response.compliance.phiData should be(false)
      response.compliance.pciData should be(false)

      implicit val dateOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)
      response.created should be < LocalDateTime.now

      response.createdBy should be(standardUsername)
    }
  }

  it should "not accept read-only fields" in {
    val oldDate = LocalDateTime.of(2010, 1, 1, 0, 0, 0)
    val oldDateString = oldDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val wrongUser = "johnsmith"
    val fakeId = 999
    val Right(json) = parse(
      s"""
         | {
         |   "id": $fakeId,
         |   "name": "sesame",
         |   "purpose": "to do something cool",
         |   "system_name": "blahblah",
         |   "compliance": {
         |     "pii_data": false,
         |     "phi_data": false,
         |     "pci_data": false
         |   },
         |   "hdfs": {
         |     "requested_size_in_gb": 0.2
         |   },
         |   "yarn": {
         |     "max_cores": 1,
         |     "max_memory_in_gb": 0.2
         |   },
         |   "created": "$oldDateString",
         |   "created_by": "$wrongUser"
         | }""".stripMargin)

    val workspaceService = mock[WorkspaceService]
    (workspaceService.create _).expects(*).returning(Future(TestProject()))
    val authService = mock[AuthService]
    (authService.validateToken _).expects(*).returning(Future(Some(User("", ""))))
    val restApi = new WorkspaceController(authService, workspaceService)

    Post("/workspaces", json) ~> addCredentials(OAuth2BearerToken("AbCdEf123456")) ~> restApi.route ~> check {
      status should be(StatusCodes.Created)

      val result = responseAs[SharedWorkspace]
      result.id should not be fakeId
      result.created should not be oldDate
      result.createdBy should not be wrongUser
      result.systemName should not be "blahblah"
    }
  }

  it should "list all projects" in {
    val projects@Seq(project1) = Seq(
      TestProject(id = 321L, name = "Project 2")
    )

    val workspaceService = mock[WorkspaceService]
    (workspaceService.list _).expects(*).returning(Future(projects))
    val authService = mock[AuthService]
    (authService.validateToken _).expects(*).returning(Future(Some(User("", standardUsername))))
    val restApi = new WorkspaceController(authService, workspaceService)

    Get("/workspaces") ~> addCredentials(OAuth2BearerToken("AbCdEf123456")) ~> restApi.route ~> check {
      status should be(StatusCodes.OK)
      val result = responseAs[Seq[SharedWorkspace]]
      result.head.id should be(project1.id)
    }
  }
}