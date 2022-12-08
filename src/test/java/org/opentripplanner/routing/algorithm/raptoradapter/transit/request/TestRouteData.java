package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.DATE;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.OFFSET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class TestRouteData {

  private final Route route;
  private final List<Trip> trips;
  private final Map<Trip, List<StopTime>> stopTimesByTrip = new HashMap<>();
  private final Map<Trip, TripTimes> tripTimesByTrip = new HashMap<>();
  private final Map<Trip, TripSchedule> tripSchedulesByTrip = new HashMap<>();
  private final RaptorTimeTable<TripSchedule> timetable;
  private final TripPattern tripPattern;
  private Trip currentTrip;

  public TestRouteData(String route, TransitMode mode, List<RegularStop> stops, String... times) {
    final Deduplicator deduplicator = new Deduplicator();
    this.route = TransitModelForTest.route(route).withMode(mode).withShortName(route).build();
    this.trips =
      Arrays
        .stream(times)
        .map(it -> parseTripInfo(route, it, stops, deduplicator))
        .collect(Collectors.toList());

    List<StopTime> stopTimesFistTrip = firstTrip().getStopTimes();
    // Get TripTimes in same order as the trips
    List<TripTimes> tripTimes = trips
      .stream()
      .map(tripTimesByTrip::get)
      .collect(Collectors.toList());

    tripPattern =
      TripPattern
        .of(TransitModelForTest.id("TP:" + route))
        .withRoute(this.route)
        .withStopPattern(new StopPattern(stopTimesFistTrip))
        .build();
    tripTimes.forEach(tripPattern::add);

    RoutingTripPattern routingTripPattern = tripPattern.getRoutingTripPattern();

    var patternForDates = new TripPatternForDates(
      routingTripPattern,
      new TripPatternForDate[] {
        new TripPatternForDate(routingTripPattern, tripTimes, List.of(), DATE),
      },
      new int[] { OFFSET },
      null,
      null
    );
    int id = 0;
    for (Trip trip : trips) {
      var tripSchedule = new TripScheduleWithOffset(patternForDates, id);
      id += 1;
      tripSchedulesByTrip.put(trip, tripSchedule);
    }

    this.timetable = patternForDates;
  }

  public Route getRoute() {
    return route;
  }

  public Trip trip() {
    return currentTrip;
  }

  public TestRouteData firstTrip() {
    this.currentTrip = trips.get(0);
    return this;
  }

  public TestRouteData lastTrip() {
    this.currentTrip = trips.get(trips.size() - 1);
    return this;
  }

  public StopTime getStopTime(RegularStop stop) {
    return stopTimesByTrip.get(currentTrip).get(stopPosition(stop));
  }

  public RaptorTimeTable<TripSchedule> getTimetable() {
    return timetable;
  }

  public TripSchedule getTripSchedule() {
    return tripSchedulesByTrip.get(currentTrip);
  }

  public TripPattern getTripPattern() {
    return tripPattern;
  }

  public int stopPosition(StopLocation stop) {
    List<StopTime> times = firstTrip().getStopTimes();
    for (int i = 0; i < times.size(); ++i) {
      if (stop == times.get(i).getStop()) {
        return i;
      }
    }
    throw new IllegalArgumentException();
  }

  private Trip parseTripInfo(
    String route,
    String tripTimes,
    List<RegularStop> stops,
    Deduplicator deduplicator
  ) {
    var trip = Trip
      .of(TransitModelForTest.id(route + "-" + stopTimesByTrip.size() + 1))
      .withRoute(this.route)
      .build();
    var stopTimes = stopTimes(trip, stops, tripTimes);
    this.stopTimesByTrip.put(trip, stopTimes);
    this.tripTimesByTrip.put(trip, new TripTimes(trip, stopTimes, deduplicator));
    return trip;
  }

  private List<StopTime> getStopTimes() {
    return stopTimesByTrip.get(currentTrip);
  }

  private List<StopTime> stopTimes(Trip trip, List<RegularStop> stops, String timesAsString) {
    var times = TimeUtils.times(timesAsString);
    var stopTimes = new ArrayList<StopTime>();
    for (int i = 0; i < stops.size(); i++) {
      stopTimes.add(stopTime(trip, stops.get(i), times[i], i + 1));
    }
    return stopTimes;
  }

  private StopTime stopTime(Trip trip, RegularStop stop, int time, int seq) {
    var s = new StopTime();
    s.setTrip(trip);
    s.setStop(stop);
    s.setArrivalTime(time);
    s.setDepartureTime(time);
    s.setStopSequence(seq);
    s.setStopHeadsign(new NonLocalizedString("NA"));
    s.setRouteShortName("NA");
    return s;
  }
}
