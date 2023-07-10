package org.opentripplanner.framework.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client providing convenience methods to send HTTP requests and map HTTP responses to Java
 * objects.
 *
 * <p>
 * <p/>
 * <h3>Exception management</h3>
 * Exceptions thrown during network operations or response mapping are wrapped in
 * {@link OtpHttpClientException}
 * <h3>Timeout configuration</h3>
 * The same timeout value is applied to the following parameters:
 * <ul>
 *  <li>Connection request timeout: the maximum waiting time for leasing a connection in the
 *  connection pool.
 *  <li>Connect timeout: the maximum waiting time for the first packet received from the server.
 *  <li>Socket timeout: the maximum waiting time between two packets received from the server.
 * </ul>
 * <h3>Connection time-to-live</h3>
 * Optionally a maximum ttl can be set for the HTTP connections in the
 * connection pool. This is usually not required since HTTP 1.1 and HTTP/2 rely on persistent
 * connections.
 * <h3>Resource management</h3>
 * It is recommended to use the <code>getAndMapXXX</code> and <code>postAndMapXXX</code> methods
 * in this class since they
 * ensure that the underlying network resources are properly released.
 * The method {@link #getAsInputStream} gives access to an input stream on the body response but
 * requires the caller to close this stream. For most use cases, this method is not recommended .
 * <h3>Connection Pooling</h3>
 * The connection pool holds a maximum of 20 connections, with maximum 2 connections per host.
 * <h3>Thread-safety</h3>
 * Instances of this class are thread-safe.
 */
public class OtpHttpClient implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(OtpHttpClient.class);
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

  private final CloseableHttpClient httpClient;

  /**
   * Creates an HTTP client with default timeout and unlimited connection time-to-live.
   */
  public OtpHttpClient() {
    this(DEFAULT_TIMEOUT, null);
  }

  /**
   * Creates an HTTP client with the provided timeout and connection time-to-live.
   */
  public OtpHttpClient(Duration timeout, Duration connectionTtl) {
    Objects.requireNonNull(timeout);
    HttpClientBuilder httpClientBuilder = HttpClients
      .custom()
      .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout((int) timeout.toMillis()).build())
      .setDefaultRequestConfig(requestConfig(timeout));

    if (connectionTtl != null) {
      httpClientBuilder.setConnectionTimeToLive(connectionTtl.toSeconds(), TimeUnit.SECONDS);
    }
    httpClient = httpClientBuilder.build();
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
      new HttpGet(uri),
      timeout,
      requestHeaderValues,
      response -> {
        if (isFailedRequest(response)) {
          LOG.warn(
            "Headers of resource {} unavailable. HTTP error code {}",
            sanitizeUri(uri),
            response.getStatusLine().getStatusCode()
          );

          return Collections.emptyList();
        }
        if (response.getEntity() == null || response.getEntity().getContent() == null) {
          throw new OtpHttpClientException("HTTP request failed: empty response");
        }
        return Arrays.stream(response.getAllHeaders()).toList();
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
    return getAndMap(
      uri,
      timeout,
      headers,
      is -> {
        try {
          return objectMapper.readValue(is, clazz);
        } catch (Exception e) {
          throw new OtpHttpClientException(e);
        }
      }
    );
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
    return getAndMap(
      uri,
      timeout,
      headers,
      is -> {
        try {
          return objectMapper.readTree(is);
        } catch (Exception e) {
          throw new OtpHttpClientException(e);
        }
      }
    );
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
    URL downloadUrl;
    try {
      downloadUrl = uri.toURL();
    } catch (MalformedURLException e) {
      throw new OtpHttpClientException(e);
    }
    String proto = downloadUrl.getProtocol();
    if (proto.equals("http") || proto.equals("https")) {
      return executeAndMap(new HttpGet(uri), timeout, headers, contentMapper);
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
    HttpRequestBase httpRequest,
    Duration timeout,
    Map<String, String> headers,
    ResponseMapper<T> contentMapper
  ) {
    return executeAndMapWithResponseHandler(
      httpRequest,
      timeout,
      headers,
      response -> {
        if (isFailedRequest(response)) {
          throw new OtpHttpClientException(
            "HTTP request failed with status code " + response.getStatusLine().getStatusCode()
          );
        }
        if (response.getEntity() == null || response.getEntity().getContent() == null) {
          throw new OtpHttpClientException("HTTP request failed: empty response");
        }
        try (InputStream is = response.getEntity().getContent()) {
          return contentMapper.apply(is);
        } catch (Exception e) {
          throw new OtpHttpClientException(e);
        }
      }
    );
  }

  @Override
  public void close() {
    try {
      httpClient.close();
    } catch (IOException e) {
      throw new OtpHttpClientException(e);
    }
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

    HttpRequestBase httpRequest = new HttpGet(uri);
    httpRequest.setConfig(requestConfig(timeout));
    requestHeaders.forEach(httpRequest::addHeader);
    HttpResponse response = httpClient.execute(httpRequest);

    if (isFailedRequest(response)) {
      throw new IOException(
        "Service unavailable: " +
        uri +
        ". HTTP status code: " +
        response.getStatusLine().getStatusCode() +
        " - " +
        response.getStatusLine().getReasonPhrase()
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
    HttpRequestBase httpRequest,
    Duration timeout,
    Map<String, String> requestHeaders,
    final ResponseHandler<? extends T> responseHandler
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

  /**
   * Configures the request with a custom timeout.
   */

  private static RequestConfig requestConfig(Duration timeout) {
    int to = (int) timeout.toMillis();
    return RequestConfig
      .custom()
      .setConnectionRequestTimeout(to)
      .setConnectTimeout(to)
      .setSocketTimeout(to)
      .build();
  }

  /**
   * Returns true if the HTTP status code is not 200.
   */
  private static boolean isFailedRequest(HttpResponse response) {
    return response.getStatusLine().getStatusCode() != 200;
  }

  /**
   * Removes the query part from the URI.
   */
  private static String sanitizeUri(URI uri) {
    return uri.toString().replace('?' + uri.getQuery(), "");
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
