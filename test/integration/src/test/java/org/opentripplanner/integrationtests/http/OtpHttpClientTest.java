package org.opentripplanner.integrationtests.http;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This integration test makes sure that Apache HTTP client still works with important hosts and
 * HTTP servers.
 */
@Tag("integration")
class OtpHttpClientTest {

  private static final Logger LOG = LoggerFactory.getLogger(OtpHttpClientTest.class);
  private static final OtpHttpClient OTP_HTTP_CLIENT = new OtpHttpClientFactory().create(LOG);

  @ParameterizedTest
  @ValueSource(
    strings = {
      // a few entur URLs
      "https://api.entur.io/mobility/v2/gbfs/",
      "https://storage.googleapis.com/marduk-production/outbound/gtfs/rb_sjn-aggregated-gtfs.zip",
      // Apache HTTP Client broke handling of S3 SSL certificates previously
      "https://s3.amazonaws.com/kcm-alerts-realtime-prod/tripupdates.pb",
    }
  )
  void httpGetRequest(String url) throws IOException {
    var uri = UriBuilder.fromUri(url).build();

    var stream = OTP_HTTP_CLIENT.getAsInputStream(uri, Duration.ofSeconds(30), Map.of());
    var bytes = IOUtils.toByteArray(stream);

    assertNotEquals(0, bytes.length, "Empty response body for %s".formatted(url));
  }
}
