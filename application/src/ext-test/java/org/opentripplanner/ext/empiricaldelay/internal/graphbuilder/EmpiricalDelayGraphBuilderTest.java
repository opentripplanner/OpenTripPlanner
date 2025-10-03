package org.opentripplanner.ext.empiricaldelay.internal.graphbuilder;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class EmpiricalDelayGraphBuilderTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final Route ROUTE_1 = TimetableRepositoryForTest.route("route1").build();
  private static final Route ROUTE_2 = TimetableRepositoryForTest.route("route2").build();

  private static final RegularStop STOP_A = TEST_MODEL.stop("STOP-A", 1, 1).build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("STOP-B", 2, 1).build();
  private static final RegularStop STOP_C = TEST_MODEL.stop("STOP-C", 3, 1).build();

  private static final String FEED_ID = "F";

  @Test
  void createStopIdsByTripIdMap() {
    var trip = TimetableRepositoryForTest.trip("Trip-A").build(); //.withServiceId(new FeedScopedId("F", "2"))

    var stopTimes = List.of(
      TEST_MODEL.stopTime(trip, 0, STOP_A),
      TEST_MODEL.stopTime(trip, 1, STOP_B),
      TEST_MODEL.stopTime(trip, 2, STOP_C)
    );
    var stopPattern = new StopPattern(stopTimes);

    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
    var pattern = TripPattern.of(trip.getId())
      .withRoute(trip.getRoute())
      .withStopPattern(stopPattern)
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();

    var map = EmpiricalDelayGraphBuilder.createStopIdsByTripIdMap(List.of(pattern));

    var stops = map.get(trip.getId());
    assertEquals(stopTimes.get(0).getStop().getId(), stops.get(0));
    assertEquals(stopTimes.get(1).getStop().getId(), stops.get(1));
    assertEquals(stopTimes.get(2).getStop().getId(), stops.get(2));
  }
}
