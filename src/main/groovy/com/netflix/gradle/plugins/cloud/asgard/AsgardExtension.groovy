/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.netflix.gradle.plugins.cloud.asgard

import com.netflix.gradle.plugins.cloud.CloudExtension
import com.netflix.gradle.plugins.cloud.model.DeploymentExtension
import com.netflix.gradle.plugins.cloud.asgard.services.AccountIdRetriever
import com.netflix.gradle.plugins.cloud.asgard.services.AsgardService

class AsgardExtension implements DeploymentExtension {
  @Delegate(excludes = ['getIamPolicyDoc', 'getIamRole', 'getInstanceProfileName', 'getInstanceType', 'getSecurityGroup'])
  CloudExtension cloudExtension

  String iamPolicyDoc = getClass().getResourceAsStream("/fullPrivilegeIamPolicy.json").text
  String iamRole = "AsgardIAMRole"
  String instanceProfileName = "AsgardIAMRole_InstanceProfile"
  String instanceType = "m3.large"
  String securityGroup = "asgard"
  int checkPort = 8080
  AccountIdRetriever accountIdRetriever
  String accountId
  AsgardService asgardService

  String getAccountId() {
    if (!this.accountId) {
      this.accountId = getAccountIdRetriever().accountId
    }
    this.accountId
  }

  AccountIdRetriever getAccountIdRetriever() {
    if (!this.accountIdRetriever) {
      this.accountIdRetriever = new AccountIdRetriever(getEc2())
    }
    this.accountIdRetriever
  }

  AsgardService getAsgardService() {
    if (!this.asgardService) {
      this.asgardService = new AsgardService(this)
    }
    this.asgardService
  }

  @Override
  String getIamPolicyDoc() {
    this.iamPolicyDoc
  }

  @Override
  String getInstanceProfileName() {
    this.instanceProfileName
  }

  @Override
  String getIamRole() {
    this.iamRole
  }
}
