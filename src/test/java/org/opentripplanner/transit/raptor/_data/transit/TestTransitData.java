package org.opentripplanner.transit.raptor._data.transit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.val;
import org.opentripplanner.transit.raptor._data.debug.TestDebugLogger;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;

@SuppressWarnings("UnusedReturnValue")
public class TestTransitData implements RaptorTransitDataProvider<TestTripSchedule> {

  private final List<List<RaptorTransfer>> transfersByStop = new ArrayList<>();
  private final List<Set<TestRoute>> routesByStop = new ArrayList<>();
  private final List<TestRoute> routes = new ArrayList<>();

  @Override
  public Iterator<? extends RaptorTransfer> getTransfers(int fromStop) {
    return transfersByStop.get(fromStop).iterator();
  }

  @Override
  public Iterator<? extends RaptorRoute<TestTripSchedule>> routeIterator(IntIterator stops) {
    Set<RaptorRoute<TestTripSchedule>> routes = new HashSet<>();
    while (stops.hasNext()) {
      int stop = stops.next();
      routes.addAll(routesByStop.get(stop));
    }
    return routes.iterator();
  }

  @Override
  public int numberOfStops() {
    return routesByStop.size();
  }

  @Override
  public int[] stopBoarAlightCost() {
    // Not implemented, no test for this yet.
    return null;
  }

  public void debugToStdErr(RaptorRequestBuilder<TestTripSchedule> request) {
    List<Integer> stops = new ArrayList<>();
    for (int i = 0; i < numberOfStops(); i++) { stops.add(i); }
    val logger = new TestDebugLogger(true);
    request.debug().addStops(stops)
        .stopArrivalListener(logger::stopArrivalLister)
        .patternRideDebugListener(logger::patternRideLister)
        .pathFilteringListener(logger::pathFilteringListener)
        .logger(logger);
  }

  public TestTransitData withRoute(TestRoute route) {
    this.routes.add(route);
    var pattern = route.pattern();
    for(int i=0; i< pattern.numberOfStopsInPattern(); ++i) {
      int stopIndex = pattern.stopIndex(i);
      expandNumOfStops(stopIndex);
      routesByStop.get(stopIndex).add(route);
    }
    return this;
  }

  public TestTransitData withRoutes(TestRoute ... routes) {
    for (TestRoute route : routes) {
      withRoute(route);
    }
    return this;
  }

  public TestTransitData withGuaranteedTransfers(
          TestTripSchedule fromTrip, int fromStopPos,
          TestTripSchedule toTrip, int toStopPos
  ) {
    for (TestRoute route : routes) {
      for (int i = 0; i < route.timetable().numberOfTripSchedules(); i++) {
        var trip = route.timetable().getTripSchedule(i);
        if(fromTrip == trip) {
          route.addGuaranteedTxFrom(trip, i, fromStopPos, toTrip, toStopPos);
        }
        if(toTrip == trip) {
          route.addGuaranteedTxTo(fromTrip, fromStopPos, trip, i, toStopPos);
        }
      }
    }
    return this;
  }

  private void expandNumOfStops(int stopIndex) {
    for (int i=numberOfStops(); i<=stopIndex; ++i) {
      transfersByStop.add(new ArrayList<>());
      routesByStop.add(new HashSet<>());
    }
  }
}
