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



package com.netflix.gradle.plugins.cloud.eureka

import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Client
import com.netflix.gradle.plugins.cloud.CloudExtension
import com.netflix.gradle.plugins.cloud.bakery.BakeryExtension
import com.netflix.gradle.plugins.deb.Deb
import org.gradle.api.Plugin
import org.gradle.api.Project


import static com.netflix.gradle.plugins.cloud.CloudPluginSupport.getExtension

class EurekaPlugin implements Plugin<Project> {
  static String EUREKA_COORDINATES

  static {
    EUREKA_COORDINATES = System.getProperty("eureka.coordinates", "com.netflix.eureka:eureka-server:1.1.145@war")
  }

  @Override
  void apply(Project project) {
    CloudExtension cloudExtension = getExtension(project, CloudExtension)
    BakeryExtension bakeryExtension = getExtension(project, BakeryExtension)
    EurekaExtension eurekaExtension = project.extensions.create('eureka', EurekaExtension)
    eurekaExtension.cloudExtension = cloudExtension ? cloudExtension : new CloudExtension(project)
    eurekaExtension.availabilityZones = getAvailabilityZones(eurekaExtension.region)

    project.configurations {
      eureka
    }

    project.dependencies {
      eureka EUREKA_COORDINATES
    }

    Closure eurekaOnlyIf = {
      eurekaExtension.eurekaService.getResource() == null
    }

    project.task('initEurekaDeps') << {
      def outFile = project.file("${project.buildDir}/eureka")
      outFile.mkdirs()
      def warFile = new File(outFile, "eureka.war")
      project.file(project.configurations.eureka.asPath).renameTo(warFile)
    }
    project.tasks.initEurekaDeps.onlyIf(eurekaOnlyIf)

    def packageEurekaTask = project.task(type: Deb, dependsOn: 'initEurekaDeps', 'packageEureka') {
      packageName = "eureka"
      version = "1.1.145"

      from("${project.buildDir}/eureka") {
        into "/var/lib/tomcat7/webapps"
      }
      from("${project.projectDir}/gradle/eureka/deployEnv") {
        into "."
      }

      requires 'openjdk-7-jdk'
      requires 'tomcat7'

      preInstall 'locale-gen en_US en_US.UTF-8 cy_GB.UTF-8'
      postInstall 'rm -rf /var/lib/tomcat7/webapps/ROOT && cd /var/lib/tomcat7/webapps && mkdir eureka && mv eureka.war eureka/ && cd eureka/ && jar xvf eureka.war && rm -rf eureka.war && service tomcat7 restart'
    }
    packageEurekaTask.onlyIf(eurekaOnlyIf)

    if (bakeryExtension) {
      def bakeEurekaTask = project.task(dependsOn: ['packageEureka', 'publishDebs'], 'bakeEureka') << {
        def amiId = bakeryExtension.bakeryService.bake("${bakeryExtension.pubDir}/${project.tasks.packageEureka.archivePath.name}")
        eurekaExtension.eurekaService.lastAmi = amiId
      }
      bakeEurekaTask.onlyIf(eurekaOnlyIf)
    } else {
      project.task('bakeEureka') {
        throw new RuntimeException("Bakery plugin must be applied for this to work!")
      }
    }

    def initEurekaTask = project.task(dependsOn: 'bakeEureka', 'initEureka') << {
      eurekaExtension.eurekaService.create()
    }
    initEurekaTask.onlyIf(eurekaOnlyIf)
  }

  private static List<String> getAvailabilityZones(String region) {
    AmazonEC2 ec2 = new AmazonEC2Client()
    ec2.setRegion(Region.getRegion(Regions.fromName(region)))
    ec2.describeAvailabilityZones().availabilityZones*.zoneName
  }
}
