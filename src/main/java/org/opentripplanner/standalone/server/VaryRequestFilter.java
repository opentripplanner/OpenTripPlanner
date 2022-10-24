package org.opentripplanner.standalone.server;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

public class VaryRequestFilter implements ContainerResponseFilter {

  public static final String HEADER_VARY = "Vary";

  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response)
    throws IOException {
    var headers = response.getHeaders();
    headers.add(HEADER_VARY, "Accept, Accept-Encoding, Accept-Language");
  }
}
