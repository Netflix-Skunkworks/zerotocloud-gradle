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



package com.netflix.gradle.plugins.cloud.bakery.services

import com.netflix.gradle.plugins.cloud.services.AbstractResourceService
import com.netflix.gradle.plugins.cloud.bakery.BakeryExtension

class BakeryService extends AbstractResourceService<BakeryExtension> {
  static AMI_LOG_PATTERN = /.*\[INFO\] id\: (.*)/

  final BakeryExtension ext

  BakeryService(BakeryExtension ext) {
    super(ext)
    this.ext = ext
  }

  @Override
  protected String getName() {
    ext.name
  }

  @Override
  protected String getInstanceType() {
    ext.instanceType
  }

  @Override
  protected String getSecurityGroup() {
    ext.securityGroup
  }

  @Override
  protected int getCheckPort() {
    ext.checkPort
  }

  String bake(String pkg) {
    recordingLogger.reset()
    def baseAmi = discoverBaseAmi()
    getResource()
    exec "sudo aminate -B ${baseAmi.imageId} -e ${ext.bakeryEnvironment} ${pkg}"
    def id = (recordingLogger.messages.find { it ==~ AMI_LOG_PATTERN } =~ AMI_LOG_PATTERN)[0][1]
    if (!id) {
      throw new RuntimeException("Could not extrapolate AMI ID from aminator logging output!")
    }
    id
  }
}
