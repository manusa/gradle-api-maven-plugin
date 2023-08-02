package com.marcnuri.plugins.gradle.api;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.file.StandardOpenOption.WRITE;

public class GradleApi implements Callable<Collection<Path>> {

  static final String GRADLE_GROUP_ID = "org.gradle";
  private static final String GRADLE_DISTRIBUTION_BASE_URL = "https://services.gradle.org/distributions/";
  private final Log log;
  private final boolean forceDownload;
  private final String gradleVersion;
  private final Path repositoryBaseDir;
  private final Path tempDir;
  private final Map<String, String> artifactBinaries;

  public GradleApi(Log log, boolean forceDownload, String gradleVersion, Collection<String> artifactIds, Path repositoryBaseDir) {
    this.log = log;
    this.gradleVersion = gradleVersion;
    this.forceDownload = forceDownload;
    this.repositoryBaseDir = repositoryBaseDir;
    tempDir = new File(System.getProperty("java.io.tmpdir")).toPath();
    artifactBinaries = artifactIds.stream()
      .collect(Collectors.toMap(a -> a + "-" + gradleVersion + ".jar", Function.identity()));
  }

  @Override
  public final Collection<Path> call() {
    if (forceDownload || shouldDownload()) {
      download();
    }
    final Set<Path> jarFiles = artifactBinaries.values().stream()
      .map(this::resolveArtifactJar)
      .collect(java.util.stream.Collectors.toSet());
    for (Path jarFile : jarFiles) {
      if (!jarFile.toFile().exists()) {
        throw new IllegalStateException("Couldn't find required " + jarFile.toFile().getName() + "\n" +
          "Try to force download with the Maven -U --update-snapshots option");
      }
    }
    return jarFiles;
  }

  private boolean shouldDownload() {
    for (String artifact : artifactBinaries.values()) {
      if (!resolveArtifactJar(artifact).toFile().exists()) {
        return true;
      }
    }
    return false;
  }

  private void download() {
    log.info("Downloading Gradle " + gradleVersion + "...");
    try {
      final String distArchive = "gradle-" + gradleVersion + "-bin.zip";
      final URL remoteBin = new URL(GRADLE_DISTRIBUTION_BASE_URL + distArchive);
      final Path localBin = tempDir.resolve(distArchive);
      writeToFile(remoteBin.openStream(), localBin);
      localBin.toFile().deleteOnExit();
      log.info("Gradle " + gradleVersion + " download complete");
      log.info("Extracting Gradle " + gradleVersion + " to local Maven repository...");
      try (
        ZipFile zipFile = new ZipFile(localBin.toFile())
      ) {
        final Set<ZipEntry> toExtract = new HashSet<>();
        zipFile.stream()
          .filter(e -> artifactBinaries.keySet().stream().anyMatch(b -> e.getName().endsWith(b)))
          .forEach(toExtract::add);
        for (ZipEntry entry : toExtract) {
          final String artifactJar = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
          final Path artifactJarPath = resolveArtifactJar(artifactBinaries.get(artifactJar));
          Files.createDirectories(artifactJarPath.getParent());
          writeToFile(zipFile.getInputStream(entry), artifactJarPath);
          writePom(artifactBinaries.get(artifactJar));
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't download Gradle " + gradleVersion, e);
    }
  }

  private Path resolveArtifactJar(String artifact) {
    return resolveArtifactDir(artifact).resolve(artifact + "-" + gradleVersion + ".jar");
  }

  private Path resolveArtifactDir(String artifact) {
    return repositoryBaseDir.resolve("org").resolve("gradle").resolve(artifact).resolve(gradleVersion);
  }

  private void writePom(String artifact) throws IOException {
    Files.write(
      resolveArtifactDir(artifact).resolve(artifact + "-" + gradleVersion + ".pom"),
      Arrays.asList(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
        "  <modelVersion>4.0.0</modelVersion>",
        "  <groupId>" + GRADLE_GROUP_ID + "</groupId>",
        "  <artifactId>" + artifact + "</artifactId>",
        "  <version>" + gradleVersion + "</version>",
        "</project>"
      )
    );
  }

  private static void writeToFile(InputStream stream, Path targetPath) throws IOException {
    Files.deleteIfExists(targetPath);
    Files.createFile(targetPath);
    try (
      ReadableByteChannel in = Channels.newChannel(stream);
      FileChannel out = FileChannel.open(targetPath, WRITE)
    ) {
      out.transferFrom(in, 0, Long.MAX_VALUE);
    }
  }
}
