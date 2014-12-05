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



package com.netflix.gradle.plugins.cloud.asgard

import com.amazonaws.services.ec2.model.Instance
import com.netflix.gradle.plugins.cloud.CloudExtension
import com.netflix.gradle.plugins.cloud.asgard.tasks.AsgardIamRoleInitializer
import com.netflix.gradle.plugins.cloud.asgard.tasks.BakeAsgardTask
import com.netflix.gradle.plugins.cloud.asgard.tasks.DownloadAsgardTask
import com.netflix.gradle.plugins.cloud.asgard.tasks.InitAsgardConfigTask
import com.netflix.gradle.plugins.cloud.bakery.BakeryExtension
import com.netflix.gradle.plugins.cloud.eureka.EurekaExtension
import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.deb.DebPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project


import static com.netflix.gradle.plugins.cloud.CloudPluginSupport.getExtension

class AsgardPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    CloudExtension cloudExtension = getExtension(project, CloudExtension)
    EurekaExtension eurekaExtension = getExtension(project, EurekaExtension)
    BakeryExtension bakeryExtension = getExtension(project, BakeryExtension)
    AsgardExtension asgardExtension = project.extensions.create('asgard', AsgardExtension)
    asgardExtension.cloudExtension = cloudExtension ? cloudExtension : new CloudExtension(project)

    project.task(type: AsgardIamRoleInitializer, 'initAsgardIamRole') {
      conventionMapping.assumeRolePolicyDoc = { asgardExtension.assumeRolePolicyDoc }
      conventionMapping.iamPolicyDoc = { asgardExtension.iamPolicyDoc }
      conventionMapping.iamRole = { asgardExtension.iamRole }
      conventionMapping.instanceProfileName = { asgardExtension.instanceProfileName }
    }

    def downloadAsgardTask = project.task(type: DownloadAsgardTask, 'downloadAsgard')
    downloadAsgardTask.onlyIf {
      !(project.file("${project.buildDir}/asgard/ROOT.war").exists()) && !asgardExtension.asgardService.getResource()
    }

    def asgardOnlyIf = {
      !asgardExtension.asgardService.getResource()
    }

    if (eurekaExtension && eurekaExtension.getEurekaService().getResource()) {
      Instance eureka = eurekaExtension.getEurekaService().getResource()
      project.task(type: InitAsgardConfigTask, 'initAsgardConfig') {
        conventionMapping.accountId = { asgardExtension.accountId }
        conventionMapping.asgardPort = { asgardExtension.checkPort }
        conventionMapping.eurekaHost = { eureka.publicDnsName }
        conventionMapping.eurekaPort = { eurekaExtension.checkPort }
        conventionMapping.eurekaRegion = { eurekaExtension.region }
        conventionMapping.eurekaAvailabilityZones = { eurekaExtension.availabilityZones }
      }
    } else {
      project.task(type: InitAsgardConfigTask, 'initAsgardConfig') {
        conventionMapping.accountId = { asgardExtension.accountId }
        conventionMapping.asgardPort = { asgardExtension.checkPort }
      }
    }
    project.tasks.initAsgardConfig.onlyIf(asgardOnlyIf)

    def packageAsgardTask = project.task(type: Deb, dependsOn: ['downloadAsgard', 'initAsgardConfig'], 'packageAsgard') {
      packageName = "asgard"
      version = "1.5.1"

      from("${project.buildDir}/asgard") {
        into "/var/lib/tomcat7/webapps"
      }
      from("${project.projectDir}/gradle/asgard/deployEnv") {
        into "."
      }
      from("${project.buildDir}/deployEnv/asgard") {
        into "."
      }

      requires 'openjdk-7-jre'
      requires 'tomcat7'

      postInstall 'rm -rf /var/lib/tomcat7/webapps/ROOT && service tomcat7 restart'
    }
    packageAsgardTask.onlyIf(asgardOnlyIf)
    project.tasks.publishDebs.mustRunAfter(packageAsgardTask)

    if (bakeryExtension) {
      project.task(type: BakeAsgardTask, dependsOn: ['packageAsgard'], 'bakeAsgard') {
        conventionMapping.bakeryService = { bakeryExtension.bakeryService }
        conventionMapping.asgardService = { asgardExtension.asgardService }
        conventionMapping.pubDir = { bakeryExtension.pubDir }
      }
    } else {
      project.task('bakeAsgard') << {
        throw new RuntimeException("Bakery plugin must be applied for this to work!")
      }
    }
    project.tasks.bakeAsgard.onlyIf(asgardOnlyIf)

    project.task(dependsOn: 'bakeAsgard', 'initAsgard') << {
      asgardExtension.asgardService.create()
    }
    project.tasks.initAsgard.onlyIf(asgardOnlyIf)
  }

}
