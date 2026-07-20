package org.opentripplanner.gbfs;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.slf4j.LoggerFactory;

/**
 * Tests that the auto-configuration file (gbfs.json) is fetched only once during setup, for both
 * determining the GBFS version and listing the feeds. Some servers throttle repeated requests for
 * the same file, so fetching it twice made setup fail with a read timeout (issue #7839).
 */
class GbfsFeedLoaderAndMapperTest {

  @Test
  void fetchV30AutoConfigurationOnlyOnce() {
    var client = spyClient();

    var loaderAndMapper = GbfsFeedLoaderAndMapper.create(
      params("file:src/test/resources/gbfs/ridecheck/almere/gbfs.json"),
      factoryOf(client)
    );

    verifySingleAutoConfigurationFetch(client);
    assertTrue(loaderAndMapper.update());
  }

  @Test
  void fetchV23AutoConfigurationOnlyOnce() {
    var client = spyClient();

    var loaderAndMapper = GbfsFeedLoaderAndMapper.create(
      params("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json"),
      factoryOf(client)
    );

    verifySingleAutoConfigurationFetch(client);
    assertTrue(loaderAndMapper.update());
  }

  private static void verifySingleAutoConfigurationFetch(OtpHttpClient client) {
    verify(client, times(1)).getAndMapAsJsonNode(any(), any(), any());
    verify(client, never()).getAndMapAsJsonObject(any(), any(), any(), any());
  }

  private static OtpHttpClient spyClient() {
    return spy(
      new OtpHttpClientFactory().create(LoggerFactory.getLogger(GbfsFeedLoaderAndMapperTest.class))
    );
  }

  private static OtpHttpClientFactory factoryOf(OtpHttpClient client) {
    var factory = mock(OtpHttpClientFactory.class);
    when(factory.create(any())).thenReturn(client);
    return factory;
  }

  private static GbfsVehicleRentalDataSourceParameters params(String url) {
    return new GbfsVehicleRentalDataSourceParameters(
      url,
      "nb",
      false,
      HttpHeaders.empty(),
      "test-network",
      false,
      true,
      false,
      Set.of()
    );
  }
}
