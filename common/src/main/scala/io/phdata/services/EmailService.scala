package io.phdata.services

import java.net.InetAddress

import cats.data._
import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.phdata.AppContext
import io.phdata.clients.LDAPUser
import io.phdata.models.{DistinguishedName, MemberRoleRequest, WorkspaceRequest}
import org.fusesource.scalate.TemplateEngine

trait EmailService[F[_]] {

  def newMemberEmail(workspaceId: Long, memberRoleRequest: MemberRoleRequest): OptionT[F, Unit]

  def newWorkspaceEmail(workspaceRequest: WorkspaceRequest): F[Unit]
}

class EmailServiceImpl[F[_]: Effect](context: AppContext[F], workspaceService: WorkspaceService[F])
    extends EmailService[F] with LazyLogging {

  lazy val templateEngine: TemplateEngine = new TemplateEngine()

  override def newMemberEmail(workspaceId: Long, memberRoleRequest: MemberRoleRequest): OptionT[F, Unit] = {

    for {
      workspace <- workspaceService.findById(workspaceId)
      fromAddress = context.appConfig.smtp.fromEmail
      to <- context.lookupLDAPClient.findUserByDN(memberRoleRequest.distinguishedName)
      toAddress <- OptionT(Effect[F].pure(to.email))
      owner <- context.lookupLDAPClient.findUserByDN(workspace.requestedBy)
      values = Map(
        "roleName" -> memberRoleRequest.role.get.show,
        "resourceType" -> memberRoleRequest.resource,
        "workspaceName" -> workspace.name,
        "uiUrl" -> resolveUiUrl,
        "workspaceId" -> workspaceId,
        "ownerName" -> owner.name,
        "ownerEmail" -> {
            if (owner.email.isDefined) {
              owner.email.get
            } else {
              logger.error("Owner's email is not provided")
              "[Owner's email is not provided]"
            }
          }
      )
      email <- OptionT.liftF(Effect[F].delay(templateEngine.layout("/templates/emails/welcome.mustache", values)))
      _ <- OptionT.some[F](logger.debug(s"Sending email: $email"))
      result <- OptionT.liftF(
        context.emailClient
          .send(
            s"Archway Workspace: Welcome to ${workspace.name}",
            EmbeddedImageEmail.create(
              email,
              List(("public/images/logo_big.png", "logo"), ("public/images/check_mark.png", "checkMark"))
            ),
            fromAddress,
            toAddress
          )
          .onError {
            case e: Throwable =>
              logger
                .error(
                  s"Failed to send an email with subject 'Archway Workspace: Welcome to ${workspace.name}', " +
                      s"from address $fromAddress to address $toAddress, ${e.getLocalizedMessage}",
                  e
                )
                .pure[F]
          }
      )
    } yield result
  }

  override def newWorkspaceEmail(workspaceRequest: WorkspaceRequest): F[Unit] = {
    val values = Map(
      "uiUrl" -> resolveUiUrl,
      "workspaceId" -> workspaceRequest.id.get,
      "workspaceName" -> workspaceRequest.name
    )

    for {
      user <- context.lookupLDAPClient.findUserByDN(workspaceRequest.requestedBy).value
      email <- Effect[F].delay(
        templateEngine.layout(
          "/templates/emails/incoming.mustache",
          values + ("userName" -> s"${user.getOrElse(LDAPUser("Unknown", "Unknown", DistinguishedName("cn=Unknown"), Seq.empty, None)).name}")
        )
      )
      _ <- logger.debug(s"Sending email: $email").pure[F]
      addressList = context.appConfig.approvers.notificationEmail
      fromAddress = context.appConfig.smtp.fromEmail
    } yield {
      addressList.map(
        recipient =>
          context.emailClient
            .send(
              "A New Workspace is Waiting",
              EmbeddedImageEmail.create(email, List(("images/logo_big.png", "logo"))),
              fromAddress,
              recipient
            )
            .onError {
              case e: Throwable =>
                logger
                  .error(
                    s"Failed to send an email with subject 'A New Workspace is Waiting', " +
                        s"from address $fromAddress to address $recipient, ${e.getLocalizedMessage}",
                    e
                  )
                  .pure[F]
            }
      )
    }
  }

  private def resolveUiUrl = {
    if (context.appConfig.ui.url.isEmpty) {
      s"https://${InetAddress.getLocalHost.getCanonicalHostName}:${context.appConfig.rest.port}"
    } else {
      context.appConfig.ui.url
    }
  }
}
