/*
 * Copyright 2025 Marc Nuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marcnuri.plugins.gradle.api;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "gradle-api", defaultPhase = LifecyclePhase.INITIALIZE)
public class GradleApiMojo extends AbstractMojo {
  @Parameter(defaultValue = "${session}", readonly = true)
  MavenSession session;

  public final void execute() {
    final Plugin plugin = session.getCurrentProject().getPlugin("com.marcnuri.plugins:gradle-api-maven-plugin");
    if (!plugin.isExtensions()) {
      getLog().warn("Gradle API Maven Plugin should be configured with <extensions>true</extensions>");
    }
    // NO-OP: just check if extensions are enabled
  }
}
