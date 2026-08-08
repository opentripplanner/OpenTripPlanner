package org.opentripplanner.gbfs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.slf4j.LoggerFactory;

class GbfsAutoConfigurationTest {

  private static final OtpHttpClient OTP_HTTP_CLIENT = new OtpHttpClientFactory().create(
    LoggerFactory.getLogger(GbfsAutoConfigurationTest.class)
  );

  @Test
  void listsFeedNamesOfAV2Feed() {
    var subject = fetch("file:src/test/resources/gbfs/tieroslo/gbfs.json");

    assertThat(subject.feedNames()).containsExactly("system_information", "geofencing_zones");
  }

  @Test
  void listsFeedNamesOfAV3Feed() {
    var subject = fetch("file:src/test/resources/gbfs/duplicate-stations-v3/gbfs.json");

    assertThat(subject.feedNames()).containsExactly(
      "system_information",
      "station_information",
      "station_status"
    );
  }

  @Test
  void feedNamesDoesNotContainAFeedThatIsNotPublished() {
    var subject = fetch("file:src/test/resources/gbfs/duplicate-stations-v3/gbfs.json");

    assertThat(subject.feedNames()).doesNotContain("geofencing_zones");
  }

  private static GbfsAutoConfiguration fetch(String url) {
    return GbfsAutoConfiguration.fetch(url, HttpHeaders.empty(), OTP_HTTP_CLIENT);
  }
}
