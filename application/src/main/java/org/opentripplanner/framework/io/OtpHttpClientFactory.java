package org.opentripplanner.framework.io;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;

/**
 * Factory for creating {@link OtpHttpClient} instances. These instances will share the same
 * {@link CloseableHttpClient} instance.
 *
 * <h3>Timeout configuration</h3>
 * The same timeout value is applied to the following parameters:
 * <ul>
 *  <li>Connection request timeout: the maximum waiting time for leasing a connection in the
 *  connection pool.
 *  <li>Connect timeout: the maximum waiting time for the first packet received from the server.
 *  <li>Socket timeout: the maximum waiting time between two packets received from the server.
 * </ul>
 * The default timeout is set to 5 seconds.
 * <h3>Connection time-to-live</h3>
 * Maximum time an HTTP connection can stay in the connection pool before being closed.
 * Note that HTTP 1.1 and HTTP/2 rely on persistent connections and the HTTP server is allowed to
 * close idle connections at any time.
 * The default connection time-to-live is set to 1 minute.
 * <h3>Connection Pooling</h3>
 * The connection pool holds by default a maximum of 25 connections, with maximum 5 connections
 * per host.
 *
 * <h3>Thread-safety</h3>
 * Instances of this class are thread-safe.
 */
public class OtpHttpClientFactory implements AutoCloseable {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

  private static final Duration DEFAULT_TTL = Duration.ofMinutes(1);

  /**
   * see {@link PoolingHttpClientConnectionManager#DEFAULT_MAX_TOTAL_CONNECTIONS}
   */
  public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 25;

  private final CloseableHttpClient httpClient;

  /**
   * Creates an HTTP client with default timeout, default connection time-to-live and default max
   * number of connections.
   */
  public OtpHttpClientFactory() {
    this(DEFAULT_TIMEOUT, DEFAULT_TTL);
  }

  /**
   * Creates an HTTP client with default timeout, default connection time-to-live and the given max
   * number of connections.
   */
  public OtpHttpClientFactory(int maxConnections) {
    this(DEFAULT_TIMEOUT, DEFAULT_TTL, maxConnections);
  }

  /**
   * Creates an HTTP client the given timeout and connection time-to-live and the default max
   * number of connections.
   */
  public OtpHttpClientFactory(Duration timeout, Duration connectionTtl) {
    this(timeout, connectionTtl, DEFAULT_MAX_TOTAL_CONNECTIONS);
  }

  /**
   * Creates an HTTP client with custom configuration.
   */
  private OtpHttpClientFactory(Duration timeout, Duration connectionTtl, int maxConnections) {
    Objects.requireNonNull(timeout);
    Objects.requireNonNull(connectionTtl);

    PoolingHttpClientConnectionManager connectionManager =
      PoolingHttpClientConnectionManagerBuilder.create()
        .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(Timeout.of(timeout)).build())
        .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
        .setConnPoolPolicy(PoolReusePolicy.LIFO)
        .setMaxConnTotal(maxConnections)
        .setDefaultConnectionConfig(
          ConnectionConfig.custom()
            .setSocketTimeout(Timeout.of(timeout))
            .setConnectTimeout(Timeout.of(timeout))
            .setTimeToLive(TimeValue.of(connectionTtl))
            .build()
        )
        .build();

    HttpClientBuilder httpClientBuilder = HttpClients.custom()
      .setUserAgent("OpenTripPlanner")
      .setConnectionManager(connectionManager)
      .setDefaultRequestConfig(requestConfig(timeout));

    httpClient = httpClientBuilder.build();
  }

  public OtpHttpClient create(Logger logger) {
    return new OtpHttpClient(httpClient, logger);
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
   * Configures the request with a custom timeout.
   */
  private static RequestConfig requestConfig(Duration timeout) {
    return RequestConfig.custom()
      .setResponseTimeout(Timeout.of(timeout))
      .setConnectionRequestTimeout(Timeout.of(timeout))
      .setProtocolUpgradeEnabled(false)
      .build();
  }
}
