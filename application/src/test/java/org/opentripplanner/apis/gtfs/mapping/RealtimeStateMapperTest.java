package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLRealtimeState.ADDED;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLRealtimeState.CANCELED;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLRealtimeState.MODIFIED;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLRealtimeState.SCHEDULED;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLRealtimeState.UPDATED;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model._data.TripTimesForTest;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.TripTimes;

class RealtimeStateMapperTest {

  private static final TripPattern PATTERN = TransitRepositoryForTest.tripPattern(
    "P",
    TransitRepositoryForTest.route("R").build()
  )
    .withStopPattern(TransitRepositoryForTest.of().stopPattern(2))
    .build();

  // ---- ScheduledTripTimes -----------------------------------------------

  @Test
  void scheduledTripTimesReturnsScheduled() {
    assertEquals(SCHEDULED, RealtimeStateMapper.map(TripTimesForTest.scheduled()));
  }

  // ---- RealTimeTripTimes: no flags set ----------------------------------

  /**
   * A RealTimeTripTimes built without calling any state method (all flags false,
   * realTimeUpdated=false) is logically identical to a scheduled trip.
   */
  @Test
  void realTimeTripTimesWithNoFlagsReturnsScheduled() {
    assertEquals(SCHEDULED, RealtimeStateMapper.map(TripTimesForTest.realTime(b -> {})));
  }

  // ---- RealTimeTripTimes: single-flag cases -----------------------------

  @Test
  void updatedReturnsUpdated() {
    assertEquals(
      UPDATED,
      RealtimeStateMapper.map(
        TripTimesForTest.realTime(RealTimeTripTimesBuilder::withRealTimeUpdated)
      )
    );
  }

  @Test
  void addedReturnsAdded() {
    assertEquals(
      ADDED,
      RealtimeStateMapper.map(TripTimesForTest.realTime(RealTimeTripTimesBuilder::withAdded))
    );
  }

  @Test
  void modifiedReturnsModified() {
    assertEquals(
      MODIFIED,
      RealtimeStateMapper.map(
        TripTimesForTest.realTime(RealTimeTripTimesBuilder::withModifiedTripPattern)
      )
    );
  }

  @Test
  void canceledReturnsCanceled() {
    assertEquals(
      CANCELED,
      RealtimeStateMapper.map(TripTimesForTest.realTime(RealTimeTripTimesBuilder::withCanceled))
    );
  }

  /**
   * DELETED is an internal soft-deletion state not exposed in the API; it maps to CANCELED.
   */
  @Test
  void deletedReturnsCanceled() {
    assertEquals(
      CANCELED,
      RealtimeStateMapper.map(TripTimesForTest.realTime(RealTimeTripTimesBuilder::withDeleted))
    );
  }

  // ---- Priority ordering ------------------------------------------------

  /**
   * CANCELED takes priority over ADDED. In practice, SIRI's xsd:choice and GTFS-RT's single enum
   * field prevent both flags from being set by a single message, but the mapper must handle any
   * combination defensively.
   */
  @Test
  void canceledTakesPriorityOverAdded() {
    assertEquals(
      CANCELED,
      RealtimeStateMapper.map(
        TripTimesForTest.realTime(b -> {
          b.withAdded();
          b.withCanceled();
        })
      )
    );
  }

  /**
   * DELETED takes priority over MODIFIED.
   */
  @Test
  void deletedTakesPriorityOverModified() {
    assertEquals(
      CANCELED,
      RealtimeStateMapper.map(
        TripTimesForTest.realTime(b -> {
          b.withModifiedTripPattern();
          b.withDeleted();
        })
      )
    );
  }

  /**
   * ADDED takes priority over MODIFIED.
   */
  @Test
  void addedTakesPriorityOverModified() {
    assertEquals(
      ADDED,
      RealtimeStateMapper.map(
        TripTimesForTest.realTime(b -> {
          b.withAdded();
          b.withModifiedTripPattern();
        })
      )
    );
  }

  /**
   * CANCELED takes priority over MODIFIED.
   */
  @Test
  void canceledTakesPriorityOverModified() {
    assertEquals(
      CANCELED,
      RealtimeStateMapper.map(
        TripTimesForTest.realTime(b -> {
          b.withModifiedTripPattern();
          b.withCanceled();
        })
      )
    );
  }

  // ---- Per stop state ---------------------------------------------------

  @Test
  void stopStateIsTripStateIfPatternIsNotModified() {
    var tripTimes = TripTimesForTest.realTime(b -> {
      b.withRealTimeUpdated();
      b.withExtraCall(1, true);
    });
    assertEquals(UPDATED, RealtimeStateMapper.map(stopTime(tripTimes, 0)));
    assertEquals(UPDATED, RealtimeStateMapper.map(stopTime(tripTimes, 1)));
  }

  @Test
  void stopStateOfModifiedPattern() {
    var tripTimes = TripTimesForTest.realTime(b -> {
      b.withModifiedTripPattern();
      b.withNoData(0);
      b.withExtraCall(1, true);
    });
    assertEquals(SCHEDULED, RealtimeStateMapper.map(stopTime(tripTimes, 0)));
    assertEquals(ADDED, RealtimeStateMapper.map(stopTime(tripTimes, 1)));
  }

  @Test
  void noDataExtraCallIsAdded() {
    var tripTimes = TripTimesForTest.realTime(b -> {
      b.withModifiedTripPattern();
      b.withNoData(0);
      b.withExtraCall(0, true);
    });
    assertEquals(ADDED, RealtimeStateMapper.map(stopTime(tripTimes, 0)));
  }

  @Test
  void keptStopOfModifiedPatternWithRealTimeIsUpdated() {
    var tripTimes = TripTimesForTest.realTime(RealTimeTripTimesBuilder::withModifiedTripPattern);
    assertEquals(UPDATED, RealtimeStateMapper.map(stopTime(tripTimes, 0)));
  }

  @Test
  void stopStateOfAddedTripIsAdded() {
    var tripTimes = TripTimesForTest.realTime(b -> {
      b.withAdded();
      b.withModifiedTripPattern();
    });
    assertEquals(ADDED, RealtimeStateMapper.map(stopTime(tripTimes, 0)));
  }

  @Test
  void stopStateOfCanceledTripIsCanceled() {
    var tripTimes = TripTimesForTest.realTime(b -> {
      b.withModifiedTripPattern();
      b.withCanceled();
    });
    assertEquals(CANCELED, RealtimeStateMapper.map(stopTime(tripTimes, 0)));
  }

  @Test
  void canceledExtraCallIsCanceled() {
    var tripTimes = TripTimesForTest.realTime(b -> {
      b.withModifiedTripPattern();
      b.withCanceled(0);
      b.withExtraCall(0, true);
    });
    assertEquals(CANCELED, RealtimeStateMapper.map(stopTime(tripTimes, 0)));
    assertEquals(UPDATED, RealtimeStateMapper.map(stopTime(tripTimes, 1)));
  }

  @Test
  void canceledStopOfUnmodifiedTripIsCanceled() {
    var tripTimes = TripTimesForTest.realTime(b -> {
      b.withRealTimeUpdated();
      b.withCanceled(1);
    });
    assertEquals(UPDATED, RealtimeStateMapper.map(stopTime(tripTimes, 0)));
    assertEquals(CANCELED, RealtimeStateMapper.map(stopTime(tripTimes, 1)));
  }

  private static TripTimeOnDate stopTime(TripTimes<?> tripTimes, int stopPos) {
    return new TripTimeOnDate(tripTimes, stopPos, PATTERN);
  }
}
