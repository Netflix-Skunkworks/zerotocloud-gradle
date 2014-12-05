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



package com.netflix.gradle.plugins.cloud.bakery

import com.netflix.gradle.plugins.cloud.CloudExtension
import com.netflix.gradle.plugins.cloud.model.DeploymentExtension
import com.netflix.gradle.plugins.cloud.model.Extension
import com.netflix.gradle.plugins.cloud.bakery.services.BakeryService

class BakeryExtension implements DeploymentExtension {
  @Delegate
  CloudExtension cloudExtension

  String name = "bakery"
  String instanceType = "m3.medium"
  String securityGroup = "bakery"
  int checkPort = 22
  String bakeryEnvironment = "ec2_aptitude_linux"
  String pubDir = "/repo"

  private BakeryService bakeryService

  public BakeryService getBakeryService() {
    if (!this.bakeryService) {
      this.bakeryService = new BakeryService(this)
    }
    this.bakeryService
  }
}
