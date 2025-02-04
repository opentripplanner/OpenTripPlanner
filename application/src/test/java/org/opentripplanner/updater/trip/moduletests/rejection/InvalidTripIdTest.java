package org.opentripplanner.updater.trip.moduletests.rejection;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

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
    var env = RealtimeTestEnvironment.of().build();
    var tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();
    if (tripId != null) {
      tripDescriptorBuilder.setTripId(tripId);
    }
    tripDescriptorBuilder.setScheduleRelationship(SCHEDULED);
    var tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    var tripUpdate = tripUpdateBuilder.build();

    assertFailure(INVALID_INPUT_STRUCTURE, env.applyTripUpdate(tripUpdate));
  }
}
