package org.opentripplanner.standalone.server;

import static org.opentripplanner.framework.io.HttpUtils.APPLICATION_X_PROTOBUF;

import com.google.common.hash.Hashing;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.hc.core5.http.HttpStatus;
import org.opentripplanner.utils.text.HexString;

public class EtagRequestFilter implements ContainerResponseFilter {

  public static final String DIRECTIVE_NO_STORE = "no-store";
  public static final String HEADER_ETAG = "ETag";
  public static final String HEADER_IF_NONE_MATCH = "If-None-Match";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_CACHE_CONTROL = "Cache-Control";

  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response)
    throws IOException {
    if (
      isEligibleForEtag(request, response) &&
      hasAllowedContentType(response) &&
      response.getEntity() instanceof byte[] bytes &&
      bytes.length > 0
    ) {
      var clientEtag = request.getHeaderString(HEADER_IF_NONE_MATCH);
      var etag = generateETagHeaderValue(bytes);
      var headers = response.getHeaders();
      headers.add(HEADER_ETAG, etag);

      // if the client's etag matches the generated one then send an empty response
      if (clientEtag != null && clientEtag.equals(etag)) {
        response.setEntity(null);
        response.setEntityStream(new ByteArrayOutputStream());
        response.setStatus(HttpStatus.SC_NOT_MODIFIED);
      }
    }
  }

  private static boolean isEligibleForEtag(
    ContainerRequestContext request,
    ContainerResponseContext response
  ) {
    var statusCode = response.getStatus();
    if (statusCode >= 200 && statusCode < 300 && HttpMethod.GET.matches(request.getMethod())) {
      String cacheControl = response.getHeaderString(HEADER_CACHE_CONTROL);
      return (cacheControl == null || !cacheControl.contains(DIRECTIVE_NO_STORE));
    }

    return false;
  }

  private static String generateETagHeaderValue(byte[] input) {
    StringBuilder builder = new StringBuilder(10);
    builder.append('"');
    // according to https://softwareengineering.stackexchange.com/questions/49550
    // Murmur is the fastest hash algorithm and has an acceptable number of collisions.
    // (It doesn't need to be cryptographically secure.)
    var hash = Hashing.murmur3_32_fixed().hashBytes(input).asBytes();
    var hex = HexString.of(hash);
    builder.append(hex);
    builder.append('"');
    return builder.toString();
  }

  private static boolean hasAllowedContentType(ContainerResponseContext response) {
    return APPLICATION_X_PROTOBUF.equals(response.getStringHeaders().getFirst(HEADER_CONTENT_TYPE));
  }
}
