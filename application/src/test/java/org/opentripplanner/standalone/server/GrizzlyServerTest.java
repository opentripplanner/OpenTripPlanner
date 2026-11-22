package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrizzlyServerTest {

  @TempDir
  Path classpathRoot;

  @Test
  void debugClientIsFoundWhenItIsOnTheClasspath() throws IOException {
    Files.createDirectories(classpathRoot.resolve("client"));
    Files.writeString(classpathRoot.resolve("client/index.html"), "<html></html>");

    try (var classLoader = classLoaderWithoutParent()) {
      assertTrue(GrizzlyServer.hasDebugClient(classLoader));
    }
  }

  @Test
  void debugClientIsMissingWhenItIsNotOnTheClasspath() throws IOException {
    try (var classLoader = classLoaderWithoutParent()) {
      assertFalse(GrizzlyServer.hasDebugClient(classLoader));
    }
  }

  /** No parent, so that a debug client on the classpath of the test run is not found. */
  private URLClassLoader classLoaderWithoutParent() throws IOException {
    return new URLClassLoader(new URL[] { classpathRoot.toUri().toURL() }, null);
  }
}
