package org.opentripplanner.jags.edgetype.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.Alight;
import org.opentripplanner.jags.edgetype.PatternBoard;
import org.opentripplanner.jags.edgetype.PatternHop;
import org.opentripplanner.jags.edgetype.TripPattern;
import org.opentripplanner.jags.edgetype.Walkable;
import org.opentripplanner.jags.gtfs.GtfsContext;

class StopPattern {
    Vector<Stop> stops;

    Vector<Long> times;

    AgencyAndId calendarId;

    public StopPattern(Vector<Stop> stops, Vector<Long> times, AgencyAndId calendarId) {
        this.stops = stops;
        this.times = times;
        this.calendarId = calendarId;
    }

    public boolean equals(Object other) {
        if (other instanceof StopPattern) {
            StopPattern pattern = (StopPattern) other;
            return pattern.stops.equals(stops) && pattern.times.equals(times)
                    && pattern.calendarId.equals(calendarId);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.stops.hashCode() ^ this.times.hashCode() ^ this.calendarId.hashCode();
    }

    public String toString() {
        return "StopPattern(" + stops + ", " + times + ", " + calendarId + ")";
    }
}


public class GTFSPatternHopFactory {

    private GtfsRelationalDao _dao;

    public GTFSPatternHopFactory(GtfsContext context) throws Exception {
        _dao = context.getDao();
    }

    public static StopPattern stopPatternfromTrip(Trip trip, GtfsRelationalDao dao) {
        Vector<Stop> stops = new Vector<Stop>();
        Vector<Long> times = new Vector<Long>();
        int start = -1;

        for (StopTime stoptime : dao.getStopTimesForTrip(trip)) {
            stops.add(stoptime.getStop());
            if (start == -1) {
                start = stoptime.getArrivalTime();
            }
            long crossingTime = stoptime.getArrivalTime() - start;
            times.add(crossingTime);
            long dwellTime = stoptime.getDepartureTime() - stoptime.getArrivalTime();
            times.add(dwellTime);
        }
        StopPattern pattern = new StopPattern(stops, times, trip.getServiceId());
        return pattern;
    }

    private String id(AgencyAndId id) {
        return id.getAgencyId() + "_" + id.getId();
    }

    public ArrayList<Walkable> run(Graph graph) throws Exception {

        ArrayList<Walkable> ret = new ArrayList<Walkable>();

        // Load hops
        Collection<Trip> trips = _dao.getAllTrips();

        HashMap<StopPattern, TripPattern> patterns = new HashMap<StopPattern, TripPattern>();

        for (Trip trip : trips) {
            List<StopTime> stopTimes = _dao.getStopTimesForTrip(trip);
            StopPattern stopPattern = stopPatternfromTrip(trip, _dao);
            TripPattern tripPattern = patterns.get(stopPattern);
            if (tripPattern == null) {
                tripPattern = new TripPattern(trip, stopTimes);
                int lastStop = stopTimes.size() - 1;
                for (int i = 0; i < lastStop; i++) {
                    StopTime st0 = stopTimes.get(i);
                    Stop s0 = st0.getStop();
                    StopTime st1 = stopTimes.get(i + 1);
                    Stop s1 = st1.getStop();
                    int runningTime = st1.getArrivalTime() - st0.getDepartureTime();

                    PatternHop hop = new PatternHop(s0, s1, runningTime,
                            tripPattern);
                    ret.add(hop);

                    Vertex startStation = graph.getVertex(id(s0.getId()));
                    Vertex endStation = graph.getVertex(id(s1.getId()));

                    // create journey vertices
                    Vertex startJourney = graph.addVertex(id(s0.getId()) + "_" + id(trip.getId()));
                    Vertex endJourney = graph.addVertex(id(s1.getId()) + "_" + id(trip.getId()));

                    PatternBoard boarding = new PatternBoard(tripPattern, s0);
                    graph.addEdge(startStation, startJourney, boarding);
                    graph.addEdge(endJourney, endStation, new Alight());
                    graph.addEdge(startJourney, endJourney, hop);
                }
                patterns.put(stopPattern, tripPattern);
            }
            int firstDepartureTime = stopTimes.get(0).getDepartureTime();
            tripPattern.addStartTime(firstDepartureTime);
        }

        return ret;
    }
}
