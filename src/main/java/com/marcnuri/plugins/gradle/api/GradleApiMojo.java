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
