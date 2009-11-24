package org.opentripplanner.routing.edgetype.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Alight;
import org.opentripplanner.routing.edgetype.Board;
import org.opentripplanner.routing.edgetype.Dwell;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
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

        int index = 0;
        
        for (Trip trip : trips) {
            
            if( index % 1000 == 0)
                _log.debug("trips=" + index + "/" + trips.size());
            index++;
            
            List<StopTime> stopTimes = _dao.getStopTimesForTrip(trip);
            
            if( stopTimes.isEmpty() )
                continue;
            
            StopPattern2 stopPattern = stopPatternfromTrip(trip, _dao);
            TripPattern tripPattern = patterns.get(stopPattern);
            int lastStop = stopTimes.size() - 1;
            if (tripPattern == null) {
                tripPattern = new TripPattern(trip, stopTimes);
                Geometry geometry = getTripGeometry(trip);
                LocationIndexedLine lol = null;
                List<ShapePoint> shapePoints = null;
                if (geometry != null) {
                    shapePoints = _dao.getShapePointsForShapeId(trip.getShapeId());
                    lol = new LocationIndexedLine(geometry);
                }
                for (int i = 0; i < lastStop; i++) {
                    StopTime st0 = stopTimes.get(i);
                    Stop s0 = st0.getStop();
                    StopTime st1 = stopTimes.get(i + 1);
                    Stop s1 = st1.getStop();
                    int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
                    int runningTime = st1.getArrivalTime() - st0.getDepartureTime();

                    // create journey vertices

                    Vertex startJourneyDepart = graph.addVertex(id(s0.getId()) + "_"
                            + id(trip.getId()) + "_D", s0.getLon(), s0.getLat());
                    Vertex endJourneyArrive = graph.addVertex(id(s1.getId()) + "_"
                            + id(trip.getId()) + "_A", s1.getLon(), s1.getLat());
                    Vertex startJourneyArrive;
                    if (i != 0) {
                        startJourneyArrive = graph.addVertex(id(s0.getId()) + "_"
                                + id(trip.getId()) + "_A", s0.getLon(), s0.getLat());

                        PatternDwell dwell = new PatternDwell(startJourneyArrive,
                                startJourneyDepart, i, tripPattern);
                        graph.addEdge(dwell);
                    }

                    PatternHop hop = new PatternHop(startJourneyDepart, endJourneyArrive, s0, s1,
                            i, tripPattern);

                    if (geometry != null) {
                        hop.setGeometry(getHopGeometry(shapePoints, lol, st0, st1,
                                startJourneyDepart, endJourneyArrive));
                    }

                    tripPattern.addHop(i, 0, st0.getDepartureTime(), runningTime, st0
                            .getArrivalTime(), dwellTime);
                    graph.addEdge(hop);

                    Vertex startStation = graph.getVertex(id(s0.getId()));
                    Vertex endStation = graph.getVertex(id(s1.getId()));

                    PatternBoard boarding = new PatternBoard(startStation, startJourneyDepart,
                            tripPattern, i);
                    graph.addEdge(boarding);
                    graph.addEdge(new PatternAlight(endJourneyArrive, endStation, tripPattern, i + 1));

                }
                patterns.put(stopPattern, tripPattern);
            } else {
                int insertionPoint = tripPattern.getDepartureTimeInsertionPoint(stopTimes.get(0)
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
                        int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
                        int runningTime = st1.getArrivalTime() - st0.getDepartureTime();
                        try {
                            tripPattern.addHop(i, insertionPoint, st0.getDepartureTime(),
                                    runningTime, st0.getArrivalTime(), dwellTime);
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
        loadTransfers(graph);
    }

    private void loadTransfers(Graph graph) {
        Collection<Transfer> transfers = _dao.getAllTransfers();
        Set<org.opentripplanner.routing.edgetype.Transfer> createdTransfers = new HashSet<org.opentripplanner.routing.edgetype.Transfer>();
        for (Transfer t : transfers) {
            Stop fromStop = t.getFromStop();
            Stop toStop = t.getToStop();
            Vertex fromStation = graph.getVertex(id(fromStop.getId()));
            Vertex toStation = graph.getVertex(id(toStop.getId()));
            int transferTime = 0;
            if (t.getTransferType() < 3) {
                if (t.getTransferType() == 2) {
                    transferTime = t.getMinTransferTime();
                }
                org.opentripplanner.routing.edgetype.Transfer edge = new org.opentripplanner.routing.edgetype.Transfer(
                        fromStation, toStation, transferTime);
                if (createdTransfers.contains(edge)) {
                    continue;
                }
                createdTransfers.add(edge);
                graph.addEdge(edge);
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

        LineString geometry = getTripGeometry(trip);
        LocationIndexedLine lol = null;
        List<ShapePoint> shapePoints = null;
        if (geometry != null) {
            shapePoints = _dao.getShapePointsForShapeId(trip.getShapeId());
            lol = new LocationIndexedLine(geometry);
        }
        for (int i = 0; i < stopTimes.size() - 1; i++) {
            StopTime st0 = stopTimes.get(i);
            Stop s0 = st0.getStop();
            StopTime st1 = stopTimes.get(i + 1);
            Stop s1 = st1.getStop();
            Vertex startStation = graph.getVertex(id(s0.getId()));
            Vertex endStation = graph.getVertex(id(s1.getId()));

            // create journey vertices
            Vertex startJourneyArrive = graph.addVertex(id(s0.getId()) + "_" + tripId, s0.getLon(),
                    s0.getLat());
            Vertex startJourneyDepart = graph.addVertex(id(s0.getId()) + "_" + tripId, s0.getLon(),
                    s0.getLat());
            Vertex endJourney = graph.addVertex(id(s1.getId()) + "_" + tripId, s1.getLon(), s1
                    .getLat());

            Dwell dwell = new Dwell(startJourneyArrive, startJourneyDepart, st0);
            graph.addEdge(dwell);
            Hop hop = new Hop(startJourneyDepart, endJourney, st0, st1);
            if (geometry != null) {
                hop.setGeometry(getHopGeometry(shapePoints, lol, st0, st1, startJourneyDepart,
                        endJourney));
            }
            hops.add(hop);
            Board boarding = new Board(startStation, startJourneyDepart, hop);
            graph.addEdge(boarding);
            graph.addEdge(new Alight(endJourney, endStation, hop));

        }
    }

    private Geometry getHopGeometry(List<ShapePoint> points, LocationIndexedLine lol, StopTime st0,
            StopTime st1, Vertex startJourney, Vertex endJourney) {
        if (lol == null) {
            return null;
        }
        double startDt = st0.getShapeDistTraveled();
        if (startDt == -1) {
            LinearLocation startCoord = lol.indexOf(startJourney.getCoordinate());
            LinearLocation endCoord = lol.indexOf(endJourney.getCoordinate());
            return lol.extractLine(startCoord, endCoord);
        } else {
            double endDt = st1.getShapeDistTraveled();

            // find the line segment that startDt is in
            ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
            ShapePoint prev = null;
            ;
            Iterator<ShapePoint> it = points.iterator();
            while (it.hasNext()) {
                ShapePoint point = it.next();
                if (point.getDistTraveled() >= startDt) {
                    Coordinate c = interpolatePoint(startDt, prev, point);
                    coords.add(c);
                    // now, find end
                    do {
                        if (point.getDistTraveled() < endDt) {
                            coords.add(new Coordinate(point.getLon(), point.getLat()));
                        } else {
                            c = interpolatePoint(endDt, prev, point);
                            coords.add(c);
                        }
                        prev = point;
                        if( it.hasNext())
                            point = it.next();
                    } while (it.hasNext());
                    break;
                }
                prev = point;
            }
            GeometryFactory factory = new GeometryFactory();
            if (coords.size() < 2) {
                Coordinate p0 = new Coordinate(st0.getStop().getLon(),st0.getStop().getLat());
                Coordinate p1 = new Coordinate(st1.getStop().getLon(),st1.getStop().getLat());
                return factory.createLineString(new Coordinate[] { p0, p1 } );
                // TODO Not all feeds are going to have Shape data... 
                // throw new RuntimeException("Not enough points when interpolating geometry by shape_dist_traveled");
            }
            return factory.createLineString(coords.toArray(new Coordinate[0]));
        }
    }

    private Coordinate interpolatePoint(double startDt, ShapePoint before, ShapePoint point) {
        if (before == null) {
            return new Coordinate(point.getLon(), point.getLat());
        }
        double segmentLength = point.getDistTraveled() - before.getDistTraveled();
        double startLocation = startDt - before.getDistTraveled();
        double interpolation = startLocation / segmentLength;
        double x = before.getLon() * interpolation + point.getLon() * (1 - interpolation);
        double y = before.getLat() * interpolation + point.getLat() * (1 - interpolation);
        Coordinate c = new Coordinate(x, y);
        return c;
    }
}
