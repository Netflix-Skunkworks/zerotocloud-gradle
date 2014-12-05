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
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.gradle.plugins.cloud.asgard.model.Deploy
import groovy.json.JsonSlurper
import retrofit.RestAdapter
import retrofit.RetrofitError
import retrofit.client.OkClient
import retrofit.client.Response
import retrofit.converter.JacksonConverter


import static retrofit.Endpoints.newFixedEndpoint

class Deployer {
  private static final JsonSlurper slurper = new JsonSlurper()
  private final AsgardClient asgardClient
  private final Deploy deploy
  private final AmazonEC2 ec2

  private Deployer(AsgardClient asgardClient, Deploy deploy, AmazonEC2 ec2) {
    this.asgardClient = asgardClient
    this.deploy = deploy
    this.ec2 = ec2
  }

  // we get no feedback as to the ASG created here, so we'll need to just trust, i guess.
  void deploy() {
    Response response
    try {
      response = asgardClient.getApp(deploy.appName)
    } catch (RetrofitError e) {
      if (e.response.status == 404) {
        // you can update all this later in the UI if you really care, just need the deployment to succeed at this point...
        asgardClient.createApp(deploy.appName, "", "Web Service", "Auto-Created During Continuous Delivery/Deployment",
            response = System.properties['user.name'], "${System.properties['user.name']}@domain.com", "none", "")
      } else {
        throw e
      }
    }

    def json = response.body.in().text
    def appMap = slurper.parseText(json) as Map
    if (appMap.clusters.contains(deploy.cluster)) {
      nextDeploy deploy
    } else {
      initialDeploy deploy
    }
  }

  private void nextDeploy(Deploy deploy) {
    // sigh...
    def res = ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupNames(deploy.selectedSecurityGroups))
    def groupIds = res.securityGroups*.groupId

    def payload = [
        asgOptions       : [
            suspendedProcesses    : [],
            tags                  : [],
            autoScalingGroupName  : null,
            subnetPurpose         : null,
            healthCheckType       : "EC2",
            desiredCapacity       : deploy.desiredCapacity,
            availabilityZones     : deploy.selectedZones,
            loadBalancerNames     : [],
            minSize               : deploy.min,
            healthCheckGracePeriod: deploy.healthCheckGracePeriod,
            defaultCooldown       : 10,
            maxSize               : deploy.max,
            terminationPolicies   : [deploy.terminationPolicy]
        ],
        lcOptions        : [
            securityGroups             : groupIds,
            kernelId                   : "",
            launchConfigurationName    : null,
            userData                   : null,
            instancePriceType          : "ON_DEMAND",
            instanceType               : deploy.instanceType,
            blockDeviceMappings        : [],
            imageId                    : deploy.imageId,
            keyName                    : deploy.keyName,
            ramdiskId                  : "",
            instanceMonitoringIsEnabled: false,
            iamInstanceProfile         : deploy.iamInstanceProfile,
            ebsOptimized               : false
        ],
        deploymentOptions: [
            clusterName            : deploy.cluster,
            notificationDestination: "null@null.nl",
            steps                  : [[
                                          type: "CreateAsg"
                                      ], [
                                          type                 : "Resize",
                                          capacity             : 1,
                                          startUpTimeoutMinutes: 31
                                      ], [
                                          type     : "DisableAsg",
                                          targetAsg: "Previous"
                                      ]]
        ]
    ]

    asgardClient.deployNext(deploy.region, payload)
  }

  private void initialDeploy(Deploy deploy) {
    asgardClient.deploy(deploy.region, deploy.region, deploy.appName, deploy.stack, deploy.min,
        deploy.desiredCapacity, deploy.max, deploy.healthCheckGracePeriod, deploy.terminationPolicy,
        deploy.subnetPurpose, deploy.selectedZones, deploy.imageId, deploy.keyName, deploy.selectedSecurityGroups,
        deploy.instanceType, deploy.iamInstanceProfile)
  }

  static Builder builder(String asgardUrl, AmazonEC2 ec2) {
    def asgardClient = new RestAdapter.Builder()
        .setEndpoint(newFixedEndpoint(asgardUrl))
        .setClient(new OkClient())
        .setLogLevel(RestAdapter.LogLevel.FULL)
        .setConverter(new JacksonConverter(new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)))
        .setClient(new OkClient())
        .setLogLevel(RestAdapter.LogLevel.FULL)
        .build()
        .create(AsgardClient)
    new Builder(asgardClient, ec2)
  }

  static class Builder {
    private final Deploy deploy = new Deploy()
    private final AsgardClient asgardClient
    private final AmazonEC2 ec2

    Builder(AsgardClient asgardClient, AmazonEC2 ec2) {
      this.asgardClient = asgardClient
      this.ec2 = ec2
    }

    Builder app(String app) {
      deploy.@appName = app
      this
    }

    Builder stack(String stack) {
      deploy.@stack = stack
      this
    }

    Builder region(String region) {
      deploy.@region = region
      this
    }

    Builder imageId(String imageId) {
      deploy.@imageId = imageId
      this
    }

    Builder capacity(int min, desired, max) {
      deploy.@min = min
      deploy.@desiredCapacity = desired
      deploy.@max = max
      this
    }

    Builder healthCheckGracePeriod(int period) {
      deploy.@healthCheckGracePeriod = period
      this
    }

    Builder terminationPolicy(String terminationPolicy) {
      deploy.@terminationPolicy = terminationPolicy
      this
    }

    Builder subnetPurpose(String purpose) {
      deploy.@subnetPurpose = purpose
      this
    }

    Builder zones(List<String> zones) {
      deploy.@selectedZones = zones
      this
    }

    Builder keyName(String keyName) {
      deploy.@keyName = keyName
      this
    }

    Builder securityGroups(List<String> groups) {
      deploy.@selectedSecurityGroups = groups
      this
    }

    Builder instanceType(String instanceType) {
      deploy.@instanceType = instanceType
      this
    }

    Builder instanceProfile(String profile) {
      deploy.@iamInstanceProfile = profile
      this
    }

    Deployer build() {
      new Deployer(asgardClient, deploy, ec2)
    }
  }
}
