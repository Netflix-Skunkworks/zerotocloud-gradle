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



package com.netflix.gradle.plugins.cloud.asgard.model

import groovy.transform.ToString

@ToString
class Deploy {
  String appName
  String stack
  String region
  int min
  int desiredCapacity
  int max
  int healthCheckGracePeriod = 600
  String terminationPolicy = 'default'
  String subnetPurpose
  List<String> selectedZones = []
  String imageId
  String keyName
  List<String> selectedSecurityGroups
  String instanceType
  String iamInstanceProfile

  String getCluster() {
    "${appName}${stack?'-'+stack:''}".toString()
  }
}
