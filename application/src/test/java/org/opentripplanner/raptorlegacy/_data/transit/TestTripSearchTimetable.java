package org.opentripplanner.raptorlegacy._data.transit;

import java.util.function.IntUnaryOperator;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripScheduleSearchFactory;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripSearchTimetable;

/**
 *
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
public class TestTripSearchTimetable implements TripSearchTimetable<TestTripSchedule> {

  private final TestTripSchedule[] trips;

  public TestTripSearchTimetable(TestRoute route) {
    int nTrips = route.timetable().numberOfTripSchedules();
    this.trips = new TestTripSchedule[nTrips];

    for (int i = 0; i < nTrips; ++i) {
      trips[i] = route.getTripSchedule(i);
    }
  }

  @Override
  public TestTripSchedule getTripSchedule(int index) {
    return trips[index];
  }

  @Override
  public int numberOfTripSchedules() {
    return trips.length;
  }

  @Override
  public IntUnaryOperator getArrivalTimes(int stopPositionInPattern) {
    return (int tripIndex) -> trips[tripIndex].arrival(stopPositionInPattern);
  }

  @Override
  public IntUnaryOperator getDepartureTimes(int stopPositionInPattern) {
    return (int tripIndex) -> trips[tripIndex].departure(stopPositionInPattern);
  }

  @Override
  public RaptorTripScheduleSearch<TestTripSchedule> tripSearch(SearchDirection direction) {
    return TripScheduleSearchFactory.create(direction, this);
  }
}
