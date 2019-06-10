package com.heimdali.repositories

import com.heimdali.common.IntegrationTest
import com.heimdali.test.fixtures._
import doobie.scalatest.IOChecker
import org.scalatest.{FunSuite, Matchers}

class KafkaTopicRepositoryImplIntegrationSpec
  extends FunSuite
    with Matchers
    with DBTest
    with IOChecker
    with IntegrationTest {

  val repo = new KafkaTopicRepositoryImpl()

  test("insert") { check(repo.Statements.create(initialTopic)) }
  test("list") { check(repo.Statements.findByWorkspaceId(id)) }
}
