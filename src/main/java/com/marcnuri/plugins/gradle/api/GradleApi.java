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

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardOpenOption.WRITE;

public class GradleApi implements Callable<Collection<String>> {

  static final String GRADLE_GROUP_ID = "org.gradle";
  static final String GRADLE_ALL_ARTIFACT_ID = "gradle-all";
  static final String GRADLE_TOOLING_API_ARTIFACT_ID = "gradle-tooling-api";
  static final String GRADLE_DISTRIBUTION_BASE_URL = "https://services.gradle.org/distributions/";
  static final String GRADLE_LIB_RELEASES_BASE_URL = "https://repo.gradle.org/artifactory/libs-releases/";
  private final GradleApiLog log;
  private final Proxy proxy;
  private final boolean forceUpdate;
  private final String gradleVersion;
  private final Path repositoryBaseDir;
  private final Path gradleBinZip;

  public GradleApi(GradleApiLog log, Proxy proxy, boolean forceUpdate, String gradleVersion, Path repositoryBaseDir) {
    this.log = log;
    this.proxy = proxy;
    this.gradleVersion = gradleVersion;
    this.forceUpdate = forceUpdate;
    this.repositoryBaseDir = repositoryBaseDir;
    gradleBinZip = resolveGroupDir().resolve(GRADLE_ALL_ARTIFACT_ID).resolve(gradleVersion)
      .resolve("gradle-" + gradleVersion + "-bin.zip");
  }

  @Override
  public final Collection<String> call() {
    if (forceUpdate || !gradleBinZip.toFile().exists()) {
      downloadDistribution();
      downloadGradleToolingApi();
    }
    final Set<String> artifactIds = extract();
    artifactIds.add(GRADLE_TOOLING_API_ARTIFACT_ID);
    return artifactIds;
  }

  private void downloadDistribution() {
    log.info("Downloading Gradle " + gradleVersion + "...");
    try {
      Files.createDirectories(resolveGroupDir());
      final InputStream gradleBinStream = streamUrl(GRADLE_DISTRIBUTION_BASE_URL + gradleBinZip.toFile().getName());
      writeToFile(gradleBinStream, gradleBinZip);
      writePom(GRADLE_ALL_ARTIFACT_ID, "pom");
      log.info("Gradle " + gradleVersion + " download complete");
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't download Gradle " + gradleVersion, e);
    }
  }

  /*
   * Gradle tooling api artifact needs to be downloaded from the Maven repository.
   * This artifact is published by Gradle to https://repo.gradle.org/ui/native/libs-releases/
   * The published artifact is different from that included in the Gradle official distribution.
   * It's published to Maven to add support for IDEs and other tools to interact with Gradle,
   * and includes additional classes that are not included in the Gradle distribution.
   * (org.gradle.wrapper, org.gradle.internal, org.gradle.api, and more)
   */
  private void downloadGradleToolingApi() {
    log.info("Downloading Gradle Tooling API " + gradleVersion + "...");
    try {
      final InputStream gradleToolingApi = streamUrl(GRADLE_LIB_RELEASES_BASE_URL +"org/gradle/gradle-tooling-api/" +
        gradleVersion + "/" + GRADLE_TOOLING_API_ARTIFACT_ID + "-" + gradleVersion + ".jar");
      writeToFile(gradleToolingApi, resolveArtifactJar(GRADLE_TOOLING_API_ARTIFACT_ID));
      writePom(GRADLE_TOOLING_API_ARTIFACT_ID, "jar");
      log.info("Gradle Tooling API " + gradleVersion + " download complete");
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't download Gradle Tooling API " + gradleVersion, e);
    }
  }

  private Set<String> extract() {
    log.info("Extracting Gradle " + gradleVersion + " to local Maven repository...");
    final Set<String> artifactIds = new HashSet<>();
    try (
      ZipFile zipFile = new ZipFile(gradleBinZip.toFile())
    ) {
      final Set<ZipEntry> applicableEntries = zipFile.stream()
        .filter(e -> e.getName().indexOf("gradle-", e.getName().lastIndexOf('/') + 1) >= 0)
        .filter(e -> e.getName().endsWith("-" + gradleVersion + ".jar"))
        // Gradle Tooling API requires special treatment
        .filter(e -> !e.getName().endsWith("gradle-tooling-api-" + gradleVersion + ".jar"))
        .collect(Collectors.toSet());
      for (ZipEntry entry : applicableEntries) {
        final String artifactJar = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
        final String artifactId = artifactJar.substring(0, artifactJar.lastIndexOf('-' + gradleVersion));
        final Path artifactJarPath = resolveArtifactJar(artifactId);
        if (forceUpdate || !artifactJarPath.toFile().exists()) {
          Files.createDirectories(artifactJarPath.getParent());
          writeToFile(zipFile.getInputStream(entry), artifactJarPath);
          writePom(artifactId, "jar");
        }
        artifactIds.add(artifactId);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't extract Gradle " + gradleVersion, e);
    }
    return artifactIds;
  }

  private Path resolveArtifactJar(String artifact) {
    return resolveArtifactDir(artifact).resolve(artifact + "-" + gradleVersion + ".jar");
  }

  private Path resolveArtifactDir(String artifact) {
    return resolveGroupDir().resolve(artifact).resolve(gradleVersion);
  }

  private Path resolveGroupDir() {
    return repositoryBaseDir.resolve("org").resolve("gradle");
  }

  private InputStream streamUrl(String url) throws IOException {
    final URL remoteBin = new URL(url);
    // Connection
    final InputStream stream;
    if (proxy == null) {
      stream = remoteBin.openStream();
    } else {
      log.info("Using proxy");
      stream = remoteBin.openConnection(proxy).getInputStream();
    }
    return stream;
  }

  private void writePom(String artifact, String packaging) throws IOException {
    final Path pom = resolveArtifactDir(artifact).resolve(artifact + "-" + gradleVersion + ".pom");
    Files.deleteIfExists(pom);
    Files.write(
      Files.createFile(pom),
      Arrays.asList(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<!-- Generated by https://github.com/manusa/gradle-api-maven-plugin -->",
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
        "  <modelVersion>4.0.0</modelVersion>",
        "  <groupId>" + GRADLE_GROUP_ID + "</groupId>",
        "  <artifactId>" + artifact + "</artifactId>",
        "  <version>" + gradleVersion + "</version>",
        "  <packaging>" + packaging + "</packaging>",
        "</project>"
      )
    );
  }

  private static void writeToFile(InputStream stream, Path targetPath) throws IOException {
    Files.deleteIfExists(targetPath);
    Files.createDirectories(targetPath.getParent());
    Files.createFile(targetPath);
    try (
      ReadableByteChannel in = Channels.newChannel(stream);
      FileChannel out = FileChannel.open(targetPath, WRITE)
    ) {
      out.transferFrom(in, 0, Long.MAX_VALUE);
    }
  }
}
