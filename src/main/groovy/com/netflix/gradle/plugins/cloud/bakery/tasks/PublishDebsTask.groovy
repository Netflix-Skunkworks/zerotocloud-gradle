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



package com.netflix.gradle.plugins.cloud.bakery.tasks

import com.netflix.gradle.plugins.cloud.bakery.services.BakeryService
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PublishDebsTask extends DefaultTask {

  def BakeryService bakeryService
  def String remoteUser
  def String pubDir
  def File sshKey

  @TaskAction
  void init() {
    ant.taskdef(name: 'scp', classname: 'org.apache.tools.ant.taskdefs.optional.ssh.Scp',
        classpath: project.configurations.ssh.asPath)

    def bakery = getBakeryService().getResource()

    getBakeryService().with {
      exec "sudo mkdir -p ${getPubDir()} && sudo chmod 777 ${getPubDir()}"
    }

    println "using ${getSshKey().absolutePath}"

    ant.scp(todir: "${getRemoteUser()}@${bakery.publicDnsName}:${getPubDir()}",
        keyfile: getSshKey(),
        trust: 'true',
        verbose: 'true') {
      fileset(dir: "${project.buildDir}/distributions") {
        include(name: '**/*.deb')
      }
    }
  }
}
