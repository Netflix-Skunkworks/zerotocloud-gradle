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



package com.netflix.gradle.plugins.cloud.services

import com.aestasit.ssh.SshOptions
import com.aestasit.ssh.dsl.SshDslEngine
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest
import com.amazonaws.services.ec2.model.CreateTagsRequest
import com.amazonaws.services.ec2.model.DescribeImagesRequest
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import com.amazonaws.services.ec2.model.Filter
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification
import com.amazonaws.services.ec2.model.Image
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.IpPermission
import com.amazonaws.services.ec2.model.RunInstancesRequest
import com.amazonaws.services.ec2.model.Tag
import com.netflix.gradle.plugins.cloud.bakery.ssh.RecordingLogger
import com.netflix.gradle.plugins.cloud.model.Extension

abstract class AbstractResourceService<T extends Extension> {
  protected AmazonEC2 ec2
  protected Instance resource
  protected RecordingLogger recordingLogger
  protected SshDslEngine engine

  final T ext
  String lastAmi

  AbstractResourceService(T ext) {
    this.ext = ext
    this.ec2 = ext.ec2
    this.recordingLogger = new RecordingLogger()
    this.engine = new SshDslEngine(new SshOptions(verbose: true, logger: recordingLogger, defaultKeyFile: ext.sshKey))
  }

  protected abstract String getName()
  protected abstract String getInstanceType()
  protected abstract String getSecurityGroup()
  protected abstract int getCheckPort()

  Instance getResource() {
    if (!resource) {
      def req = new DescribeInstancesRequest()
      def res = ec2.describeInstances(req)
      Instance resourceInstance = null
      while (true) {
        for (reservation in res.reservations) {
          resourceInstance = reservation.instances.find {
            it.tags.find { it.key == "Name" && it.value == getName() } &&
                it.state.code == 16 /* running */
          }
          if (resourceInstance) {
            break
          }
        }
        if (resourceInstance) {
          println "Found the resource (${getName()}: ${resourceInstance.instanceId})"
          break
        }
        if (res.nextToken) {
          res = ec2.describeInstances(req.withNextToken(res.nextToken))
        } else {
          break
        }
      }
      resource = resourceInstance
    }
    resource
  }

  String getLastAmi() {
    if (!this.lastAmi) {
      this.lastAmi = discoverBaseAmi()?.imageId
    }
    this.lastAmi
  }

  void create() {
    configureSecurityGroup()
    def req = new RunInstancesRequest()
        .withInstanceType(getInstanceType())
        .withSecurityGroups(getSecurityGroup())
        .withKeyName(ext.getKeyPairName())
        .withImageId(getLastAmi())
        .withIamInstanceProfile(new IamInstanceProfileSpecification().withName(ext.getInstanceProfileName()))
        .withMinCount(1)
        .withMaxCount(1)
    def res = ec2.runInstances(req)
    Instance instance = res.reservation.instances[0]
    while (true) {
      try {
        def resourceInstance = instanceIsReady(instance.instanceId)
        if (resourceInstance) {
          println("${getName()} instance deployed!")
          this.resource = resourceInstance
          tag()
          break
        } else {
          println("Waiting for ${getName()}...")
          sleep 1000
        }
      } catch (e) {
        e.printStackTrace()
        sleep 1000
      }
    }
  }

  void exec(String command) {
    def connect = "${ext.getRemoteUser()}@${resource.publicDnsName}:22"
    engine.remoteSession(connect) {
      exec command: command
    }
  }

  protected void tag() {
    def req = new CreateTagsRequest([resource.instanceId], [new Tag("Name", getName())])
    ec2.createTags(req)
  }

  protected Instance instanceIsReady(String instanceId) {
    def result = null
    def req = new DescribeInstancesRequest(instanceIds: [instanceId])
    def res = ec2.describeInstances(req)
    Instance instance = res.reservations[0].instances[0]
    if (instance.state.code == 16 /* "running" */) {
      try {
        println "${getName()} instance is ready, checking for port ${getCheckPort()}"
        def sock = new Socket()
        sock.connect(new InetSocketAddress(instance.publicDnsName, getCheckPort()), 500)
        if (sock.connected) {
          println("${getName()} Instance is ready!")
          result = instance
        } else {
          println "Still waiting on ${getName()} instance..."
        }
      } catch (O_o) {
        O_o.printStackTrace()
      }
    }
    result
  }

  protected Image discoverBaseAmi() {
    def req = new DescribeImagesRequest(filters: [new Filter("name", ["${ext.getBaseAmiName()}*".toString()]),
                                                  new Filter("owner-id", [ext.getBaseAmiOwner()]),
                                                  new Filter("is-public", [ext.getBaseAmiIsPublic().toString()])])
    def images = ec2.describeImages(req).images.sort { a, b -> b.name <=> a.name }
    if (!images) {
      throw new RuntimeException("Could not resolve Base AMI!")
    } else {
      images[0]
    }
  }

  protected void configureSecurityGroup() {
    try {
      ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest(groupNames: [getSecurityGroup()]))
    } catch (AmazonServiceException e) {
      ec2.createSecurityGroup(new CreateSecurityGroupRequest(getSecurityGroup(), "${getName()} Security Group"))
    }
    try {
      ec2.authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest(getSecurityGroup(),
          [new IpPermission(fromPort: 22, toPort: 22, ipRanges: ['0.0.0.0/0'], ipProtocol: 'tcp')]))
    } catch (AmazonServiceException e) {
      println("Could not authorize SSH access to Bakery security group!")
      e.printStackTrace()
    }
  }
}
