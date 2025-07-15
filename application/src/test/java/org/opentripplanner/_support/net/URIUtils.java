package org.opentripplanner._support.net;

import java.net.URI;
import java.net.URISyntaxException;

public class URIUtils {

  /**
   * Create URI, throw {@link RuntimeException} if it fails. This is used to simplify tests, so
   * they do not have to handle the checked {@link URISyntaxException} thrown by the Java
   * {@link URI} constructor.
   */
  public static URI uri(String uri) {
    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
