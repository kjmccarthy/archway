package com.heimdali.controller

import com.heimdali.services._
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ClusterControllerSpec
  extends FlatSpec
    with Matchers
    with GuiceOneAppPerSuite {

  behavior of "Configuration Controller"

  it should "get a list of clusters" in {
    val request = FakeRequest(GET, "/clusters").withHeaders(AUTHORIZATION -> "Bearer ABC")
    val response = route(app, request).get

    status(response) should be(OK)

    val json = contentAsJson(response)

    json shouldBe a[JsArray]
    val items = json.as[JsArray].value

    items.size should be(1)
    val cluster = items.head

    (cluster \ "name").as[String] should be (FakeClusterService.odin.name)
    (cluster \ "id").as[String] should be (FakeClusterService.odin.id)
    (cluster \ "distribution" \ "name").as[String] should be (FakeClusterService.odin.distribution.getClass.getSimpleName)
    (cluster \ "distribution" \ "version").as[String] should be (FakeClusterService.odin.distribution.version)
  }

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(bind[ClusterService].to[FakeClusterService])
      .overrides(bind[AccountService].to[PassiveAccountService])
      .build()

}