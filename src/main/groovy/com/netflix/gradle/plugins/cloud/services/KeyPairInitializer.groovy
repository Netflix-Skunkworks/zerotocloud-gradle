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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.CreateKeyPairRequest
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class KeyPairInitializer extends DefaultTask {
  def AmazonEC2 ec2
  def String keyPairName

  @TaskAction
  void init() {
    try {
      getEc2().describeKeyPairs(new DescribeKeyPairsRequest(keyNames: [getKeyPairName()]))
    } catch (AmazonServiceException e) {
      println("KeyPair ${getKeyPairName()} not found...")
      try {
        create()
      } catch (e1) {
        ec2.deleteKeyPair(new DeleteKeyPairRequest(getKeyPairName()))
        throw e1
      }
    }
  }

  void create() {
    println("Creating KeyPair with name: ${getKeyPairName()}")
    def result = getEc2().createKeyPair(new CreateKeyPairRequest(keyName: getKeyPairName()))
    def pem = result.keyPair.keyMaterial
    def pemFile = new File("${System.properties['user.home']}/.ssh/${getKeyPairName()}.pem")
    if (pemFile.exists()) {
      pemFile.delete()
    }
    println("KeyPair created! Writing to ${pemFile.canonicalPath}")
    pemFile << pem
    println("Adding PEM file to SSH config")
    def process = "chmod 600 ${pemFile.canonicalPath}".execute()
    if (process.exitValue() > 0) {
      throw new RuntimeException("Failed to save PEM file")
    }
  }
}
