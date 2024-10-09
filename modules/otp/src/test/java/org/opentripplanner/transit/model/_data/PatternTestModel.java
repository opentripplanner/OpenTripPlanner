package org.opentripplanner.transit.model._data;

import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.StopModel;

public class PatternTestModel {

  public static final Route ROUTE_1 = TransitModelForTest.route("1").build();

  private static final FeedScopedId SERVICE_ID = id("service");
  private static final Trip TRIP = TransitModelForTest
    .trip("t1")
    .withRoute(ROUTE_1)
    .withServiceId(SERVICE_ID)
    .build();
  private static final TransitModelForTest MODEL = new TransitModelForTest(StopModel.of());
  private static final RegularStop STOP_1 = MODEL.stop("1").build();
  private static final StopPattern STOP_PATTERN = TransitModelForTest.stopPattern(STOP_1, STOP_1);

  /**
   * Creates a trip pattern that has a stop pattern, trip times and a trip with a service id.
   */
  public static TripPattern pattern() {
    var tt = ScheduledTripTimes
      .of()
      .withTrip(TRIP)
      .withArrivalTimes("10:00 10:05")
      .withDepartureTimes("10:00 10:05")
      .build();

    return TransitModelForTest
      .tripPattern("1", ROUTE_1)
      .withStopPattern(STOP_PATTERN)
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tt))
      .build();
  }
}
