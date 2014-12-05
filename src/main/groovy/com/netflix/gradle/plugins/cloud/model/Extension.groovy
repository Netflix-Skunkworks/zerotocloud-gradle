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



package com.netflix.gradle.plugins.cloud.model

import com.amazonaws.services.ec2.AmazonEC2
import org.gradle.api.Project

interface Extension {
  Project getProject()
  AmazonEC2 getEc2()

  String getIamRole()// = "BaseIAMRole"
  String getInstanceProfileName()// = "BaseIAMRole_InstanceProfile"
  String getKeyPairName()// = "nf-oss"
  String getAssumeRolePolicyDoc()// = getClass().getResourceAsStream("/assumeRolePolicy.json").text
  String getIamPolicyDoc()// = getClass().getResourceAsStream("/baseIamPolicy.json").text
  String getRegion()// = "us-east-1"
  String getRemoteUser()// = "ubuntu"
  String getBaseSecurityGroup()// = "base"
  String getBaseAmiArch()// = "amd64"
  String getBaseAmiHypervisor()// = "xen"
  String getBaseAmiOwner()// = "099720109477" /* Canonical */
  String getBaseAmiName()// = "ubuntu/images/ebs/ubuntu-trusty-14.04-amd64-server" /* We'll lookup the latest one... */
  Boolean getBaseAmiIsPublic()// = true

  File getSshKey()// = "~/.ssh/nf-oss.pem"
}