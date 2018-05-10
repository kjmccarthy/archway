package com.heimdali.provisioning

import akka.actor.{ActorLogging, ActorRef, FSM, Props}
import com.heimdali.models.{LDAPRegistration, Workspace}
import com.heimdali.provisioning.HiveActor.{CreateDatabase, DatabaseCreated}
import com.typesafe.config.Config


object WorkspaceProvisioner {

  case object Start

  case object Started

  def props[A, T <: Workspace[A]](ldapActor: ActorRef, hdfsActor: ActorRef, hiveActor: ActorRef, saveActor: ActorRef, configuration: Config, sharedWorkspace: T) =
    Props(new WorkspaceProvisioner[A, T](ldapActor, hdfsActor, hiveActor, saveActor, configuration, sharedWorkspace))

}

class WorkspaceProvisioner[A, T <: Workspace[A]](ldapActor: ActorRef,
                                                 hdfsActor: ActorRef,
                                                 hiveActor: ActorRef,
                                                 saveActor: ActorRef,
                                                 configuration: Config,
                                                 workspace: T)
  extends FSM[WorkspaceState, T] with ActorLogging {

  import HDFSActor._
  import LDAPActor._
  import WorkspaceProvisioner._
  import WorkspaceSaver._

  startWith(Idle, workspace)

  when(Idle) {
    case Event(Start, _) =>
      ldapActor ! CreateGroup(workspace.groupName(configuration), workspace.initialMembers)
      hdfsActor ! CreateDirectory(workspace.dataDirectory(configuration), workspace.requestedDiskSize(configuration), workspace.onBehalfOf)
      goto(Provisioning) replying Started
  }

  when(Provisioning) {
    case Event(DirectoryCreated(_), _) =>
      hiveActor ! CreateDatabase(workspace.groupName(configuration), workspace.databaseName, workspace.role(configuration), workspace.dataDirectory(configuration))
      stay()

    case Event(DatabaseCreated(db), _) =>
      saveActor ! HiveUpdate[A](workspace.workspaceId, db.copy(sizeInGB = workspace.requestedDiskSize(configuration)))
      stay()

    case Event(LDAPGroupCreated(name, dn), _) =>
      saveActor ! LDAPUpdate[A](workspace.workspaceId, LDAPRegistration(None, dn, name))
      stay()
  }

  whenUnhandled {
    case Event(e, s) ⇒
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay()
  }

  //TODO: Clean up
}