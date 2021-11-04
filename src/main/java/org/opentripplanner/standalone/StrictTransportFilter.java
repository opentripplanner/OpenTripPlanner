package org.opentripplanner.standalone;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

/**
 * Simple filter to add Strict Transport Security (HSTS) headers to responses.
 */
public class StrictTransportFilter implements ContainerResponseFilter {

  /**
   * prevent man in the middle attacks per RFC6797
   * and https://www.owasp.org/index.php/HTTP_Strict_Transport_Security_Cheat_Sheet
   */
  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
    String origin = request.getHeaderString("Origin"); // case insensitive
    MultivaluedMap<String, Object> headers = response.getHeaders();
    // we can provide this over HTTP or HTTPS; it's simply ignored over HTTP
    headers.add("Strict-Transport-Security", "max-age=31536000;includeSubDomains");
  }


}
