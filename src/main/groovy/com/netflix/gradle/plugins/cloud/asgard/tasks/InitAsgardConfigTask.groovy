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



package com.netflix.gradle.plugins.cloud.asgard.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class InitAsgardConfigTask extends DefaultTask {
  def String accountId
  def Integer asgardPort
  def String eurekaHost
  def Integer eurekaPort
  def String eurekaRegion
  def List<String> eurekaAvailabilityZones

  @TaskAction
  void init() {
    def configDir = project.file("${project.buildDir}/deployEnv/asgard/usr/share/tomcat7/.asgard")
    configDir.mkdirs()
    def configFile = new File(configDir, "Config.groovy")
    if (configFile.exists()) {
      configFile.delete()
    }
    def config = ""
    if (getEurekaHost() && getEurekaPort() && getEurekaRegion() && getEurekaAvailabilityZones()) {
      config = getEurekaConfig()
    }
    config = """\
      |${config}
      |${getConfig()}
    """.stripMargin()
    configFile << config

  }

  private String getConfig() {
    """\
      |grails {
      |  awsAccounts=['${getAccountId()}']
      |  awsAccountNames=['${getAccountId()}':'prod']
      |}
      |secret {
      |  accessId=''
      |  secretKey=''
      |}
      |cloud {
      |  accountName='prod'
      |}
    """.stripMargin()
  }

  private String getEurekaConfig() {
    def asgardRegion = realRegionToAsgardRegionEnumString(getEurekaRegion())
    """\
      |import com.netflix.asgard.Region
      |eureka {
      |  port = '${getEurekaPort()}'
      |  defaultRegistrationUrl='http://${getEurekaHost()}:${getEurekaPort()}/eureka/v2/'
      |  urlTemplateForZoneRegionEnv='http://${getEurekaHost()}:${getEurekaPort()}/eureka/v2/'
      |  localVirtualHostName='asgard:${getAsgardPort()}'
      |  regionsToServers = [(${asgardRegion}): '${getEurekaHost()}']
      |  zoneListsByRegion = ['${getEurekaRegion()}': [${getEurekaAvailabilityZones().collect {"'${it}'"}.join(',')}]]
      |}
    """.stripMargin()
  }

  private static String realRegionToAsgardRegionEnumString(String region) {
    "Region.${region.toUpperCase().replaceAll('-','_')}"
  }
}
