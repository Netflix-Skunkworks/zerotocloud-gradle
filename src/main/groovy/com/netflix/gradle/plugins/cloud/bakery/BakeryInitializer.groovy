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

import com.netflix.gradle.plugins.cloud.bakery.services.BakeryService
import com.netflix.gradle.plugins.cloud.model.CloudInitializer
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class BakeryInitializer extends DefaultTask implements CloudInitializer {
  def BakeryService bakeryService

  @TaskAction
  void init() {
    if (!getBakeryService().resource) {
      create()
    }
    configureBakery()
  }

  void create() {
    getBakeryService().with {
      create()
    }
  }

  private void configureBakery() {
    getBakeryService().with {
      println "Updating apt repo"
      def aptGetUpdate = "sudo apt-get update"
      exec aptGetUpdate
      println "Installing pip on the target instance"
      def pythonPipInstall = "sudo apt-get install python-pip --force-yes --yes"
      try {
        // this fails 100% of the time the first try
        exec pythonPipInstall
      } catch (e) {
        exec aptGetUpdate
        exec pythonPipInstall
      }
      println "Installing git on the target instance"
      exec "sudo apt-get install git --force-yes --yes"
      println "Installing aminator..."
      exec "sudo pip install git+https://github.com/Netflix/aminator.git#egg=aminator"
    }
  }
}
