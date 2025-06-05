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
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.datastore.file.FileDataSourceRepository;

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
   * Return a composite datasource (directory or zip) in the resource catalog. The given
   * {@code relativePath} should be a directory in the "package" catalog.
   */
  public CompositeDataSource catalogDataSource(String relativePath, FileType fileType) {
    return FileDataSourceRepository.compositeSource(file(relativePath), fileType);
  }

  /** Return a file datasource with the given {@code filename}. */
  public DataSource dataSource(String filename, FileType fileType) {
    return new FileDataSource(file(filename), fileType);
  }

  /**
   * Return a File instance for the given path.
   */
  public File file(String path) {
    URL resource = url(path);
    File file;
    try {
      file = new File(new URI(resource.toString()));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    assertFileExists(file);
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
   * Returns a File instance in the original main resources folder.
   */
  public File mainResourceFile(String path) {
    return resourceFile("main", path);
  }

  /**
   * Returns a File instance in the original test resources folder.
   */
  public File testResourceFile(String path) {
    return resourceFile("test", path);
  }

  /**
   * Returns a File instance in the original ext-test resources folder.
   */
  public File extTestResourceFile(String path) {
    return resourceFile("ext-test", path);
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

  private static void assertFileExists(File file) {
    assertTrue(
      file.exists(),
      "File '%s' not found on file system.".formatted(file.getAbsolutePath())
    );
  }

  /**
   * Returns a File instance from the resources folder of the specified resourceDir (for example
   * test).
   */
  private File resourceFile(String resourceDir, String path) {
    var fullPath =
      "src/%s/resources/%s/%s".formatted(
          resourceDir,
          clazz.getPackage().getName().replace(".", "/"),
          path
        );
    File file = new File(fullPath);
    assertFileExists(file);
    return file;
  }
}
