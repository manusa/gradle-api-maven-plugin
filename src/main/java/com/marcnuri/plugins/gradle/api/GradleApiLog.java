package com.marcnuri.plugins.gradle.api;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.util.function.Consumer;

public final class GradleApiLog {

  private final int loggingLevel;
  private final Log delegate;

  GradleApiLog(int loggingLevel) {
    this.loggingLevel = loggingLevel;
    delegate = new SystemStreamLog();
  }

  public void debug(CharSequence content) {
    log(MavenExecutionRequest.LOGGING_LEVEL_DEBUG, delegate::debug, content);
  }

  public void info(CharSequence content) {
    log(MavenExecutionRequest.LOGGING_LEVEL_INFO, delegate::info, content);
  }

  public void warn(CharSequence content) {
    log(MavenExecutionRequest.LOGGING_LEVEL_WARN, delegate::warn, content);
  }

  public void error(CharSequence content) {
    log(MavenExecutionRequest.LOGGING_LEVEL_ERROR, delegate::error, content);
  }

  private void log(int level, Consumer<CharSequence> logFunc, CharSequence content) {
    if (loggingLevel <= level) {
      logFunc.accept(content);
    }
  }
}
