package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Loads files from the resources folder.
 */
public class ResourceLoader {

  /**
   * Return a File instance for the given path.
   */
  public static File file(String path) {
    URL resource = url(path);
    var file = new File(resource.getFile());
    assertTrue(file.exists(), "File %s not found on file system".formatted(file.getAbsolutePath()));
    return file;
  }

  /**
   * Return a File instance for the given name from the /osm subfolder.
   */
  public static File osmFile(String osmFile) {
    return file("/osm/" + osmFile);
  }

  /**
   * Return a URL for the given resource.
   */
  public static URL url(String name) {
    var resource = ResourceLoader.class.getResource(name);
    var msg = "Resource %s not found on file system".formatted(resource);
    assertNotNull(resource, msg);
    return resource;
  }

  /**
   * Return a URI for the given resource.
   */
  public static URI uri(String s) {
    try {
      return url(s).toURI();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
