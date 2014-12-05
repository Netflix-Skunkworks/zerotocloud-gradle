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

class DownloadAsgardTask extends DefaultTask {
  @TaskAction
  void download() {
    project.file("${getProject().buildDir}/asgard").mkdirs()
    // Groovy is my fav :-)
    def warBytes = "https://github.com/Netflix/asgard/releases/download/1.5.1/asgard.war".toURL().bytes
    project.file("${getProject().buildDir}/asgard/ROOT.war") << warBytes
  }
}
