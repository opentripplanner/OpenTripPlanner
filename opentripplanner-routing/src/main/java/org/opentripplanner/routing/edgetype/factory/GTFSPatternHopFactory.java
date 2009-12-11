/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.routing.edgetype.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.TraverseMode;
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
import com.vividsolutions.jts.geom.CoordinateSequence;
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

class EncodedTrip {
    Trip trip;

    int patternIndex;

    TripPattern pattern;

    public EncodedTrip(Trip trip, int i, TripPattern pattern) {
        this.trip = trip;
        this.patternIndex = i;
        this.pattern = pattern;
    }

    public boolean equals(Object o) {
        if (!(o instanceof EncodedTrip))
            return false;
        EncodedTrip eto = (EncodedTrip) o;
        return trip.equals(eto.trip) && patternIndex == eto.patternIndex
                && pattern.equals(eto.pattern);
    }

    public String toString() {
        return "EncodedTrip(" + this.trip + ", " + this.patternIndex + ", " + this.pattern + ")";
    }
}

public class GTFSPatternHopFactory {

    private final Logger _log = LoggerFactory.getLogger(GTFSPatternHopFactory.class);

    private static GeometryFactory _factory = new GeometryFactory();

    private GtfsRelationalDao _dao;

    private Map<ShapeSegmentKey, LineString> _geometriesByShapeSegmentKey = new HashMap<ShapeSegmentKey, LineString>();

    private Map<AgencyAndId, LineString> _geometriesByShapeId = new HashMap<AgencyAndId, LineString>();

    private Map<AgencyAndId, double[]> _distancesByShapeId = new HashMap<AgencyAndId, double[]>();

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
        return GtfsLibrary.convertIdToString(id);
    }

    public void run(Graph graph) throws Exception {

        clearCachedData();

        /*
         * For each trip, create either pattern edges, the entries in a trip pattern's list of
         * departures, or simple hops
         */

        // Load hops
        Collection<Trip> trips = _dao.getAllTrips();

        HashMap<StopPattern2, TripPattern> patterns = new HashMap<StopPattern2, TripPattern>();

        int index = 0;

        HashMap<String, ArrayList<EncodedTrip>> tripsByBlock = new HashMap<String, ArrayList<EncodedTrip>>();

        for (Trip trip : trips) {

            if (index % 100 == 0)
                _log.debug("trips=" + index + "/" + trips.size());
            index++;

            List<StopTime> stopTimes = _dao.getStopTimesForTrip(trip);

            if (stopTimes.isEmpty())
                continue;

            StopPattern2 stopPattern = stopPatternfromTrip(trip, _dao);
            TripPattern tripPattern = patterns.get(stopPattern);
            int lastStop = stopTimes.size() - 1;
            TraverseMode mode = GtfsLibrary.getTraverseMode(trip.getRoute());
            if (tripPattern == null) {

                tripPattern = new TripPattern(trip, stopTimes);
                int patternIndex = -1;

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

                    hop.setGeometry(getHopGeometry(trip.getShapeId(), st0, st1, startJourneyDepart,
                            endJourneyArrive));

                    patternIndex = tripPattern.addHop(i, 0, st0.getDepartureTime(), runningTime,
                            st1.getArrivalTime(), dwellTime);
                    graph.addEdge(hop);

                    Vertex startStation = graph.getVertex(id(s0.getId()));
                    Vertex endStation = graph.getVertex(id(s1.getId()));

                    PatternBoard boarding = new PatternBoard(startStation, startJourneyDepart,
                            tripPattern, i, mode);
                    graph.addEdge(boarding);
                    graph.addEdge(new PatternAlight(endJourneyArrive, endStation, tripPattern, i, mode));
                }
                patterns.put(stopPattern, tripPattern);

                String blockId = trip.getBlockId();
                if (blockId != null && !blockId.equals("")) {
                    ArrayList<EncodedTrip> blockTrips = tripsByBlock.get(blockId);
                    if (blockTrips == null) {
                        blockTrips = new ArrayList<EncodedTrip>();
                        tripsByBlock.put(blockId, blockTrips);
                    }
                    blockTrips.add(new EncodedTrip(trip, patternIndex, tripPattern));
                }
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
                    boolean simple = false;
                    for (int i = 0; i < lastStop; i++) {
                        StopTime st0 = stopTimes.get(i);
                        StopTime st1 = stopTimes.get(i + 1);
                        int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
                        int runningTime = st1.getArrivalTime() - st0.getDepartureTime();
                        try {
                            tripPattern.addHop(i, insertionPoint, st0.getDepartureTime(),
                                    runningTime, st1.getArrivalTime(), dwellTime);
                        } catch (TripOvertakingException e) {
                            _log
                                    .warn("trip "
                                            + trip.getId()
                                            + " overtakes another trip with the same stops.  This will be handled correctly but inefficiently.");
                            // back out trips and revert to the simple method
                            for (i=i-1; i >= 0; --i) {
                                tripPattern.removeHop(i, insertionPoint);
                            }
                            createSimpleHops(graph, trip, stopTimes);
                            simple = true;
                            break;
                        }
                    }
                    if (!simple) {
                        String blockId = trip.getBlockId();
                        if (blockId != null && !blockId.equals("")) {
                            ArrayList<EncodedTrip> blockTrips = tripsByBlock.get(blockId);
                            if (blockTrips == null) {
                                blockTrips = new ArrayList<EncodedTrip>();
                                tripsByBlock.put(blockId, blockTrips);
                            }
                            blockTrips.add(new EncodedTrip(trip, 0, tripPattern));
                        }
                    }
                }
            }
        }

        /* for interlined trips, add final dwell edge */
        for (ArrayList<EncodedTrip> blockTrips : tripsByBlock.values()) {
            HashMap<Stop, EncodedTrip> starts = new HashMap<Stop, EncodedTrip>();
            for (EncodedTrip encoded : blockTrips) {
                Trip trip = encoded.trip;
                List<StopTime> stopTimes = _dao.getStopTimesForTrip(trip);
                Stop start = stopTimes.get(0).getStop();
                starts.put(start, encoded);
            }
            for (EncodedTrip encoded : blockTrips) {
                Trip trip = encoded.trip;
                List<StopTime> stopTimes = _dao.getStopTimesForTrip(trip);
                StopTime endTime = stopTimes.get(stopTimes.size() - 1);
                Stop end = endTime.getStop();

                if (starts.containsKey(end)) {
                    EncodedTrip nextTrip = starts.get(end);

                    Vertex arrive = graph.addVertex(
                            id(end.getId()) + "_" + id(trip.getId()) + "_A", end.getLon(), end
                                    .getLat());

                    Vertex depart = graph.addVertex(id(end.getId()) + "_"
                            + id(nextTrip.trip.getId()) + "_D", end.getLon(), end.getLat());
                    PatternDwell dwell = new PatternDwell(arrive, depart, nextTrip.patternIndex,
                            encoded.pattern);

                    graph.addEdge(dwell);

                    List<StopTime> nextStopTimes = _dao.getStopTimesForTrip(nextTrip.trip);
                    StopTime startTime = nextStopTimes.get(0);
                    int dwellTime = startTime.getDepartureTime() - startTime.getArrivalTime();
                    encoded.pattern.setDwellTime(stopTimes.size() - 2, encoded.patternIndex,
                            dwellTime);

                }
            }
        }

        loadTransfers(graph);

        clearCachedData();
    }

    private void clearCachedData() {
        _log.debug("shapes=" + _geometriesByShapeId.size());
        _log.debug("segments=" + _geometriesByShapeSegmentKey.size());
        _geometriesByShapeId.clear();
        _distancesByShapeId.clear();
        _geometriesByShapeSegmentKey.clear();
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
                GeometryFactory factory = new GeometryFactory();
                LineString geometry = factory.createLineString(new Coordinate[] {
                        new Coordinate(fromStop.getLon(), fromStop.getLat()),
                        new Coordinate(toStop.getLon(), toStop.getLat()) });
                edge.setGeometry(geometry);
                createdTransfers.add(edge);
                graph.addEdge(edge);
            }
        }
    }

    private void createSimpleHops(Graph graph, Trip trip, List<StopTime> stopTimes)
            throws Exception {

        String tripId = id(trip.getId());
        ArrayList<Hop> hops = new ArrayList<Hop>();

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
            hop.setGeometry(getHopGeometry(trip.getShapeId(), st0, st1, startJourneyDepart,
                    endJourney));
            hops.add(hop);
            Board boarding = new Board(startStation, startJourneyDepart, hop);
            graph.addEdge(boarding);
            graph.addEdge(new Alight(endJourney, endStation, hop));

        }
    }

    private Geometry getHopGeometry(AgencyAndId shapeId, StopTime st0, StopTime st1,
            Vertex startJourney, Vertex endJourney) {

        if (shapeId == null || shapeId.getId() == null || shapeId.getId().equals(""))
            return null;

        double startDistance = st0.getShapeDistTraveled();
        double endDistance = st1.getShapeDistTraveled();

        boolean hasShapeDist = startDistance != -1 && endDistance != -1;

        if (hasShapeDist) {

            ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);
            LineString geometry = _geometriesByShapeSegmentKey.get(key);
            if (geometry != null)
                return geometry;

            double[] distances = getDistanceForShapeId(shapeId);

            if (distances != null) {

                LinearLocation startIndex = getSegmentFraction(distances, startDistance);
                LinearLocation endIndex = getSegmentFraction(distances, endDistance);

                LineString line = getLineStringForShapeId(shapeId);
                LocationIndexedLine lol = new LocationIndexedLine(line);

                return getSegmentGeometry(shapeId, lol, startIndex, endIndex, startDistance,
                        endDistance);
            }
        }

        LineString line = getLineStringForShapeId(shapeId);
        LocationIndexedLine lol = new LocationIndexedLine(line);

        LinearLocation startCoord = lol.indexOf(startJourney.getCoordinate());
        LinearLocation endCoord = lol.indexOf(endJourney.getCoordinate());

        double distanceFrom = startCoord.getSegmentLength(line);
        double distanceTo = endCoord.getSegmentLength(line);

        return getSegmentGeometry(shapeId, lol, startCoord, endCoord, distanceFrom, distanceTo);
    }

    private Geometry getSegmentGeometry(AgencyAndId shapeId,
            LocationIndexedLine locationIndexedLine, LinearLocation startIndex,
            LinearLocation endIndex, double startDistance, double endDistance) {

        ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);

        LineString geometry = _geometriesByShapeSegmentKey.get(key);
        if (geometry == null) {

            geometry = (LineString) locationIndexedLine.extractLine(startIndex, endIndex);

            // Pack the resulting line string
            CoordinateSequence sequence = new PackedCoordinateSequence.Float(geometry
                    .getCoordinates(), 2);
            geometry = _factory.createLineString(sequence);

            _geometriesByShapeSegmentKey.put(key, geometry);
        }

        return geometry;
    }

    private LineString getLineStringForShapeId(AgencyAndId shapeId) {

        LineString geometry = _geometriesByShapeId.get(shapeId);

        if (geometry != null)
            return geometry;

        List<ShapePoint> points = _dao.getShapePointsForShapeId(shapeId);
        Coordinate[] coordinates = new Coordinate[points.size()];
        double[] distances = new double[points.size()];

        boolean hasAllDistances = true;

        int i = 0;
        for (ShapePoint point : points) {
            coordinates[i] = new Coordinate(point.getLon(), point.getLat());
            distances[i] = point.getDistTraveled();
            if (point.getDistTraveled() == -1)
                hasAllDistances = false;
            i++;
        }

        /**
         * If we don't have distances here, we can't calculate them ourselves because we can't
         * assume the units will match
         */

        if (!hasAllDistances) {
            distances = null;
        }

        CoordinateSequence sequence = new PackedCoordinateSequence.Float(coordinates, 2);
        geometry = _factory.createLineString(sequence);
        _geometriesByShapeId.put(shapeId, geometry);
        _distancesByShapeId.put(shapeId, distances);

        return geometry;
    }

    private double[] getDistanceForShapeId(AgencyAndId shapeId) {
        getLineStringForShapeId(shapeId);
        return _distancesByShapeId.get(shapeId);
    }

    private LinearLocation getSegmentFraction(double[] distances, double distance) {
        int index = Arrays.binarySearch(distances, distance);
        if (index < 0)
            index = -(index + 1);
        if (index == 0)
            return new LinearLocation(0, 0.0);
        if (index == distances.length)
            return new LinearLocation(distances.length, 0.0);

        double indexPart = (distance - distances[index - 1])
                / (distances[index] - distances[index - 1]);
        return new LinearLocation(index - 1, indexPart);
    }
}
