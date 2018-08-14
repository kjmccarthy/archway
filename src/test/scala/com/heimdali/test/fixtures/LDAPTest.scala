package com.heimdali.test.fixtures

import com.typesafe.config.ConfigFactory
import com.unboundid.ldap.sdk.{LDAPConnection, SearchScope}
import org.scalatest.{BeforeAndAfterEach, Suite}

import scala.collection.JavaConverters._
import scala.util.Try

trait LDAPTest extends BeforeAndAfterEach {
  this: Suite =>

  val baseDN = "dc=jotunn,dc=io"
  val userDN = "ou=users,ou=hadoop"
  val groupDN = "ou=groups,ou=hadoop"
  val bindDN = "cn=readonly,dc=jotunn,dc=io"
  val bindPassword = "readonly"
  val username = "username"
  val password = "password"

  val config = ConfigFactory.load()
  lazy val ldapConnection = new LDAPConnection(config.getString("ldap.server"), config.getInt("ldap.port"), config.getString("ldap.bindDN"), config.getString("ldap.bindPassword"))

  override protected def beforeEach(): Unit =
    try {
      ldapConnection.add(
        s"dn: cn=$username,$userDN,$baseDN",
        "objectClass: inetOrgPerson",
        "sn: Doe",
        "givenName: Dude",
        "userPassword: password")
      ldapConnection.add(
        s"dn: cn=edh_sw_sesame,$groupDN,$baseDN",
        "objectClass: group",
        "objectClass: top",
        "sAMAccountName: edh_sw_sesame",
        "cn: edh_sw_sesame"
      )
    } catch {
      case _: Throwable =>
    }

  override protected def afterEach(): Unit = {
    val users = ldapConnection.search("ou=users,ou=hadoop,dc=jotunn,dc=io", SearchScope.SUB, "(objectClass=inetOrgPerson)").getSearchEntries.asScala
    val groups = ldapConnection.search("ou=groups,ou=hadoop,dc=jotunn,dc=io", SearchScope.SUB, "(objectClass=group)").getSearchEntries.asScala
    (users ++ groups).map(_.getDN).map(ldapConnection.delete)
  }
}