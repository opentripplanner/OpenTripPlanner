package org.opentripplanner.updater.trip.gtfs.updater.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.trip.gtfs.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.ForwardsDelayPropagationType;

public class GtfsRealtimeTripUpdateSourceTest {

  @Test
  public void parseFeed() {
    var source = new HttpTripUpdateSource(
      new PollingTripUpdaterParameters(
        "rt",
        Duration.ofSeconds(10),
        false,
        ForwardsDelayPropagationType.DEFAULT,
        BackwardsDelayPropagationType.ALWAYS,
        "rt",
        ResourceLoader.of(this).url("septa.pbf").toString(),
        HttpHeaders.empty()
      )
    );
    var updates = source.getUpdates();

    assertNotNull(updates);

    assertEquals(35, updates.size());

    var first = updates.get(0);
    assertEquals("AIR_4846_V55_M", first.getTrip().getTripId());
  }
}
