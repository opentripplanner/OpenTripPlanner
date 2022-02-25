package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.*;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.util.time.TimeUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.opentripplanner.routing.algorithm.raptor.transit.request.TestTransitCaseData.*;

public class TestRouteData {

    private final Route route;
    private final List<Trip> trips;
    private final Map<Trip, List<StopTime>> stopTimesByTrip = new HashMap<>();
    private final Map<Trip, TripTimes> tripTimesByTrip = new HashMap<>();
    private final Map<Trip, TripSchedule> tripSchedulesByTrip = new HashMap<>();
    private final RaptorTimeTable<TripSchedule> timetable;
    private final TripPatternWithRaptorStopIndexes raptorTripPattern;
    private Trip currentTrip;

    public TestRouteData(String route, TransitMode mode, List<Stop> stops, String ... times) {
        final Deduplicator deduplicator = new Deduplicator();
        this.route = new Route(id(route));
        this.route.setMode(mode);
        this.trips = Arrays.stream(times)
                .map(it -> parseTripInfo(route, it, stops, deduplicator))
                .collect(Collectors.toList());

        List<StopTime> stopTimesFistTrip = firstTrip().getStopTimes();
        // Get TripTimes in same order as the trips
        List<TripTimes> tripTimes = trips.stream()
                .map(tripTimesByTrip::get)
                .collect(Collectors.toList());

        raptorTripPattern = new TripPatternWithRaptorStopIndexes(
                new TripPattern(id("TP:"+route), this.route, new StopPattern(stopTimesFistTrip)),
                stopIndexes(stopTimesFistTrip)
        );
        tripTimes.forEach(t -> raptorTripPattern.getPattern().add(t));

        var listOfTripPatternForDates = List.of(
                new TripPatternForDate(raptorTripPattern, tripTimes, DATE)
        );

        var patternForDates = new TripPatternForDates(
                raptorTripPattern, listOfTripPatternForDates, List.of(OFFSET)
        );
        for (Trip trip : trips) {
            var tripSchedule = new TripScheduleWithOffset(
                    patternForDates, DATE, tripTimesByTrip.get(trip), OFFSET
            );
            tripSchedulesByTrip.put(trip, tripSchedule);
        }

        this.timetable = patternForDates;
    }

    private Trip parseTripInfo(String route, String tripTimes, List<Stop> stops, Deduplicator deduplicator) {
        var trip = new Trip(id(route + "-" + stopTimesByTrip.size()+1));
        trip.setRoute(this.route);
        var stopTimes = stopTimes(trip, stops, tripTimes);
        this.stopTimesByTrip.put(trip, stopTimes);
        this.tripTimesByTrip.put(trip, new TripTimes(trip, stopTimes, deduplicator));
        return trip;
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
        this.currentTrip = trips.get(trips.size()-1);
        return this;
    }

    public StopTime getStopTime(Stop stop) {
        return stopTimesByTrip.get(currentTrip).get(stopPosition(stop));
    }

    public RaptorTimeTable<TripSchedule> getTimetable() {
        return timetable;
    }

    public TripSchedule getTripSchedule() {
        return tripSchedulesByTrip.get(currentTrip);
    }

    public TripPatternWithRaptorStopIndexes getRaptorTripPattern() {
        return raptorTripPattern;
    }

    private List<StopTime> getStopTimes() {
        return stopTimesByTrip.get(currentTrip);
    }

    int[] stopIndexes(Collection<StopTime> times) {
        return times.stream().mapToInt(it -> stopIndex(it.getStop())).toArray();
    }

    public int stopPosition(StopLocation stop) {
        List<StopTime> times = firstTrip().getStopTimes();
        for (int i=0; i<times.size(); ++i) {
            if(stop == times.get(i).getStop()) { return i; }
        }
        throw new IllegalArgumentException();
    }

    private List<StopTime> stopTimes(Trip trip, List<Stop> stops, String timesAsString) {
        var times = TimeUtils.times(timesAsString);
        var stopTimes = new ArrayList<StopTime>();
        for (int i = 0; i < stops.size(); i++) {
            stopTimes.add(stopTime(trip, stops.get(i), times[i], i+1));
        }
        return stopTimes;
    }

    private StopTime stopTime(Trip trip, Stop stop, int time, int seq) {
        var s = new StopTime();
        s.setTrip(trip);
        s.setStop(stop);
        s.setArrivalTime(time);
        s.setDepartureTime(time);
        s.setStopSequence(seq);
        s.setStopHeadsign("NA");
        s.setRouteShortName("NA");
        return s;
    }
}
