package com.heimdali.models

import java.time.{Clock, Instant}

import cats.implicits._
import cats.data.Kleisli
import cats.effect.Effect
import io.circe._
import io.circe.java8.time._
import io.circe.syntax._
import doobie._
import doobie.implicits._

case class WorkspaceRequest(name: String,
                            requestedBy: String,
                            requestDate: Instant,
                            compliance: Compliance,
                            singleUser: Boolean,
                            id: Option[Long] = None,
                            approvals: List[Approval] = List.empty,
                            data: List[HiveDatabase] = List.empty,
                            processing: List[Yarn] = List.empty,
                            applications: List[Application] = List.empty,
                            kafkaTopics: List[KafkaTopic] = List.empty)

object WorkspaceRequest {

  implicit val workspaceRequestComposite: Composite[WorkspaceRequest] =
    Composite[(String, String, Instant, Boolean, Boolean, Boolean, Option[Long], Boolean, Option[Long])].imap(
      (t: (String, String, Instant, Boolean, Boolean, Boolean, Option[Long], Boolean, Option[Long])) =>
        WorkspaceRequest(t._1, t._2, t._3, Compliance(t._4, t._5, t._6, t._7), t._8, t._9))(
      (w: WorkspaceRequest) => (w.name, w.requestedBy, w.requestDate, w.compliance.phiData, w.compliance.pciData, w.compliance.piiData, w.compliance.id, w.singleUser, w.id)
    )

  def insert(workspaceRequest: WorkspaceRequest): Update0 =
    sql"""
          insert into workspace_request (
            name,
            compliance_id,
            requested_by,
            request_date,
            single_user
          )
          values (
            ${workspaceRequest.name},
            ${workspaceRequest.compliance.id},
            ${workspaceRequest.requestedBy},
            ${workspaceRequest.requestDate},
            ${workspaceRequest.singleUser}
          )
      """.update

  implicit val savable: Savable[WorkspaceRequest] = new Savable[WorkspaceRequest] {
    override def save[F[_]](workspace: WorkspaceRequest)(implicit F: Effect[F]): Kleisli[F, AppContext[F], Unit] =
      Kleisli[F, AppContext[F], Unit] { config =>
        insert(workspace).run.transact(config.transactor).void
      }
  }

  implicit val encoder: Encoder[WorkspaceRequest] = Encoder.instance { request =>
    request.approvals.foldLeft(
      Json.obj(
        "id" -> request.id.asJson,
        "name" -> request.name.asJson,
        "compliance" -> request.compliance.asJson,
        "data" -> request.data.asJson,
        "processing" -> request.processing.asJson,
        "applications" -> request.applications.asJson,
        "kafka_topics" -> request.kafkaTopics.asJson,
        "single_user" -> request.singleUser.asJson,
        "requester" -> request.requestedBy.asJson,
        "requested_date" -> request.requestDate.asJson
      )
    )((initial, approvals) => initial deepMerge Json.obj("approvals" -> approvals.asJson))
  }

  implicit def decoder(user: User, clock: Clock): Decoder[WorkspaceRequest] = Decoder.instance { json =>
    for {
      name <- json.downField("name").as[String]
      compliance <- json.downField("compliance").as[Compliance]
      singleUser <- json.downField("single_user").as[Boolean]
      data <- json.downField("data").as[List[HiveDatabase]]
      processing <- json.downField("processing").as[List[Yarn]]
      applications <- json.downField("applications").as[List[Application]]
      topics <- json.downField("kafka_topics").as[List[KafkaTopic]]
    } yield WorkspaceRequest(name, user.username, Instant.now(clock), compliance, singleUser, data = data, processing = processing, applications = applications, kafkaTopics = topics)
  }

}
