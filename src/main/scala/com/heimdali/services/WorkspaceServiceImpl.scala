package com.heimdali.services

import java.util.concurrent.Executors

import cats.data._
import cats.effect._
import cats.implicits._
import com.heimdali.clients._
import com.heimdali.models._
import com.heimdali.repositories.{MemberRepository, _}
import com.heimdali.tasks.{AddMember, ProvisionResult}
import com.typesafe.scalalogging.LazyLogging
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.concurrent.ExecutionContext

class WorkspaceServiceImpl[F[_]](ldapClient: LDAPClient[F],
                                 yarnRepository: YarnRepository,
                                 hiveDatabaseRepository: HiveDatabaseRepository,
                                 ldapRepository: LDAPRepository,
                                 workspaceRepository: WorkspaceRequestRepository,
                                 complianceRepository: ComplianceRepository,
                                 approvalRepository: ApprovalRepository,
                                 transactor: Transactor[F],
                                 memberRepository: MemberRepository,
                                 topicRepository: KafkaTopicRepository,
                                 applicationRepository: ApplicationRepository,
                                 appConfig: AppContext[F]
                                )(implicit val F: Effect[F], val executionContext: ExecutionContext)
  extends WorkspaceService[F]
    with LazyLogging {

  private val provisionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  private val GroupExtractor = "CN=edh_sw_([A-z0-9_]+),OU=.*".r

  def sharedMemberships(user: LDAPUser): List[String] =
    user.memberships.flatMap {
      case GroupExtractor(name) =>
        logger.info("found shared workspace {}", name)
        Some(name)
      case _ => None
    }.toList

  override def find(id: Long): OptionT[F, WorkspaceRequest] =
    OptionT((for {
      workspace <- workspaceRepository.find(id)
      datas <- OptionT.liftF(hiveDatabaseRepository.findByWorkspace(id))
      yarns <- OptionT.liftF(yarnRepository.findByWorkspaceId(id))
      appr <- OptionT.liftF(approvalRepository.findByWorkspaceId(id))
      tops <- OptionT.liftF(topicRepository.findByWorkspaceId(id))
      apps <- OptionT.liftF(applicationRepository.findByWorkspaceId(id))
    } yield workspace.copy(data = datas, processing = yarns, approvals = appr, kafkaTopics = tops, applications = apps))
      .value
      .transact(transactor))

  override def list(username: String): F[List[WorkspaceRequest]] =
    workspaceRepository.list(username).transact(transactor)

  def create(workspace: WorkspaceRequest): F[WorkspaceRequest] =
    (for {
      compliance <- complianceRepository.create(workspace.compliance)
      updatedWorkspace = workspace.copy(compliance = compliance)
      newWorkspaceId <- workspaceRepository.create(updatedWorkspace)

      insertedHive <- workspace.data.traverse[ConnectionIO, HiveDatabase] {
        db =>
          for {
            managerLdap <- ldapRepository.create(db.managingGroup.ldapRegistration)
            managerId <- appConfig.databaseGrantRepository.create(managerLdap.id.get)
            manager = db.managingGroup.copy(id = Some(managerId), ldapRegistration = managerLdap)

            _ <- memberRepository.create(workspace.requestedBy, managerLdap.id.get)

            readonly <- db.readonlyGroup.map { group =>
              for {
                ldap <- ldapRepository.create(group.ldapRegistration)
                grant <- appConfig.databaseGrantRepository.create(ldap.id.get)
              } yield group.copy(id = Some(grant), ldapRegistration = ldap)
            }.sequence[ConnectionIO, HiveGrant]

            beforeCreate = db.copy(managingGroup = manager, readonlyGroup = readonly)
            newHiveId <- hiveDatabaseRepository.create(beforeCreate)
            _ <- workspaceRepository.linkHive(newWorkspaceId, newHiveId)
          } yield beforeCreate.copy(id = Some(newHiveId))
      }

      insertedYarn <- workspace.processing.traverse[ConnectionIO, Yarn] {
        yarn =>
          for {
            newYarnId <- yarnRepository.create(yarn)
            _ <- workspaceRepository.linkPool(newWorkspaceId, newYarnId)
          } yield yarn.copy(id = Some(newYarnId))
      }
    } yield updatedWorkspace.copy(id = Some(newWorkspaceId), data = insertedHive, processing = insertedYarn))
      .transact(transactor)

  override def approve(id: Long, approval: Approval): F[Approval] =
    for {
      approval <- OptionT.liftF(approvalRepository.create(id, approval).transact(transactor)).value
      workspace <- find(id).value
      _ <- if (workspace.get.approvals.lengthCompare(2) == 0) OptionT.liftF(fs2.async.fork(provision(workspace.get))).value else OptionT.none(F).value
    } yield approval.get

  def provision(workspace: WorkspaceRequest): F[NonEmptyList[String]] = {
    import com.heimdali.tasks.ProvisionTask._

    val combined: List[ReaderT[F, AppContext[F], ProvisionResult]] =
      for {
        datas <- workspace.data.map(_.provision)
        members <- workspace.data.map(d => AddMember(d.id.get, d.managingGroup.ldapRegistration.distinguishedName, workspace.requestedBy).provision)
        yarns <- workspace.processing.map(_.provision)
      } yield (datas, members, yarns).mapN(_ |+| _ |+| _)

    combined.sequence.map(_.combineAll).apply(appConfig).map(_.messages)
  }

}
