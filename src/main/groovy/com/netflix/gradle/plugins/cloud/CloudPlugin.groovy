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

import com.netflix.gradle.plugins.cloud.asgard.AsgardPlugin
import com.netflix.gradle.plugins.cloud.bakery.BakeryPlugin
import com.netflix.gradle.plugins.cloud.eureka.EurekaPlugin
import com.netflix.gradle.plugins.cloud.services.BaseIamRoleInitializer
import com.netflix.gradle.plugins.cloud.services.BaseSecurityGroupInitializer
import com.netflix.gradle.plugins.cloud.services.KeyPairInitializer
import com.netflix.gradle.plugins.deb.DebPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class CloudPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    CloudExtension cloudExtension = project.extensions.create('cloud', CloudExtension, project)

    project.task(type: KeyPairInitializer, 'initKeyPair') {
      conventionMapping.ec2 = { cloudExtension.ec2 }
      conventionMapping.keyPairName = { cloudExtension.keyPairName }
    }

    project.task(type: BaseIamRoleInitializer, 'initBaseIamRole') {
      conventionMapping.baseIamRole = { cloudExtension.iamRole }
      conventionMapping.baseInstanceProfileName = { cloudExtension.instanceProfileName}
      conventionMapping.assumeRolePolicyDoc = { cloudExtension.assumeRolePolicyDoc }
      conventionMapping.baseIamPolicyDoc = { cloudExtension.iamPolicyDoc }
    }

    project.task(type: BaseSecurityGroupInitializer, 'initBaseSecurityGroup') {
      conventionMapping.ec2 = { cloudExtension.ec2 }
      conventionMapping.securityGroup = { cloudExtension.baseSecurityGroup }
    }

    project.plugins.apply(DebPlugin)
    project.plugins.apply(BakeryPlugin)
    project.plugins.apply(EurekaPlugin)
    project.plugins.apply(AsgardPlugin)


    def orderedTasks = [['initKeyPair', 'initBaseIamRole', 'initAsgardIamRole', 'initBaseSecurityGroup'],
                        ['initBakery'], ['initEureka'], ['initAsgard']].collect { it.collect { project.tasks[it] } }
    (orderedTasks.size() - 1).downto(0) {
      orderedTasks[it]*.mustRunAfter orderedTasks.subList(0, it)
    }

    project.task(dependsOn: orderedTasks, 'initCloud')
  }
}
