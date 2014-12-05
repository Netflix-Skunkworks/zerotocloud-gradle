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



package com.netflix.gradle.plugins.cloud

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Client
import com.netflix.gradle.plugins.cloud.model.Extension
import groovy.transform.ToString
import org.gradle.api.Project

@ToString
class CloudExtension implements Extension {
  final Project project
  final AmazonEC2 ec2

  String iamRole = "BaseIAMRole"
  String instanceProfileName = "BaseIAMRole_InstanceProfile"
  String keyPairName = "nf-oss"
  String assumeRolePolicyDoc = getClass().getResourceAsStream("/assumeRolePolicy.json").text
  String iamPolicyDoc = getClass().getResourceAsStream("/baseIamPolicy.json").text
  String region = "us-east-1"
  String remoteUser = "ubuntu"
  String baseSecurityGroup = "base"
  String baseAmiArch = "amd64"
  String baseAmiHypervisor = "xen"
  String baseAmiOwner = "099720109477" /* Canonical */
  String baseAmiName = "ubuntu/images/ebs/ubuntu-trusty-14.04-amd64-server" /* We'll lookup the latest one... */
  Boolean baseAmiIsPublic = true

  File sshKey

  CloudExtension(Project project) {
    this.project = project
    this.ec2 = new AmazonEC2Client()
  }

  File getSshKey() {
    if (!this.sshKey) {
      File sshDir = new File(System.getProperty('user.home'), '.ssh')
      this.sshKey = new File(sshDir, "${keyPairName}.pem")
    }
    this.sshKey
  }
}
