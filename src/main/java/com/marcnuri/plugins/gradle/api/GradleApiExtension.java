package com.marcnuri.plugins.gradle.api;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;

import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.marcnuri.plugins.gradle.api.GradleApi.GRADLE_ALL_ARTIFACT_ID;
import static com.marcnuri.plugins.gradle.api.GradleApi.GRADLE_GROUP_ID;

@Named("gradle-api")
public class GradleApiExtension extends AbstractMavenLifecycleParticipant {

  @Override
  public void afterProjectsRead(MavenSession session) {
    final Log log = new SystemStreamLog();
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
      artifactForVersion.getValue().addAll(new GradleApi(log,
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
}
