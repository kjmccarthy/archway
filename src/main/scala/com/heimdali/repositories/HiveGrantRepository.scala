package com.heimdali.repositories

import com.heimdali.models.HiveGrant
import doobie._

trait HiveGrantRepository {

  def create(ldapRegistrationId: Long): ConnectionIO[Long]

  def locationGranted(id: Long): ConnectionIO[Int]

  def databaseGranted(id: Long): ConnectionIO[Int]

}

