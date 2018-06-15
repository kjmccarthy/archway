package com.heimdali.services

import cats.effect.IO
import com.heimdali.clients.LDAPClient
import com.heimdali.config.{ApprovalConfig, RestConfig}
import com.heimdali.test.fixtures._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class AccountServiceSpec extends FlatSpec with MockFactory with Matchers {

  behavior of "Account Service"

  it should "return appropriate roles" in {
    val approvalConfig = ApprovalConfig("cn=foo,dc=jotunn,dc=io", "cn=bar,dc=jotunn,dc=io")
    val restConfig = RestConfig(1234, "abc")

    val accountService = new AccountServiceImpl[IO](mock[LDAPClient[IO]], restConfig, approvalConfig)

    val Right(user) = accountService.validate(infraApproverToken).value.unsafeRunSync()

    user.permissions.platformOperations should be(true)
  }

}
