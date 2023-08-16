package org.opentripplanner.test.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Loads files from the resources folder.
 */
public class ResourceLoader {

  /**
   * Return a File instance for the given path.
   */
  public static File file(String path) {
    final URL resource = ResourceLoader.class.getResource(path);
    final String msg = "Resource %s not found on file system".formatted(path);
    assertNotNull(resource, msg);
    var file = new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8));
    assertTrue(file.exists(), msg);
    return file;
  }
}
