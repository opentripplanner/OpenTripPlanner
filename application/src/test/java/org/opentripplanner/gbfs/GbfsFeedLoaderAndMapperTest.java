package org.opentripplanner.gbfs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;
import org.slf4j.LoggerFactory;

class GbfsFeedLoaderAndMapperTest {

  private static final String TIER_OSLO = "file:src/test/resources/gbfs/tieroslo/gbfs.json";

  /**
   * A caller that has already fetched the auto-configuration file to inspect it - for instance to
   * check whether the system publishes geofencing zones at all - must be able to hand it over
   * rather than have it fetched a second time, since some servers throttle repeated fetches.
   */
  @Test
  void createsALoaderFromAnAlreadyFetchedAutoConfiguration() {
    try (var httpClientFactory = new OtpHttpClientFactory()) {
      var autoConfiguration = GbfsAutoConfiguration.fetch(
        TIER_OSLO,
        HttpHeaders.empty(),
        httpClientFactory.create(LoggerFactory.getLogger(GbfsFeedLoaderAndMapperTest.class))
      );

      var subject = GbfsFeedLoaderAndMapper.create(
        parameters(),
        autoConfiguration,
        httpClientFactory
      );
      subject.update();
      // getUpdated() must be called to trigger geofencing zone mapping internally
      subject.getUpdated();

      assertThat(subject.getGeofencingZones()).isNotEmpty();
    }
  }

  private static GbfsVehicleRentalDataSourceParameters parameters() {
    return new GbfsVehicleRentalDataSourceParameters(
      TIER_OSLO,
      "en",
      false,
      HttpHeaders.empty(),
      "tieroslo",
      true,
      true,
      false,
      RentalPickupType.ALL
    );
  }
}
