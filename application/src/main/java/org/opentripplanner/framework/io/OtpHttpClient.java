package org.opentripplanner.framework.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;

/**
 * HTTP client providing convenience methods to send HTTP requests and map HTTP responses to Java
 * objects.
 *
 * <p>
 * <p/>
 * <h3>Exception management</h3>
 * Exceptions thrown during network operations or response mapping are wrapped in
 * {@link OtpHttpClientException}
 * <h3>Resource management</h3>
 * It is recommended to use the <code>getAndMapXXX</code> and <code>postAndMapXXX</code> methods in
 * this class since they ensure that the underlying network resources are properly released. The
 * method {@link #getAsInputStream} gives access to an input stream on the body response but
 * requires the caller to close this stream. For most use cases, this method is not recommended.
 *
 * <h3>Thread-safety</h3>
 * Instances of this class are thread-safe.
 */
public class OtpHttpClient {

  private final CloseableHttpClient httpClient;

  private final Logger log;

  /**
   * Creates an HTTP client with custom configuration.
   */
  OtpHttpClient(CloseableHttpClient httpClient, Logger logger) {
    this.httpClient = httpClient;
    log = logger;
  }

  /**
   * Executes an HTTP HEAD request and returns the headers. Returns an empty list if the HTTP server
   * does not accept HTTP HEAD requests.
   */
  public List<Header> getHeaders(
    URI uri,
    Duration timeout,
    Map<String, String> requestHeaderValues
  ) {
    return executeAndMapWithResponseHandler(
      new HttpHead(uri),
      timeout,
      requestHeaderValues,
      response -> {
        if (isFailedRequest(response)) {
          logResponse(response);
          log.warn(
            "Headers of resource {} unavailable. HTTP error code {}",
            sanitizeUri(uri),
            response.getCode()
          );

          return Collections.emptyList();
        }
        return Arrays.stream(response.getHeaders()).toList();
      }
    );
  }

  /**
   * Executes an HTTP GET request and returns the body mapped as a JSON object. The default timeout
   * is applied.
   */
  public <T> T getAndMapAsJsonObject(
    URI uri,
    Map<String, String> headers,
    ObjectMapper objectMapper,
    Class<T> clazz
  ) {
    return getAndMapAsJsonObject(uri, null, headers, objectMapper, clazz);
  }

  /**
   * Executes an HTTP GET request and returns the body mapped as a JSON object.
   */
  public <T> T getAndMapAsJsonObject(
    URI uri,
    Duration timeout,
    Map<String, String> headers,
    ObjectMapper objectMapper,
    Class<T> clazz
  ) {
    return getAndMap(uri, timeout, headers, is -> {
      try {
        return objectMapper.readValue(is, clazz);
      } catch (Exception e) {
        throw new OtpHttpClientException(e);
      }
    });
  }

  /**
   * Executes an HTTP GET request and returns the body mapped as a JSON node. The default timeout is
   * applied.
   */
  public JsonNode getAndMapAsJsonNode(
    URI uri,
    Map<String, String> headers,
    ObjectMapper objectMapper
  ) {
    return getAndMapAsJsonNode(uri, null, headers, objectMapper);
  }

  /**
   * Executes an HTTP GET request and returns the body mapped as a JSON node.
   */
  public JsonNode getAndMapAsJsonNode(
    URI uri,
    Duration timeout,
    Map<String, String> headers,
    ObjectMapper objectMapper
  ) {
    return getAndMap(uri, timeout, headers, is -> {
      try {
        return objectMapper.readTree(is);
      } catch (Exception e) {
        throw new OtpHttpClientException(e);
      }
    });
  }

  /**
   * Executes an HTTP GET request and returns the body mapped according to the provided content
   * mapper. The default timeout is applied.
   */
  public <T> T getAndMap(URI uri, Map<String, String> headers, ResponseMapper<T> contentMapper) {
    return getAndMap(uri, null, headers, contentMapper);
  }

  /**
   * Executes an HTTP GET request and returns the body mapped according to the provided content
   * mapper. If the protocol is neither HTTP nor HTTPS, the URI is interpreted as a local file.
   */
  public <T> T getAndMap(
    URI uri,
    Duration timeout,
    Map<String, String> headers,
    ResponseMapper<T> contentMapper
  ) {
    return sendAndMap(new HttpGet(uri), uri, timeout, headers, contentMapper);
  }

  /**
   * Send an HTTP POST request with Content-Type: application/json. The body of the request
   * is defined by {@code jsonBody}.
   */
  public <T> T postJsonAndMap(
    URI uri,
    JsonNode jsonBody,
    Duration timeout,
    Map<String, String> headers,
    ResponseMapper<T> contentMapper
  ) {
    var request = new HttpPost(uri);
    request.setEntity(new StringEntity(jsonBody.toString()));
    request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON);
    return sendAndMap(request, uri, timeout, headers, contentMapper);
  }

  /**
   * Executes an HTTP POST request with the provided XML request body and returns the body mapped
   * according to the provided content mapper.
   */
  public <T> T postXmlAndMap(
    String url,
    String xmlData,
    Duration timeout,
    Map<String, String> requestHeaderValues,
    ResponseMapper<T> contentMapper
  ) {
    HttpPost httppost = new HttpPost(url);
    if (xmlData != null) {
      httppost.setEntity(new StringEntity(xmlData, ContentType.APPLICATION_XML));
    }

    return executeAndMap(httppost, timeout, requestHeaderValues, contentMapper);
  }

  /**
   * Executes an HTTP request and returns the body mapped according to the provided content mapper.
   */
  public <T> T executeAndMap(
    HttpUriRequestBase httpRequest,
    Duration timeout,
    Map<String, String> headers,
    ResponseMapper<T> contentMapper
  ) {
    return executeAndMapWithResponseHandler(httpRequest, timeout, headers, response ->
      mapResponse(response, contentMapper)
    );
  }

  /**
   * Executes an HTTP request and returns the body mapped according to the provided content mapper.
   * Returns empty result on http status 204 "No Content"
   */
  public <T> Optional<T> executeAndMapOptional(
    HttpUriRequestBase httpRequest,
    Duration timeout,
    Map<String, String> headers,
    ResponseMapper<T> contentMapper
  ) {
    return executeAndMapWithResponseHandler(httpRequest, timeout, headers, response -> {
      if (response.getCode() == 204) {
        return Optional.empty();
      }
      return Optional.of(mapResponse(response, contentMapper));
    });
  }

  /**
   * Executes an HTTP GET request and returns an input stream on the response body. The caller must
   * close the stream in order to release resources. Use preferably the provided getAndMapXXX
   * methods in this class, since they provide automatic resource management.
   */
  public InputStream getAsInputStream(
    URI uri,
    Duration timeout,
    Map<String, String> requestHeaders
  ) throws IOException {
    Objects.requireNonNull(uri);
    Objects.requireNonNull(timeout);
    Objects.requireNonNull(requestHeaders);

    HttpUriRequestBase httpRequest = new HttpGet(uri);
    httpRequest.setConfig(requestConfig(timeout));
    requestHeaders.forEach(httpRequest::addHeader);
    CloseableHttpResponse response = httpClient.execute(httpRequest);

    if (isFailedRequest(response)) {
      throw new IOException(
        "Service unavailable: " +
        uri +
        ". HTTP status code: " +
        response.getCode() +
        " - " +
        response.getReasonPhrase()
      );
    }
    HttpEntity entity = response.getEntity();
    if (entity == null) {
      throw new IOException("HTTP response message entity is empty for url: " + uri);
    }
    return entity.getContent();
  }

  /**
   * Executes an HTTP request and returns the body mapped according to the provided response
   * handler.
   */
  protected <T> T executeAndMapWithResponseHandler(
    HttpUriRequestBase httpRequest,
    Duration timeout,
    Map<String, String> requestHeaders,
    final HttpClientResponseHandler<? extends T> responseHandler
  ) {
    Objects.requireNonNull(requestHeaders);
    if (timeout != null) {
      httpRequest.setConfig(requestConfig(timeout));
    }
    requestHeaders.forEach(httpRequest::addHeader);
    try {
      return httpClient.execute(httpRequest, responseHandler);
    } catch (IOException e) {
      throw new OtpHttpClientException(e);
    }
  }

  private <T> T mapResponse(ClassicHttpResponse response, ResponseMapper<T> contentMapper) {
    if (isFailedRequest(response)) {
      logResponse(response);
      throw new OtpHttpClientException(
        "HTTP request failed with status code " + response.getCode()
      );
    }
    if (response.getEntity() == null) {
      throw new OtpHttpClientException("HTTP request failed: empty response");
    }
    try (InputStream is = response.getEntity().getContent()) {
      if (is == null) {
        throw new OtpHttpClientException("HTTP request failed: empty response");
      }
      return contentMapper.apply(is);
    } catch (Exception e) {
      throw new OtpHttpClientException(e);
    }
  }

  private <T> T sendAndMap(
    HttpUriRequestBase request,
    URI uri,
    Duration timeout,
    Map<String, String> headers,
    ResponseMapper<T> contentMapper
  ) {
    URL downloadUrl;
    try {
      downloadUrl = uri.toURL();
    } catch (MalformedURLException e) {
      throw new OtpHttpClientException(e);
    }
    String proto = downloadUrl.getProtocol();
    if (proto.equals("http") || proto.equals("https")) {
      return executeAndMap(request, timeout, headers, contentMapper);
    } else {
      // Local file probably, try standard java
      try (InputStream is = downloadUrl.openStream()) {
        return contentMapper.apply(is);
      } catch (Exception e) {
        throw new OtpHttpClientException(e);
      }
    }
  }

  /**
   * Configures the request with a custom timeout.
   */

  private static RequestConfig requestConfig(Duration timeout) {
    return RequestConfig.custom()
      .setResponseTimeout(Timeout.of(timeout))
      .setConnectionRequestTimeout(Timeout.of(timeout))
      .setProtocolUpgradeEnabled(false)
      .build();
  }

  /**
   * Returns true if the HTTP status code is not 200.
   */
  private static boolean isFailedRequest(HttpResponse response) {
    return response.getCode() < 200 || response.getCode() >= 300;
  }

  /**
   * Removes the query part from the URI.
   */
  private static String sanitizeUri(URI uri) {
    return uri.toString().replace('?' + uri.getQuery(), "");
  }

  private void logResponse(ClassicHttpResponse response) {
    try {
      if (
        log.isTraceEnabled() &&
        response.getEntity() != null &&
        response.getEntity().getContent() != null
      ) {
        var entity = response.getEntity();
        String content = new BufferedReader(new InputStreamReader(entity.getContent()))
          .lines()
          .collect(Collectors.joining("\n"));
        log.trace("HTTP request failed with status code {}: \n{}", response.getCode(), content);
      }
    } catch (Exception e) {
      log.debug(e.getMessage());
    }
  }

  @FunctionalInterface
  public interface ResponseMapper<R> {
    /**
     * Maps the response input stream.
     *
     * @return the mapping function result
     */
    R apply(InputStream t) throws Exception;
  }
}
