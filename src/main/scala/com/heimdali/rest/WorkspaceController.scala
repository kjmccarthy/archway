package com.heimdali.rest

import java.time.{ Clock, Instant }

import cats.effect._
import com.heimdali.models._
import com.heimdali.repositories.DatabaseRole
import com.heimdali.services._
import com.heimdali.tasks.ProvisionTask._
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.dsl.io._

class WorkspaceController(authService: AuthService[IO],
                          workspaceService: WorkspaceService[IO],
                          memberService: MemberService[IO],
                          kafkaService: KafkaService[IO],
                          clock: Clock) {

  implicit val memberRequestEntityDecoder: EntityDecoder[IO, MemberRequest] = jsonOf[IO, MemberRequest]

  val route: HttpService[IO] =
    authService.tokenAuth {
      AuthedService[User, IO] {
        case req@POST -> Root / LongVar(id) / "approve" as user =>
          if(user.canApprove) {
            implicit val decoder: Decoder[Approval] = Approval.decoder(user, clock)
            implicit val approvalEntityDecoder: EntityDecoder[IO, Approval] = jsonOf[IO, Approval]
            for {
              approval <- req.req.as[Approval]
              approved <- workspaceService.approve(id, approval)
              response <- Created(approved.asJson)
            } yield response
          }
          else
            Forbidden()

        case POST -> Root / LongVar(id) / "provision" as user =>
          if(user.isSuperUser) {
            for {
              workspace <- workspaceService.find(id).value
              _ <- workspaceService.provision(workspace.get)
              response <- Created()
            } yield response
          }
          else
            Forbidden()

        case req@POST -> Root as user =>
          /* explicit implicit declaration because of `user` variable */
          implicit val decoder: Decoder[WorkspaceRequest] = WorkspaceRequest.decoder(user, clock)
          implicit val workspaceRequestEntityDecoder: EntityDecoder[IO, WorkspaceRequest] = jsonOf[IO, WorkspaceRequest]

          for {
            workspaceRequest <- req.req.as[WorkspaceRequest]
            newWorkspace <- workspaceService.create(workspaceRequest)
            response <- Created(newWorkspace.asJson)
          } yield response

        case GET -> Root as user =>
          for {
            workspaces <- workspaceService.list(user.username)
            response <- Ok(workspaces.asJson)
          } yield response

        case GET -> Root / LongVar(id) as _ =>
          for {
            maybeWorkspace <- workspaceService.find(id).value
            response <- maybeWorkspace.fold(NotFound())(workspace => Ok(workspace.asJson))
          } yield response

        case GET -> Root / LongVar(id) / database / DatabaseRole(role) as _ =>
          for {
            members <- memberService.members(id, database, role)
            response <- Ok(members.asJson)
          } yield response

        case req@POST -> Root / LongVar(id) / database / DatabaseRole(role) as _ =>
          for {
            memberRequest <- req.req.as[MemberRequest]
            newMember <- memberService.addMember(id, database, role, memberRequest.username).value
            response <- newMember.fold(NotFound())(member => Created(member.asJson))
          } yield response

        case DELETE -> Root / LongVar(id) / database / DatabaseRole(role) / username as _ =>
          for {
            removedMember <- memberService.removeMember(id, database, role, username).value
            response <- removedMember.fold(NotFound())(member => Ok(member.asJson))
          } yield response

        case req@POST -> Root / LongVar(id) / "database" / LongVar(databaseId) / "topics" as user =>
          implicit val kafkaTopicDecoder: EntityDecoder[IO, TopicRequest] = jsonOf[IO, TopicRequest]
          for {
            topic <- req.req.as[TopicRequest]
            result <- kafkaService.create(user.username, id, databaseId, topic)
            response <- Ok(result.asJson)
          } yield response
      }
    }

}
