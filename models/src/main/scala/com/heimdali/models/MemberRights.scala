package com.heimdali.models

import cats.Show
import cats.implicits._
import io.circe._
import io.circe.syntax._

case class MemberRights(name: String, id: Long, role: DatabaseRole)

object MemberRights {

  implicit val encoder: Encoder[MemberRights] =
    Encoder.instance { r =>
      Json.obj(
        "id" -> r.id.asJson,
        "role" -> r.role.show.asJson
      )
    }

}
