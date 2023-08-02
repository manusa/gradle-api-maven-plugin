package com.marcnuri.plugins.gradle.api;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import javax.inject.Named;
import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.marcnuri.plugins.gradle.api.GradleApi.GRADLE_GROUP_ID;

@Named("gradle-api")
public class GradleApiExtension extends AbstractMavenLifecycleParticipant {

  @Override
  public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
    final Log log = new SystemStreamLog();
    final Set<String> versions = new HashSet<>();
    final Set<String> artifactIds = new HashSet<>();
    session.getCurrentProject().getDependencies().stream()
      .filter(d -> d.getGroupId().equals(GRADLE_GROUP_ID))
      .forEach(d -> {
        if (!Objects.equals(d.getScope(), "provided")) {
          log.warn("Gradle API dependency should be provided, check: " + d.getGroupId() + ":" + d.getArtifactId());
        }
        artifactIds.add(d.getArtifactId());
        versions.add(d.getVersion());
      });
    if (versions.size() > 1) {
      throw new MavenExecutionException("Multiple Gradle API versions detected, please depend on a single version",
        session.getCurrentProject().getFile());
    }
    if (!versions.isEmpty()) {
      new GradleApi(log,
        session.getRequest().isUpdateSnapshots(),
        versions.iterator().next(),
        artifactIds,
        new File(session.getLocalRepository().getBasedir()).toPath()
      ).call();
    }
  }
}
