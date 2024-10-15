package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Loads files from the resources folder relative to the package name of the class/instances
 * passed to initializers.
 * <p>
 * So if your class' package is org.opentripplanner.foo, then the corresponding resources
 * must be placed in src/test/resources/org/opentripplanner/foo.
 */
public class ResourceLoader {

  private final Class<?> clazz;

  private ResourceLoader(Class<?> clazz) {
    this.clazz = clazz;
  }

  /**
   * Initialize a loader with the given class' package.
   */
  public static ResourceLoader of(Class<?> clazz) {
    return new ResourceLoader(clazz);
  }

  /**
   * Initialize a loader with the given instances' class' package.
   */
  public static ResourceLoader of(Object object) {
    return new ResourceLoader(object.getClass());
  }

  /**
   * Return a File instance for the given path.
   */
  public File file(String path) {
    URL resource = url(path);
    var file = new File(resource.getFile());
    assertTrue(
      file.exists(),
      "File '%s' not found on file system.".formatted(file.getAbsolutePath())
    );
    return file;
  }

  /**
   * Returns the string content of a file.
   */
  public String fileToString(String p) {
    try {
      return Files.readString(file(p).toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return a URL for the given resource.
   */
  public URL url(String name) {
    var resource = clazz.getResource(name);
    var msg = "Resource '%s' not found in package '%s'".formatted(name, clazz.getPackageName());
    if (resource == null) {
      throw new IllegalArgumentException(msg);
    }
    return resource;
  }

  /**
   * Return a URI for the given resource.
   */
  public URI uri(String s) {
    try {
      return url(s).toURI();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Returns the specified number of lines from a file.
   */
  public List<String> lines(String s, int lines) {
    var path = file(s).toPath();
    try {
      return Files.readAllLines(path, StandardCharsets.UTF_8).stream().limit(lines).toList();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the specified resources as an input stream.
   */
  public InputStream inputStream(String path) {
    try {
      return url(path).openStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
