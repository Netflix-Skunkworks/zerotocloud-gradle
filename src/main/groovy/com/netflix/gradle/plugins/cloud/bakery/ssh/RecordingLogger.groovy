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



package com.netflix.gradle.plugins.cloud.bakery.ssh

import com.aestasit.ssh.log.Logger

class RecordingLogger implements Logger {
  List<String> messages = []

  def void info(String message) {
    messages << message
    println message
  }
  def void warn(String message) {
    messages << message
    println message
  }
  def void debug(String message) {
    messages << message
    println message
  }
  void reset() {
    messages = []
  }
}
