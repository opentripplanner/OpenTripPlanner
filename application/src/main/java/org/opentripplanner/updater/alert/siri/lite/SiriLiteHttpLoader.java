package org.opentripplanner.updater.alert.siri.lite;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.support.siri.SiriHelper;
import org.opentripplanner.updater.support.siri.SiriLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

/**
 * Load real-time updates from SIRI-SX and SIRI-ET feeds over HTTP via a single request
 * that contains all updates.
 */
public class SiriLiteHttpLoader implements SiriLoader {

  private static final Logger LOG = LoggerFactory.getLogger(SiriLiteHttpLoader.class);
  private final HttpHeaders headers;
  private final URI uri;
  private final Duration timeout;
  private final OtpHttpClient otpHttpClient;

  public SiriLiteHttpLoader(URI uri, Duration timeout, HttpHeaders headers) {
    this.uri = uri;
    this.timeout = timeout;
    this.headers = HttpHeaders.of().acceptApplicationXML().add(headers).build();
    this.otpHttpClient = new OtpHttpClientFactory(timeout, timeout).create(LOG);
  }

  /**
   * Send a HTTP GET request and unmarshal the response as JAXB.
   */
  @Override
  public Optional<Siri> fetchSXFeed(String ignored) {
    return fetchFeed();
  }

  /**
   * Send a HTTP GET service request and unmarshal the response as JAXB.
   */
  @Override
  public Optional<Siri> fetchETFeed(String ignored) {
    return fetchFeed();
  }

  private Optional<Siri> fetchFeed() {
    return otpHttpClient.getAndMap(uri, timeout, headers.asMap(), is ->
      Optional.of(SiriHelper.unmarshal(is))
    );
  }
}
