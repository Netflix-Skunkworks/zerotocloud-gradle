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

import com.netflix.gradle.plugins.cloud.services.AbstractHttpResourceService
import com.netflix.gradle.plugins.cloud.asgard.AsgardExtension

class AsgardService extends AbstractHttpResourceService {
  final AsgardExtension extension

  AsgardService(AsgardExtension extension) {
    super(extension)
    this.extension = extension
  }

  Deployer.Builder deployBuilder() {
    def hostname = getResource()?.publicDnsName
    if (!hostname) {
      throw new RuntimeException("Could not resolve Asgard hostname!")
    }
    def url = "http://${hostname}:8080"
    Deployer.builder(url, ec2).imageId(lastAmi)
  }

  @Override
  protected String getName() {
    "asgard"
  }

  @Override
  protected String getInstanceType() {
    extension.getInstanceType()
  }

  @Override
  protected String getSecurityGroup() {
    extension.getSecurityGroup()
  }
}

