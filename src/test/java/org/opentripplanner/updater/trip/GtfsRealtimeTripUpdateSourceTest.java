package org.opentripplanner.updater.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.spi.HttpHeaders;

public class GtfsRealtimeTripUpdateSourceTest {

  @Test
  public void parseFeed() {
    var source = new GtfsRealtimeTripUpdateSource(
      new PollingTripUpdaterParameters(
        "rt",
        Duration.ofSeconds(10),
        false,
        BackwardsDelayPropagationType.ALWAYS,
        "rt",
        "file:src/test/resources/gtfs-rt/trip-updates/septa.pbf",
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
