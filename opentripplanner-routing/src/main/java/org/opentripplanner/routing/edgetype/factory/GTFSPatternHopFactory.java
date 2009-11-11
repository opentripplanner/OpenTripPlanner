package org.opentripplanner.routing.edgetype.factory;

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
import org.opentripplanner.jags.edgetype.Traversable;
import org.opentripplanner.jags.edgetype.TripPattern;
import org.opentripplanner.jags.gtfs.GtfsContext;

class StopPattern2 {
    Vector<Stop> stops;

    AgencyAndId calendarId;

    public StopPattern2(Vector<Stop> stops, AgencyAndId calendarId) {
        this.stops = stops;
        this.calendarId = calendarId;
    }

    public boolean equals(Object other) {
        if (other instanceof StopPattern2) {
            StopPattern2 pattern = (StopPattern2) other;
            return pattern.stops.equals(stops) && pattern.calendarId.equals(calendarId);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.stops.hashCode() ^ this.calendarId.hashCode();
    }

    public String toString() {
        return "StopPattern(" + stops + ", " + calendarId + ")";
    }
}

public class GTFSPatternHopFactory {

    private GtfsRelationalDao _dao;

    public GTFSPatternHopFactory(GtfsContext context) throws Exception {
        _dao = context.getDao();
    }

    public static StopPattern2 stopPatternfromTrip(Trip trip, GtfsRelationalDao dao) {
        Vector<Stop> stops = new Vector<Stop>();

        for (StopTime stoptime : dao.getStopTimesForTrip(trip)) {
            stops.add(stoptime.getStop());
        }
        StopPattern2 pattern = new StopPattern2(stops, trip.getServiceId());
        return pattern;
    }

    private String id(AgencyAndId id) {
        return id.getAgencyId() + "_" + id.getId();
    }

    public ArrayList<Traversable> run(Graph graph) throws Exception {

        ArrayList<Traversable> ret = new ArrayList<Traversable>();

        // Load hops
        Collection<Trip> trips = _dao.getAllTrips();

        HashMap<StopPattern2, TripPattern> patterns = new HashMap<StopPattern2, TripPattern>();

        for (Trip trip : trips) {
            List<StopTime> stopTimes = _dao.getStopTimesForTrip(trip);
            StopPattern2 stopPattern = stopPatternfromTrip(trip, _dao);
            TripPattern tripPattern = patterns.get(stopPattern);
            int lastStop = stopTimes.size() - 1;
            if (tripPattern == null) {
                tripPattern = new TripPattern(trip, stopTimes);
                for (int i = 0; i < lastStop; i++) {
                    StopTime st0 = stopTimes.get(i);
                    Stop s0 = st0.getStop();
                    StopTime st1 = stopTimes.get(i + 1);
                    Stop s1 = st1.getStop();
                    int runningTime = st1.getArrivalTime() - st0.getDepartureTime();

                    PatternHop hop = new PatternHop(s0, s1, i, tripPattern);
                    tripPattern.addHop(i, st0.getDepartureTime(), runningTime);
                    
                    ret.add(hop);

                    Vertex startStation = graph.getVertex(id(s0.getId()));
                    Vertex endStation = graph.getVertex(id(s1.getId()));

                    // create journey vertices
                    Vertex startJourney = graph.addVertex(id(s0.getId()) + "_" + id(trip.getId()),
                            s0.getLon(), s0.getLat());
                    Vertex endJourney = graph.addVertex(id(s1.getId()) + "_" + id(trip.getId()), s1
                            .getLon(), s1.getLat());

                    PatternBoard boarding = new PatternBoard(tripPattern, i);
                    graph.addEdge(startStation, startJourney, boarding);
                    graph.addEdge(endJourney, endStation, new Alight());
                    graph.addEdge(startJourney, endJourney, hop);
                }
                patterns.put(stopPattern, tripPattern);
            } else {
                for (int i = 0; i < lastStop; i++) {
                    StopTime st0 = stopTimes.get(i);
                    StopTime st1 = stopTimes.get(i + 1);
                    int runningTime = st1.getArrivalTime() - st0.getDepartureTime();
                    tripPattern.addHop(i, st0.getDepartureTime(), runningTime);
                }
            }
        }

        return ret;
    }
}
