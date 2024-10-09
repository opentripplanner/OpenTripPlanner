package org.opentripplanner.standalone.server;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

public class VaryRequestFilter implements ContainerResponseFilter {

  public static final String HEADER_VARY = "Vary";

  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response)
    throws IOException {
    var headers = response.getHeaders();
    headers.add(HEADER_VARY, "Accept, Accept-Encoding, Accept-Language");
  }
}
