package org.opentripplanner.standalone.server;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

/**
 * The Same Origin Policy states that JavaScript code (or other scripts) running on a web page may
 * not interact with resources originating from sites with a different hostname, protocol, or port
 * number.
 * <p>
 * We used to use JSONP ("JSON with padding") as a way to get around this. Despite being very
 * common, this is of course a big hack to defeat a security policy. Modern browsers respect "Cross
 * Origin Resource Sharing" (CORS) headers, so we have switched to that system.
 */
class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

  /**
   * CORS request filter. Hijack "preflight" OPTIONS requests before the Jersey resources get them.
   * The response will then pass through the CORS response filter on its way back out.
   */
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (HttpMethod.OPTIONS.equals(requestContext.getMethod())) {
      Response.ResponseBuilder preflightResponse = Response.status(Response.Status.OK);
      if (requestContext.getHeaderString("Access-Control-Request-Headers") != null) {
        preflightResponse.header(
          "Access-Control-Allow-Headers",
          requestContext.getHeaderString("Access-Control-Request-Headers")
        );
      }
      if (requestContext.getHeaderString("Access-Control-Request-Method") != null) {
        preflightResponse.header("Access-Control-Allow-Method", "GET,POST");
      }
      // Allow caching of pre-flight options for up to an hour
      preflightResponse.header("Access-Control-Max-Age", "3600");
      requestContext.abortWith(preflightResponse.build());
    }
  }

  /**
   * CORS response filter. Allow requests from anywhere. Just echo back the contents of the Origin
   * header. Allow credentials if the transport layer is secure.
   */
  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response)
    throws IOException {
    String origin = request.getHeaderString("Origin"); // case insensitive
    MultivaluedMap<String, Object> headers = response.getHeaders();
    headers.add("Access-Control-Allow-Origin", origin);
    boolean secureTransport = request.getSecurityContext().isSecure();
    headers.add("Access-Control-Allow-Credentials", secureTransport);
  }
}
