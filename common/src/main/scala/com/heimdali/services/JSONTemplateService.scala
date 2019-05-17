package com.heimdali.services

import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import com.heimdali.config.AppConfig
import com.heimdali.models.{Compliance, TemplateRequest, User, WorkspaceRequest}
import com.typesafe.scalalogging.LazyLogging
import io.circe.Printer
import org.fusesource.scalate.{TemplateEngine, TemplateSource}

class JSONTemplateService[F[_] : Effect : Clock](appConfig: AppConfig,
                                                 configService: ConfigService[F])
  extends TemplateService[F] with LazyLogging {

  val templateEngine = new TemplateEngine()

  override def defaults(user: User): F[TemplateRequest] =
    TemplateRequest(user.name, user.name, user.name, Compliance.empty, user.distinguishedName).pure[F]

  private[services] def generateJSON(template: TemplateRequest, templatePath: String, templateName: String): F[String] =
    Sync[F].delay {
      logger.info("Using template path {}", templatePath)
      templateEngine.layout(templatePath, Map(
        "templateName" -> templateName,
        "appConfig" -> appConfig,
        "nextGid" -> (() => configService.getAndSetNextGid.toIO.unsafeRunSync()),
        "template" -> template
      ))
    }

  override def workspaceFor(template: TemplateRequest, templateName: String): F[WorkspaceRequest] = {
    val templatePath = Paths.get(appConfig.templates.templateRoot, s"$templateName.ssp").toString
    logger.info("generating {} from {}", templateName, templatePath)
    for {
      workspaceText <- generateJSON(template, templatePath, templateName)
      _ <- logger.debug("generated this output with the {} template: {}", templateName, workspaceText).pure[F]
      time <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      Right(json) = io.circe.parser.parse(workspaceText)
      Right(result) = json.as[WorkspaceRequest](WorkspaceRequest.decoder(template.requester, Instant.ofEpochMilli(time)))
    } yield result
  }

}