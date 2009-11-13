package org.opentripplanner.routing.edgetype.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Alight;
import org.opentripplanner.routing.edgetype.Board;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TripHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.gtfs.GtfsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

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

    private final Logger _log = LoggerFactory.getLogger(GTFSPatternHopFactory.class);

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

    public void run(Graph graph) throws Exception {
        /*
         * For each trip, create either pattern edges, the entries in a trip pattern's list of
         * departures, or simple hops
         */

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
                Geometry g = getTripGeometry(trip);
                LengthIndexedLine lil = null;
                LocationIndexedLine lol = null;
                if (g != null) {
                    lil = new LengthIndexedLine(g);
                    lol = new LocationIndexedLine(g);
                }
                for (int i = 0; i < lastStop; i++) {
                    StopTime st0 = stopTimes.get(i);
                    Stop s0 = st0.getStop();
                    StopTime st1 = stopTimes.get(i + 1);
                    Stop s1 = st1.getStop();
                    int runningTime = st1.getArrivalTime() - st0.getDepartureTime();

                    // create journey vertices
                    Vertex startJourney = graph.addVertex(id(s0.getId()) + "_" + id(trip.getId()),
                            s0.getLon(), s0.getLat());
                    Vertex endJourney = graph.addVertex(id(s1.getId()) + "_" + id(trip.getId()), s1
                            .getLon(), s1.getLat());

                    PatternHop hop = new PatternHop(startJourney, endJourney, s0, s1, i,
                            tripPattern);
                    if (g != null) {
                        hop.setGeometry(getHopGeometry(lil, lol, st0, st1, startJourney, endJourney));
                    }
                    tripPattern.addHop(i, 0, st0.getDepartureTime(), runningTime);
                    graph.addEdge(hop);

                    Vertex startStation = graph.getVertex(id(s0.getId()));
                    Vertex endStation = graph.getVertex(id(s1.getId()));

                    PatternBoard boarding = new PatternBoard(startStation, startJourney,
                            tripPattern, i);
                    graph.addEdge(boarding);
                    graph.addEdge(new Alight(endJourney, endStation));

                }
                patterns.put(stopPattern, tripPattern);
            } else {
                int insertionPoint = tripPattern.getInsertionPoint(stopTimes.get(0)
                        .getDepartureTime());
                if (insertionPoint < 0) {
                    // There's already a departure at this time on this trip pattern. This means
                    // that either (a) this will have all the same stop times as that one, and thus
                    // will be a duplicate of it, or (b) it will have different stops, and thus
                    // break the assumption that trips are non-overlapping.
                    _log.warn("duplicate first departure time for trip " + trip.getId()
                            + ".  This will be handled correctly but inefficiently.");

                    createSimpleHops(graph, trip, stopTimes);

                } else {

                    // try to insert this trip at this location
                    for (int i = 0; i < lastStop; i++) {
                        StopTime st0 = stopTimes.get(i);
                        StopTime st1 = stopTimes.get(i + 1);
                        int runningTime = st1.getArrivalTime() - st0.getDepartureTime();
                        try {
                            tripPattern.addHop(i, insertionPoint, st0.getDepartureTime(),
                                    runningTime);
                        } catch (TripOvertakingException e) {
                            _log
                                    .warn("trip "
                                            + trip.getId()
                                            + "overtakes another trip with the same stops.  This will be handled correctly but inefficiently.");
                            // back out trips and revert to the simple method
                            for (; i >= 0; --i) {
                                tripPattern.removeHop(i, insertionPoint);
                            }
                            createSimpleHops(graph, trip, stopTimes);
                            break;
                        }
                    }
                }
            }
        }
    }

    private LineString getTripGeometry(Trip trip) {
        AgencyAndId shapeId = trip.getShapeId();
        if (shapeId == null || shapeId.getId() == null) {
            return null;
        }
        List<ShapePoint> points = _dao.getShapePointsForShapeId(shapeId);
        if (points.size() == 0) {
            return null;
        }
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[points.size()];
        int i = 0;
        for (ShapePoint point : points) {
            coordinates[i++] = new Coordinate(point.getLon(), point.getLat());
        }
        LineString hopString = (LineString) factory.createLineString(coordinates);

        return hopString;
    }

    private void createSimpleHops(Graph graph, Trip trip, List<StopTime> stopTimes)
            throws Exception {
        String tripId = id(trip.getId());
        ArrayList<Hop> hops = new ArrayList<Hop>();

        Geometry g = getTripGeometry(trip);
        LengthIndexedLine lil = null;
        LocationIndexedLine lol = null;
        if (g != null) {
            lil = new LengthIndexedLine(g);
            lol = new LocationIndexedLine(g);
        }
        for (int i = 0; i < stopTimes.size() - 1; i++) {
            StopTime st0 = stopTimes.get(i);
            Stop s0 = st0.getStop();
            StopTime st1 = stopTimes.get(i + 1);
            Stop s1 = st1.getStop();
            Vertex startStation = graph.getVertex(id(s0.getId()));
            Vertex endStation = graph.getVertex(id(s1.getId()));

            // create journey vertices
            Vertex startJourney = graph.addVertex(id(s0.getId()) + "_" + tripId, s0.getLon(), s0
                    .getLat());
            Vertex endJourney = graph.addVertex(id(s1.getId()) + "_" + tripId, s1.getLon(), s1
                    .getLat());

            Hop hop = new Hop(startJourney, endJourney, st0, st1);
            if (g != null) {
                hop.setGeometry(getHopGeometry(lil, lol, st0, st1, startJourney, endJourney));
            }
            hops.add(hop);
            Board boarding = new Board(startStation, startJourney, hop);
            graph.addEdge(boarding);
            graph.addEdge(new Alight(endJourney, endStation));

        }
    }

    private Geometry getHopGeometry(LengthIndexedLine lil, LocationIndexedLine lol, StopTime st0,
            StopTime st1, Vertex startJourney, Vertex endJourney) {
        if (lil == null) {
            return null;
        }
        double startDt = st0.getShapeDistTraveled();
        if (startDt == -1) {
            LinearLocation startCoord = lol.indexOf(startJourney.getCoordinate());
            LinearLocation endCoord = lol.indexOf(endJourney.getCoordinate());
            return lol.extractLine(startCoord, endCoord);
        } else {
            //fixme: I don't think this actually works correctly, because getShapeDistTraveled returns a value between m and n, not 0 and 1
            double endDt = st1.getShapeDistTraveled();
            return lil.extractLine(startDt, endDt);
        }
    }
}
