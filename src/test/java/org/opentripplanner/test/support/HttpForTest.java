package org.opentripplanner.test.support;

import java.net.URI;
import java.net.URISyntaxException;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;

public class HttpForTest {

  public static ContainerRequest containerRequest() {
    try {
      URI uri = new URI("https://localhost:8080");
      return new ContainerRequest(uri, uri, "GET", null, new MapPropertiesDelegate(), null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
