package org.opentripplanner.framework.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;

public class HttpUtils {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

  public static InputStream getData(URI uri) throws IOException {
    return getData(uri, null);
  }

  public static InputStream getData(String uri) throws IOException {
    return getData(URI.create(uri));
  }

  public static InputStream getData(String uri, Map<String, String> headers) throws IOException {
    return getData(URI.create(uri), headers);
  }

  public static InputStream getData(
    URI uri,
    Duration timeout,
    Map<String, String> requestHeaderValues
  ) throws IOException {
    HttpResponse response = getResponse(new HttpGet(uri), timeout, requestHeaderValues);
    if (response.getStatusLine().getStatusCode() != 200) {
      return null;
    }
    HttpEntity entity = response.getEntity();
    if (entity == null) {
      return null;
    }
    return entity.getContent();
  }

  public static InputStream getData(URI uri, Map<String, String> requestHeaderValues)
    throws IOException {
    return getData(uri, DEFAULT_TIMEOUT, requestHeaderValues);
  }

  public static List<Header> getHeaders(URI uri) {
    return getHeaders(uri, DEFAULT_TIMEOUT, null);
  }

  public static List<Header> getHeaders(
    URI uri,
    Duration timeout,
    Map<String, String> requestHeaderValues
  ) {
    HttpResponse response;
    //
    try {
      response = getResponse(new HttpHead(uri), timeout, requestHeaderValues);
    } catch (IOException e) {
      throw new RuntimeException(
        "Network error while querying headers for resource " + sanitizeUri(uri),
        e
      );
    }
    if (response.getStatusLine().getStatusCode() != 200) {
      throw new RuntimeException(
        "Resource " +
        sanitizeUri(uri) +
        " unavailable. HTTP error code " +
        response.getStatusLine().getStatusCode()
      );
    }
    return Arrays.stream(response.getAllHeaders()).toList();
  }

  /**
   * Remove the query part from the URI.
   */
  private static String sanitizeUri(URI uri) {
    return uri.toString().replace('?' + uri.getQuery(), "");
  }

  public static InputStream openInputStream(String url, Map<String, String> headers)
    throws IOException {
    return openInputStream(URI.create(url), headers);
  }

  public static InputStream openInputStream(URI uri, Map<String, String> headers)
    throws IOException {
    URL downloadUrl = uri.toURL();
    String proto = downloadUrl.getProtocol();
    if (proto.equals("http") || proto.equals("https")) {
      return HttpUtils.getData(uri, headers);
    } else {
      // Local file probably, try standard java
      return downloadUrl.openStream();
    }
  }

  private static HttpResponse getResponse(
    HttpRequestBase httpRequest,
    Duration timeout,
    Map<String, String> requestHeaderValues
  ) throws IOException {
    var to = (int) timeout.toMillis();
    RequestConfig requestConfig = RequestConfig
      .custom()
      .setSocketTimeout(to)
      .setConnectTimeout(to)
      .setConnectionRequestTimeout(to)
      .build();

    httpRequest.setConfig(requestConfig);

    if (requestHeaderValues != null) {
      for (Map.Entry<String, String> entry : requestHeaderValues.entrySet()) {
        httpRequest.addHeader(entry.getKey(), entry.getValue());
      }
    }

    return HttpClientBuilder.create().build().execute(httpRequest);
  }
}
