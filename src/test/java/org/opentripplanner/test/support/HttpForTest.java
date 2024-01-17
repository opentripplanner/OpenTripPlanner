package org.opentripplanner.test.support;

import java.net.URI;
import java.net.URISyntaxException;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;

public class HttpForTest {

  public static ContainerRequest containerRequest(String method) {
    try {
      URI uri = new URI("https://localhost:8080");
      return new ContainerRequest(uri, uri, method, null, new MapPropertiesDelegate(), null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static ContainerRequest containerRequest() {
    return containerRequest("GET");
  }
}
