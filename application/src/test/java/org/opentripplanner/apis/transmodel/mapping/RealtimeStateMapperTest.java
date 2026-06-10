package org.opentripplanner.apis.transmodel.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.transmodel.model.TransmodelRealTimeState.ADDED;
import static org.opentripplanner.apis.transmodel.model.TransmodelRealTimeState.CANCELED;
import static org.opentripplanner.apis.transmodel.model.TransmodelRealTimeState.MODIFIED;
import static org.opentripplanner.apis.transmodel.model.TransmodelRealTimeState.SCHEDULED;
import static org.opentripplanner.apis.transmodel.model.TransmodelRealTimeState.UPDATED;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TripTimesForTest;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;

class RealtimeStateMapperTest {

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
   * DELETED is an internal soft-deletion state not exposed in the Transmodel API; it maps to
   * CANCELED.
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
}
