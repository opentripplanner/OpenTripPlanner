package org.opentripplanner.updater.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsRealtimeExtensions;

class GtfsRealtimeHttpTripUpdateSourceTest {

  @Test
  void parseExtension() {
    var source = new GtfsRealtimeHttpTripUpdateSource(
      new GtfsRealtimeHttpTripUpdateSource.Parameters() {
        @Override
        public String getFeedId() {
          return "1";
        }

        @Override
        public String getUrl() {
          return "file:src/test/resources/gtfs-rt/otp-extensions.pbf";
        }
      }
    );

    var tripUpdates = source.getUpdates();
    assertEquals(44, tripUpdates.size());

    var first = tripUpdates.get(0);

    assertTrue(
      first
        .getStopTimeUpdateList()
        .get(0)
        .getStopTimeProperties()
        .hasExtension(GtfsRealtimeExtensions.stopTimeProperties)
    );

    assertTrue(first.getTrip().hasExtension(GtfsRealtimeExtensions.tripDescriptor));
  }
}
