package org.opentripplanner.updater.vehicle_rental.datasources.gbfs;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.io.TestHttpClientFactory;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.UpdaterConstructionException;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

class GbfsVehicleRentalDataSourceTest {

  /**
   * Test that a network or HTTP error during setup is correctly rethrown as an
   * {@link UpdaterConstructionException}, which leds OTP to start up anyway rather than shutting down completely.
   */
  @Test
  void rethrowCorrectExceptionDuringSetup() {
    var p = new GbfsVehicleRentalDataSourceParameters(
      "http://localhost:8080/gbfs.json",
      "en",
      true,
      HttpHeaders.empty(),
      "n",
      false,
      false,
      Set.of()
    );
    var s = new GbfsVehicleRentalDataSource(p, TestHttpClientFactory.failingHttpFactory());
    assertThrows(UpdaterConstructionException.class, s::setup);
  }
}
