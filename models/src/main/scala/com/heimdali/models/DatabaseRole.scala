package com.heimdali.models

import cats.Show
import doobie.util.Get

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

}

case object Manager extends DatabaseRole

case object ReadOnly extends DatabaseRole