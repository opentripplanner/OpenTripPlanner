package org.opentripplanner.updater.trip.moduletests.rejection;

import static org.opentripplanner.test.support.UpdateResultAssertions.assertFailure;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_INPUT_STRUCTURE;

import com.google.transit.realtime.GtfsRealtime;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;

class InvalidTripIdTest {

  static Stream<String> invalidCases() {
    return Stream.of(null, "", "  ");
  }

  /**
   * This test just asserts that invalid trip ids don't throw an exception and are ignored instead
   */
  @ParameterizedTest(name = "tripId=\"{0}\"")
  @MethodSource("invalidCases")
  void invalidTripId(String tripId) {
    var env = RealtimeTestEnvironment.gtfs();
    var tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
    if (tripId != null) {
      tripDescriptorBuilder.setTripId(tripId);
    }
    tripDescriptorBuilder.setScheduleRelationship(
      GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED
    );
    var tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    var tripUpdate = tripUpdateBuilder.build();

    var result = env.applyTripUpdate(tripUpdate);

    assertFailure(INVALID_INPUT_STRUCTURE, result);
  }
}
