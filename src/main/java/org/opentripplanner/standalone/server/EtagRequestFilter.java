package org.opentripplanner.standalone.server;

import com.google.common.hash.Hashing;
import java.io.IOException;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpStatus;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;

public class EtagRequestFilter implements ContainerResponseFilter {

  public static final String DIRECTIVE_NO_STORE = "no-store";
  public static final String HEADER_ETAG = "ETag";
  public static final String HEADER_IF_NONE_MATCH = "If-None-Match";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";

  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response)
    throws IOException {
    if (
      isEligibleForEtag(request, response) &&
      hasAllowedContentType(response) &&
      response.getEntity() instanceof byte[] bytes
    ) {
      var clientEtag = request.getHeaderString(HEADER_IF_NONE_MATCH);
      var etag = generateETagHeaderValue(bytes);
      var headers = response.getHeaders();
      headers.add(HEADER_ETAG, etag);

      // if the client's etag matches the generated one then send an empty response
      if (clientEtag != null && clientEtag.equals(etag)) {
        response.setEntity(null);
        response.setStatus(HttpStatus.SC_NOT_MODIFIED);
      }
    }
  }

  private static boolean isEligibleForEtag(
    ContainerRequestContext request,
    ContainerResponseContext response
  ) {
    if (response.getLength() <= 0) {
      return false;
    }
    var statusCode = response.getStatus();
    if (statusCode >= 200 && statusCode < 300 && HttpMethod.GET.matches(request.getMethod())) {
      String cacheControl = response.getHeaderString("Cache-Control");
      return (cacheControl == null || !cacheControl.contains(DIRECTIVE_NO_STORE));
    }

    return false;
  }

  private static String generateETagHeaderValue(byte[] input) {
    StringBuilder builder = new StringBuilder(11);
    builder.append("\"0");
    var md5 = Hashing.murmur3_32_fixed().hashBytes(input).asBytes();
    var hex = Hex.encodeHex(md5);
    builder.append(hex);
    builder.append('"');
    return builder.toString();
  }

  private static boolean hasAllowedContentType(ContainerResponseContext response) {
    return VectorTilesResource.APPLICATION_X_PROTOBUF.equals(
      response.getStringHeaders().getFirst(HEADER_CONTENT_TYPE)
    );
  }
}
