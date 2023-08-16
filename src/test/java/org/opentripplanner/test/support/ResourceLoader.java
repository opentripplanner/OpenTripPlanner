package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
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
   * Return a URL for the given path.
   */
  public static URL url(String s) {
    var resource = ResourceLoader.class.getResource(s);
    var msg = "Resource %s not found on file system".formatted(s);
    assertNotNull(resource, msg);
    return resource;
  }
}
