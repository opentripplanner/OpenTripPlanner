package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.DATE;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.OFFSET;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_A;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_B;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
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
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.utils.time.TimeUtils;

public class TestRouteData {

  private final Route route;
  private final List<Trip> trips;
  private final Map<Trip, List<StopTime>> stopTimesByTrip = new HashMap<>();
  private final Map<Trip, TripTimes> tripTimesByTrip = new HashMap<>();
  private final Map<Trip, TripSchedule> tripSchedulesByTrip = new HashMap<>();
  private final RaptorTimeTable<TripSchedule> timetable;
  private final TripPattern tripPattern;
  private Trip currentTrip;

  public TestRouteData(Route route, List<RegularStop> stops, List<String> times) {
    final Deduplicator deduplicator = new Deduplicator();
    this.route = route;
    this.trips = times
      .stream()
      .map(it -> parseTripInfo(route.getName(), it, stops, deduplicator))
      .collect(Collectors.toList());

    List<StopTime> stopTimesFistTrip = firstTrip().getStopTimes();
    // Get TripTimes in same order as the trips
    List<TripTimes> tripTimes = trips
      .stream()
      .map(tripTimesByTrip::get)
      .collect(Collectors.toList());

    tripPattern = TripPattern.of(TimetableRepositoryForTest.id("TP:" + route))
      .withRoute(this.route)
      .withStopPattern(new StopPattern(stopTimesFistTrip))
      .withScheduledTimeTableBuilder(builder -> builder.addAllTripTimes(tripTimes))
      .build();

    RoutingTripPattern routingTripPattern = tripPattern.getRoutingTripPattern();

    var patternForDates = new TripPatternForDates(
      routingTripPattern,
      new TripPatternForDate[] {
        new TripPatternForDate(routingTripPattern, tripTimes, List.of(), DATE),
      },
      new int[] { OFFSET },
      null,
      null,
      0
    );
    int id = 0;
    for (Trip trip : trips) {
      var tripSchedule = new TripScheduleWithOffset(patternForDates, id);
      id += 1;
      tripSchedulesByTrip.put(trip, tripSchedule);
    }

    this.timetable = patternForDates;
  }

  public static TestRouteData of(
    String route,
    TransitMode mode,
    List<RegularStop> stops,
    String... times
  ) {
    return new TestRouteData.Builder(route)
      .withMode(mode)
      .withStops(stops)
      .withTimes(Arrays.asList(times))
      .build();
  }

  public static TestRouteData.Builder of(String route, TransitMode mode) {
    return new TestRouteData.Builder(route).withMode(mode);
  }

  public static TestRouteData.Builder bus(String route) {
    return of(route, TransitMode.BUS);
  }

  public static TestRouteData.Builder rail(String route) {
    return of(route, TransitMode.RAIL);
  }

  public static TestRouteData.Builder ferry(String route) {
    return of(route, TransitMode.FERRY);
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
    var trip = Trip.of(TimetableRepositoryForTest.id(route + "-" + stopTimesByTrip.size() + 1))
      .withRoute(this.route)
      .build();
    var stopTimes = stopTimes(trip, stops, tripTimes);
    this.stopTimesByTrip.put(trip, stopTimes);
    this.tripTimesByTrip.put(trip, TripTimesFactory.tripTimes(trip, stopTimes, deduplicator));
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
    return s;
  }

  public static class Builder {

    private final String route;
    private String agency;
    private TransitMode mode = TransitMode.BUS;
    private String submode;
    private List<RegularStop> stops;
    private List<String> times;

    public Builder(String route) {
      this.route = route;
    }

    public Builder withAgency(String agency) {
      this.agency = agency;
      return this;
    }

    public Builder withMode(TransitMode mode) {
      this.mode = mode;
      return this;
    }

    public Builder withStops(List<RegularStop> stops) {
      this.stops = stops;
      return this;
    }

    public List<RegularStop> stops() {
      if (stops == null) {
        withStops(List.of(STOP_A, STOP_B));
      }
      return stops;
    }

    public Builder withTimes(List<String> times) {
      this.times = times;
      return this;
    }

    public List<String> times() {
      if (times == null) {
        var buf = new StringBuilder();
        int t = TimeUtils.time("10:00");
        for (var ignore : stops()) {
          t += 600;
          buf.append(" ").append(TimeUtils.timeToStrLong(t));
        }
        this.times = List.of(buf.substring(1));
      }
      return times;
    }

    public Builder withSubmode(String submode) {
      this.submode = submode;
      return this;
    }

    public TestRouteData build() {
      var routeBuilder = TimetableRepositoryForTest.route(route)
        .withMode(mode)
        .withShortName(route);
      if (agency != null) {
        routeBuilder.withAgency(TimetableRepositoryForTest.agency(agency));
      }
      if (submode != null) {
        routeBuilder.withNetexSubmode(submode);
      }
      return new TestRouteData(routeBuilder.build(), stops(), times());
    }
  }
}
