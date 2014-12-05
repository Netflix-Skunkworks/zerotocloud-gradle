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



package com.netflix.gradle.plugins.cloud.asgard.services

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest

class AccountIdRetriever {
  final AmazonEC2 ec2

  AccountIdRetriever(AmazonEC2 ec2) {
    this.ec2 = ec2
  }

  String getAccountId() {
    def req = new DescribeSecurityGroupsRequest(groupNames: ['default'])
    def res = ec2.describeSecurityGroups(req)
    if (!res.securityGroups) {
      throw new RuntimeException("Could not retrieve account id!")
    }
    return res.securityGroups[0].ownerId
  }

}
