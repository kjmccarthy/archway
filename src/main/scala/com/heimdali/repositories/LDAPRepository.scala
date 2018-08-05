package com.heimdali.repositories

import cats.Show
import cats.implicits._
import cats.data.OptionT
import com.heimdali.models.LDAPRegistration
import doobie.free.connection.ConnectionIO
import doobie.util.composite.Composite
import doobie.util.meta.Meta

trait LDAPRepository {

  def create(lDAPRegistration: LDAPRegistration): ConnectionIO[LDAPRegistration]

  def complete(id: Long): ConnectionIO[LDAPRegistration]

  def find(resource: String, resourceId: Long, role: String): OptionT[ConnectionIO, LDAPRegistration]

  def groupCreated(id: Long): ConnectionIO[Int]

  def roleCreated(id: Long): ConnectionIO[Int]

  def groupAssociated(id: Long): ConnectionIO[Int]

}

sealed trait DatabaseRole

object DatabaseRole {

  implicit val viewer: Show[DatabaseRole] =
    Show.show {
      case Manager => "manager"
      case ReadOnly => "readonly"
    }

  def unapply(role: String): Option[DatabaseRole] =
    role match {
      case _ if role matches "managers?" => Some(Manager)
      case "readonly" => Some(ReadOnly)
      case _ => None
    }

  implicit val composite: Meta[DatabaseRole] = Meta[String].xmap(unapply(_).get, _.show)
}

case object Manager extends DatabaseRole

case object ReadOnly extends DatabaseRole