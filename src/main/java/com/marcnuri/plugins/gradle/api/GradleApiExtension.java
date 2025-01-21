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

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import javax.inject.Named;
import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.marcnuri.plugins.gradle.api.GradleApi.GRADLE_ALL_ARTIFACT_ID;
import static com.marcnuri.plugins.gradle.api.GradleApi.GRADLE_DISTRIBUTION_BASE_URL;
import static com.marcnuri.plugins.gradle.api.GradleApi.GRADLE_GROUP_ID;

@Named("gradle-api")
public class GradleApiExtension extends AbstractMavenLifecycleParticipant {

  @Override
  public void afterProjectsRead(MavenSession session) {
    final GradleApiLog log = new GradleApiLog(session.getRequest().getLoggingLevel());
    final Map<MavenProject, Dependency> projects = new HashMap<>();
    for (MavenProject project : session.getProjects()) {
      project.getDependencies().stream()
        .filter(d -> d.getGroupId().equals(GRADLE_GROUP_ID))
        .filter(d -> d.getArtifactId().equals(GRADLE_ALL_ARTIFACT_ID))
        .findAny().ifPresent(d -> projects.put(project, d));
    }
    final Map<String, List<String>> artifactsForVersion = projects.values().stream()
      .map(Dependency::getVersion)
      .collect(Collectors.toMap(Function.identity(), v -> new ArrayList<>(), (a, b) -> a));
    for (Map.Entry<String, List<String>> artifactForVersion : artifactsForVersion.entrySet()) {
      artifactForVersion.getValue().addAll(new GradleApi(
        log,
        selectProxy(session.getRequest()),
        session.getRequest().isUpdateSnapshots(),
        artifactForVersion.getKey(),
        new File(session.getLocalRepository().getBasedir()).toPath()
      ).call());
    }
    for (Map.Entry<MavenProject, Dependency> project : projects.entrySet()) {
      final Dependency dependency = project.getValue();
      project.getKey().getDependencies().remove(dependency);
      for (String artifactId : artifactsForVersion.get(dependency.getVersion())) {
        final Dependency newDependency = new Dependency();
        newDependency.setGroupId(GRADLE_GROUP_ID);
        newDependency.setArtifactId(artifactId);
        newDependency.setVersion(dependency.getVersion());
        newDependency.setScope(dependency.getScope());
        project.getKey().getDependencies().add(newDependency);
      }
    }
  }

  private static Proxy selectProxy(MavenExecutionRequest request) {
    try {
      final URL gradleBaseUrl = new URL(GRADLE_DISTRIBUTION_BASE_URL);
      // Proxy from properties
      final Optional<Proxy> propertiesProxy = ProxySelector.getDefault().select(gradleBaseUrl.toURI()).stream()
        .filter(p -> p.type() != Proxy.Type.DIRECT)
        .findAny();
      if (propertiesProxy.isPresent()) {
        if (System.getProperties().get(gradleBaseUrl.getProtocol() + ".proxyUser") != null &&
          System.getProperty(gradleBaseUrl.getProtocol() + ".proxyPassword") != null
        ) {
          setAuthenticator(
            System.getProperty(gradleBaseUrl.getProtocol() + ".proxyUser"),
            System.getProperty(gradleBaseUrl.getProtocol() + ".proxyPassword"));
        }
        return propertiesProxy.get();
      }
      // Proxy from Maven
      if (request.getProxies() != null && !request.getProxies().isEmpty()) {
        final Optional<Proxy> mavenProxy = request.getProxies().stream()
          .filter(p -> p.isActive() && p.getProtocol().equalsIgnoreCase(gradleBaseUrl.getProtocol()))
          .findAny()
          .map(p -> {
            if (p.getUsername() != null && p.getPassword() != null) {
              setAuthenticator(p.getUsername(), p.getPassword());
            }
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(p.getHost(), p.getPort()));
          });
        if (mavenProxy.isPresent()) {
          return mavenProxy.get();
        }
      }
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalStateException("Invalid Gradle base URL", e);
    }
    return null;
  }

  static void setAuthenticator(String userName, String password) {
    System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(userName, password.toCharArray());
      }
    });
  }
}
