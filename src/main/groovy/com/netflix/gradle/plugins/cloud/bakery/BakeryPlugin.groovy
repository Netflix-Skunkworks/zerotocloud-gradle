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
import com.netflix.gradle.plugins.cloud.bakery.tasks.PublishDebsTask
import org.gradle.api.Plugin
import org.gradle.api.Project


import static com.netflix.gradle.plugins.cloud.CloudPluginSupport.getExtension

class BakeryPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    BakeryExtension bakeryExtension = project.extensions.create('bakery', BakeryExtension)
    CloudExtension cloudExtension = getExtension(project, CloudExtension)
    bakeryExtension.cloudExtension = cloudExtension ? cloudExtension : new CloudExtension(project)

    project.task(type: BakeryInitializer, 'initBakery') {
      conventionMapping.bakeryService = { bakeryExtension.bakeryService }
    }

    project.configurations { ssh }
    project.repositories { jcenter() }
    project.dependencies {
      ssh 'org.apache.ant:ant-jsch:1.9.4', 'com.jcraft:jsch:0.1.51'
    }

    def publishDebsTask = project.task(type: PublishDebsTask, 'publishDebs') {
      conventionMapping.bakeryService = { bakeryExtension.getBakeryService() }
      conventionMapping.remoteUser = { bakeryExtension.remoteUser }
      conventionMapping.pubDir = { bakeryExtension.pubDir }
      conventionMapping.sshKey = { bakeryExtension.sshKey }
    }
    publishDebsTask.onlyIf {project.file("${project.buildDir}/distributions").exists()}
    publishDebsTask.outputs.upToDateWhen {false}
  }
}
