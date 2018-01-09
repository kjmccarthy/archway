package com.heimdali.modules

import com.heimdali.rest._

trait RestModule {
  this: AkkaModule with ExecutionContextModule with ServiceModule with HttpModule with ConfigurationModule =>

  val authService: AuthServiceImpl = new AuthServiceImpl(accountService)
  val accountController = new AccountController(authService)
  val clusterController = new ClusterController(clusterService)
  val workspaceController = new WorkspaceController(authService, workspaceService)

  val restAPI = new RestAPI(http, configuration, accountController, clusterController, workspaceController)
}