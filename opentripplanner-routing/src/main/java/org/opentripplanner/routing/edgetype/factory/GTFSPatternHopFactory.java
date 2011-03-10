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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.Pathway;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.FareContext;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Alight;
import org.opentripplanner.routing.edgetype.BasicTripPattern;
import org.opentripplanner.routing.edgetype.Board;
import org.opentripplanner.routing.edgetype.Dwell;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
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

/**
 * 
 * A StopPattern is an intermediate object used when processing GTFS files. It represents an ordered
 * list of stops and a service ID. Any two trips with the same stops in the same order, and that
 * operates on the same days, can be combined using a TripPattern to save memory.
 */

class StopPattern {
    Vector<Stop> stops;
    Vector<Boolean> pickups;
    Vector<Boolean> dropoffs;

    AgencyAndId calendarId;

    public StopPattern(Vector<Stop> stops, Vector<Boolean> pickups, Vector<Boolean> dropoffs, AgencyAndId calendarId) {
        this.stops = stops;
        this.pickups = pickups;
        this.dropoffs = dropoffs;
        this.calendarId = calendarId;
    }

    public boolean equals(Object other) {
        if (other instanceof StopPattern) {
            StopPattern pattern = (StopPattern) other;
            return pattern.stops.equals(stops) && pattern.calendarId.equals(calendarId) && pattern.pickups.equals(pickups) && pattern.dropoffs.equals(dropoffs);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.stops.hashCode() ^ this.calendarId.hashCode() + this.pickups.hashCode() + this.dropoffs.hashCode();
    }

    public String toString() {
        return "StopPattern(" + stops + ", " + calendarId + ")";
    }
}

class InterliningTrip  implements Comparable<InterliningTrip> {
    public Trip trip;
    public StopTime firstStopTime;
    public StopTime lastStopTime;
    BasicTripPattern tripPattern;

    InterliningTrip(Trip trip, List<StopTime> stopTimes, BasicTripPattern tripPattern) {
        this.trip = trip;
        this.firstStopTime = stopTimes.get(0);
        this.lastStopTime = stopTimes.get(stopTimes.size() - 1);
        this.tripPattern = tripPattern;
    }

    public int getPatternIndex() {
        return tripPattern.getPatternIndex(trip);
    }
    
    @Override
    public int compareTo(InterliningTrip o) {
        return firstStopTime.getArrivalTime() - o.firstStopTime.getArrivalTime(); 
    }
    
}

class BlockIdAndServiceId {
    public String blockId;
    public AgencyAndId serviceId;

    BlockIdAndServiceId(String blockId, AgencyAndId serviceId) {
        this.blockId = blockId;
        this.serviceId = serviceId;
    }
    
    public boolean equals(Object o) {
        if (o instanceof BlockIdAndServiceId) {
            BlockIdAndServiceId other = ((BlockIdAndServiceId) o);
            return other.blockId.equals(blockId) && other.serviceId.equals(serviceId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return blockId.hashCode() * 31 + serviceId.hashCode();
    }
}

class InterlineSwitchoverKey {

    public Stop s0, s1;
    public BasicTripPattern pattern1, pattern2;

    public InterlineSwitchoverKey(Stop s0, Stop s1, BasicTripPattern pattern1,
            BasicTripPattern pattern2) {
        this.s0 = s0;
        this.s1 = s1;
        this.pattern1 = pattern1;
        this.pattern2 = pattern2;
    }
    
    public boolean equals(Object o) {
        if (o instanceof InterlineSwitchoverKey) {
            InterlineSwitchoverKey other = (InterlineSwitchoverKey) o;
            return other.s0.equals(s0) && 
                other.s1.equals(s1) &&
                other.pattern1 == pattern1 &&
                other.pattern2 == pattern2;
        }
        return false;
    }
    
    public int hashCode() {
        return (((s0.hashCode() * 31) + s1.hashCode()) * 31 + pattern1.hashCode()) * 31 + pattern2.hashCode(); 
    }
}

/**
 * Generates a set of edges from GTFS.
 */
public class GTFSPatternHopFactory {

    private static final Logger _log = LoggerFactory.getLogger(GTFSPatternHopFactory.class);

    private static GeometryFactory _factory = new GeometryFactory();

    private GtfsRelationalDao _dao;

    private Map<ShapeSegmentKey, LineString> _geometriesByShapeSegmentKey = new HashMap<ShapeSegmentKey, LineString>();

    private Map<AgencyAndId, LineString> _geometriesByShapeId = new HashMap<AgencyAndId, LineString>();

    private Map<AgencyAndId, double[]> _distancesByShapeId = new HashMap<AgencyAndId, double[]>();

    private ArrayList<PatternDwell> potentiallyUselessDwells = new ArrayList<PatternDwell> ();

    private FareContext fareContext = null;

    private HashMap<BlockIdAndServiceId, List<InterliningTrip>> tripsForBlock = new HashMap<BlockIdAndServiceId, List<InterliningTrip>>();

    private Map<InterlineSwitchoverKey, PatternInterlineDwell> interlineDwells = new HashMap<InterlineSwitchoverKey, PatternInterlineDwell>();

    public GTFSPatternHopFactory(GtfsContext context) {
        _dao = context.getDao();
       
        HashMap<AgencyAndId, FareRuleSet> fareRules = context.getFareRules();
        HashMap<AgencyAndId, FareAttribute> fareAttributes = new HashMap<AgencyAndId, FareAttribute>(); 
        for (AgencyAndId fareId: fareRules.keySet()) {
            FareAttribute attribute = context.getFareAttribute(fareId);
            fareAttributes.put(fareId, attribute);
        }
        if (fareRules.size () > 0 && fareAttributes.size() > 0) { 
            fareContext = new FareContext(fareRules, fareAttributes);
        }
    }

    public static StopPattern stopPatternfromTrip(Trip trip, GtfsRelationalDao dao) {
        Vector<Stop> stops = new Vector<Stop>();
        Vector<Boolean> pickups = new Vector<Boolean>();
        Vector<Boolean> dropoffs = new Vector<Boolean>();
        for (StopTime stoptime : dao.getStopTimesForTrip(trip)) {
            stops.add(stoptime.getStop());
            pickups.add(stoptime.getPickupType() != 1);
            dropoffs.add(stoptime.getDropOffType() != 1);
        }
        StopPattern pattern = new StopPattern(stops, pickups, dropoffs, trip.getServiceId());
        return pattern;
    }

    private String id(AgencyAndId id) {
        return GtfsLibrary.convertIdToString(id);
    }

    /**
     * Generate the edges. Assumes that there are already vertices in the graph for the stops.
     */
    public void run(Graph graph) {

        // Load stops
        loadStops(graph);
        loadPathways(graph);

        // Load hops
        _log.debug("Loading hops");

        clearCachedData();

        /*
         * For each trip, create either pattern edges, the entries in a trip pattern's list of
         * departures, or simple hops
         */

        // Load hops
        Collection<Trip> trips = _dao.getAllTrips();

        HashMap<StopPattern, BasicTripPattern> patterns = new HashMap<StopPattern, BasicTripPattern>();

        int index = 0;

        HashMap<Trip, List<Frequency>> tripFrequencies = new HashMap<Trip, List<Frequency>>();
        for(Frequency freq : _dao.getAllFrequencies()) {
            List<Frequency> freqs= tripFrequencies.get(freq.getTrip());
            if(freqs == null) {
                freqs = new ArrayList<Frequency>();
                tripFrequencies.put(freq.getTrip(), freqs);
            }
            freqs.add(freq);
        }

        for (Trip trip : trips) {

            if (index % 1000 == 0)
                _log.debug("trips=" + index + "/" + trips.size());
            index++;

            List<StopTime> originalStopTimes = _dao.getStopTimesForTrip(trip);
            interpolateStopTimes(originalStopTimes);
            if (originalStopTimes.size() < 2) {
                _log
                        .warn("Trip "
                                + trip
                                + " has fewer than two stops.  We will not use it for routing.  This is probably an error in your data");
                continue;
            }

            List<List<StopTime>> allStopTimes = new ArrayList<List<StopTime>>();
            List<Frequency>      frequencies  = tripFrequencies.get(trip);

            StopPattern stopPattern = stopPatternfromTrip(trip, _dao);
            BasicTripPattern tripPattern = patterns.get(stopPattern);
            String blockId = trip.getBlockId();

            /*
             * Trip frequencies are handled by creating new StopTimes for each departure in
             * a frequency. Since only the departure/arrival times change, the tripPattern
             * may be reused.
             *
             * FIXME: Instead of duplicating StopTimes, create a new set of edgetypes
             * FIXME: to represent frequency-based trips.
             */
            if(frequencies == null) {
                allStopTimes.add(originalStopTimes);
            } else {
                for(Frequency freq : frequencies) {
                    for(int i = freq.getStartTime(); i < freq.getEndTime(); i += freq.getHeadwaySecs()) {
                        int diff = i - originalStopTimes.get(0).getArrivalTime();
                        List<StopTime> newStopTimes = new ArrayList<StopTime>();

                        for(StopTime st : originalStopTimes) {
                            StopTime modified = cloneStopTime(st);
                            if(st.isArrivalTimeSet())
                                modified.setArrivalTime(st.getArrivalTime() + diff);
                            if(st.isDepartureTimeSet())
                                modified.setDepartureTime(st.getDepartureTime() + diff);
                            newStopTimes.add(modified);
                        }
                        allStopTimes.add(newStopTimes);
                    }
                }
            }

            for(List<StopTime> stopTimes : allStopTimes) {
                boolean simple = false;

                if (tripPattern == null) {
                    tripPattern = makeTripPattern(graph, trip, stopTimes, fareContext);

                    patterns.put(stopPattern, tripPattern);
                    if (blockId != null && !blockId.equals("")) {
                        addTripToInterliningMap(trip, stopTimes, tripPattern);
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

                        simple = true;
                        createSimpleHops(graph, trip, stopTimes);

                    } else {

                        // try to insert this trip at this location

                        StopTime st1 = null;
                        int i;
                        for (i = 0; i < stopTimes.size() - 1; i++) {
                            StopTime st0 = stopTimes.get(i);
                            st1 = stopTimes.get(i + 1);
                            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
                            int runningTime = st1.getArrivalTime() - st0.getDepartureTime();
                            try {
                                tripPattern.addHop(i, insertionPoint, st0.getDepartureTime(),
                                        runningTime, st1.getArrivalTime(), dwellTime,
                                        trip);
                            } catch (TripOvertakingException e) {
                                _log
                                        .warn("trip "
                                                + trip.getId()
                                                + " overtakes another trip with the same stops.  This will be handled correctly but inefficiently.");
                                // back out trips and revert to the simple method
                                for (i = i - 1; i >= 0; --i) {
                                    tripPattern.removeHop(i, insertionPoint);
                                }
                                createSimpleHops(graph, trip, stopTimes);
                                simple = true;
                                break;
                            }
                        }
                    }
                    if (!simple) {
                        if (blockId != null && !blockId.equals("")) {
                            addTripToInterliningMap(trip, stopTimes, tripPattern);
                        }
                        tripPattern.setTripFlags(insertionPoint, (trip.getWheelchairAccessible() != 0) ? TripPattern.FLAG_WHEELCHAIR_ACCESSIBLE : 0);
                    }
                }
            }
        }

        for (List<InterliningTrip> blockTrips : tripsForBlock.values()) {
            if (blockTrips.size() == 1) {
                //blocks of only a single trip do not need processing
                continue;
            }            
            Collections.sort(blockTrips); 
            
            /* this is all the trips for this block/schedule */
            
            for (int i = 0; i < blockTrips.size() - 1; ++i) {
                InterliningTrip fromInterlineTrip = blockTrips.get(i);
                InterliningTrip toInterlineTrip = blockTrips.get(i + 1);
                
                Trip fromTrip = fromInterlineTrip.trip;                
                Trip toTrip = toInterlineTrip.trip;
                
                StopTime st0 = fromInterlineTrip.lastStopTime;
                StopTime st1 = toInterlineTrip.firstStopTime;
                
                Stop s0 = st0.getStop();
                Stop s1 = st1.getStop();
                
                Trip fromExemplar = fromInterlineTrip.tripPattern.exemplar;
                Trip toExemplar = toInterlineTrip.tripPattern.exemplar;
                
                List<StopTime> fromStopTimes = _dao.getStopTimesForTrip(fromExemplar);
                int exemplarStopSequence0 = fromStopTimes.get(fromStopTimes.size() - 1).getStopSequence();
                
                List<StopTime> toStopTimes = _dao.getStopTimesForTrip(toExemplar);
                int exemplarStopSequence1 = toStopTimes.get(0).getStopSequence();
                
                PatternInterlineDwell dwell;
                // do we already have a PID for this dwell?
                InterlineSwitchoverKey dwellKey = new InterlineSwitchoverKey(s0, s1, fromInterlineTrip.tripPattern, toInterlineTrip.tripPattern);
                dwell = getInterlineDwell(dwellKey);
                if (dwell == null) {
                    
                    // create the dwell
                    String startId = id(s0.getId()) + "_" + id(fromExemplar.getId()) + "_"  + exemplarStopSequence0 + "_A";
                    Vertex startJourney = graph.getVertex(startId);
                    String endId = id(s1.getId()) + "_" + id(toExemplar.getId()) + "_"  + exemplarStopSequence1 + "_D";
                    Vertex endJourney = graph.getVertex(endId);
                    dwell = new PatternInterlineDwell(startJourney, endJourney, toTrip);
                    putInterlineDwell(dwellKey, dwell);
                    graph.addEdge(dwell);
                }
                int dwellTime = st1.getDepartureTime() - st0.getArrivalTime();
                dwell.addTrip(fromTrip.getId(), toTrip.getId(), dwellTime, 
                        fromInterlineTrip.getPatternIndex(), toInterlineTrip.getPatternIndex());

            }
        }

        loadTransfers(graph);
        deleteUselessDwells(graph);
        clearCachedData();
      }

    private void putInterlineDwell(InterlineSwitchoverKey key, PatternInterlineDwell dwell) {
        interlineDwells.put(key, dwell);
    }

    private PatternInterlineDwell getInterlineDwell(InterlineSwitchoverKey key) {
        return interlineDwells.get(key);
    }

    private void loadPathways(Graph graph) {
        for (Pathway pathway : _dao.getAllPathways()) {
            Vertex fromVertex = graph.getVertex(id(pathway.getFromStop().getId()));
            Vertex toVertex = graph.getVertex(id(pathway.getToStop().getId()));
            PathwayEdge path;
            if (pathway.isWheelchairTraversalTimeSet()) {
                path = new PathwayEdge(fromVertex, toVertex, pathway.getTraversalTime());
            } else {
                path = new PathwayEdge(fromVertex, toVertex, pathway.getTraversalTime(), pathway.getWheelchairTraversalTime());
            }
            graph.addEdge(path); 
        }
    }

    private void loadStops(Graph graph) {
        for (Stop stop : _dao.getAllStops()) {
            //add a vertex representing the stop
            Vertex stopVertex = graph.addVertex(new TransitStop(id(stop.getId()), stop.getLon(),
                    stop.getLat(), stop.getName(), stop.getId().getId(), stop));
            
            if (stop.getLocationType() != 2) {
                //add a vertex representing arriving at the stop
                Vertex arrive = graph.addVertex(new GenericVertex(arrivalVertexId(id(stop.getId())), stop.getLon(),
                        stop.getLat(), stop.getName(), stop.getId().getId()));

                //add a vertex representing departing from at the stop
                Vertex depart = graph.addVertex(new GenericVertex(departureVertexId(id(stop.getId())), stop.getLon(),
                        stop.getLat(), stop.getName(), stop.getId().getId()));

                //add edges from arrive to stop and stop to depart

                graph.addEdge(new PreAlightEdge(arrive, stopVertex));
                graph.addEdge(new PreBoardEdge(stopVertex, depart));
            }
        }
    }
    
    /**
     * Delete dwell edges that take no time, and merge their start and end vertices.
     * For trip patterns that have no trips with dwells, remove the dwell data, and merge the arrival
     * and departure times.
     */
    private void deleteUselessDwells(Graph graph) {
        HashSet<BasicTripPattern> possiblySimplePatterns = new HashSet<BasicTripPattern>();
        HashSet<BasicTripPattern> notSimplePatterns = new HashSet<BasicTripPattern>();
        for (PatternDwell dwell : potentiallyUselessDwells) {
            BasicTripPattern pattern = (BasicTripPattern) dwell.getPattern();
            boolean useless = true;
            for (int i = 0; i < pattern.getNumDwells(); ++i) {
                if (pattern.getDwellTime(dwell.getStopIndex(), i) != 0) {
                    useless = false;
                    break;
                }
            }

            if (!useless) {
                possiblySimplePatterns.remove(pattern);
                notSimplePatterns.add(pattern);
                continue;
            }

            GenericVertex v = (GenericVertex) dwell.getFromVertex();
            v.mergeFrom(graph, (GenericVertex) dwell.getToVertex());
            if (!notSimplePatterns.contains(pattern)) {
                possiblySimplePatterns.add(pattern);
            }
        }
        for (BasicTripPattern pattern: possiblySimplePatterns) {
            pattern.simplify();
        }
    }

    private void addTripToInterliningMap(Trip trip, List<StopTime> stopTimes, BasicTripPattern tripPattern) {
        String blockId = trip.getBlockId();
        BlockIdAndServiceId key = new BlockIdAndServiceId(blockId, trip.getServiceId()); 
        List<InterliningTrip> trips = tripsForBlock.get(key);
        if (trips == null) {
            trips = new ArrayList<InterliningTrip>();
            tripsForBlock.put(key, trips);
        }
        trips.add(new InterliningTrip(trip, stopTimes, tripPattern));
    }

    private void interpolateStopTimes(List<StopTime> stopTimes) {
        int lastStop = stopTimes.size() - 1;
        int numInterpStops = -1;
        int departureTime = -1, prevDepartureTime = -1;
        int interpStep = 0;

        int i;
        StopTime st1 = null;
        for (i = 0; i < lastStop; i++) {
            StopTime st0 = stopTimes.get(i);
            st1 = stopTimes.get(i + 1);

            prevDepartureTime = departureTime;
            departureTime = st0.getDepartureTime();

            /* Interpolate, if necessary, the times of non-timepoint stops */

            /* trivial cases */
            if (!st0.isDepartureTimeSet() && st0.isArrivalTimeSet()) {
                st0.setDepartureTime(st0.getArrivalTime());
            }
            if (!st1.isDepartureTimeSet() && st1.isArrivalTimeSet()) {
                st1.setDepartureTime(st1.getArrivalTime());
            }
            /* genuine interpolation needed */
            if (!(st0.isDepartureTimeSet() && st0.isArrivalTimeSet())) {
                // figure out how many such stops there are in a row.
                int j;
                StopTime st = null;
                for (j = i + 1; j < lastStop + 1; ++j) {
                    st = stopTimes.get(j);
                    if (st.isDepartureTimeSet() || st.isArrivalTimeSet()) {
                        break;
                    }
                }
                if (j == lastStop + 1) {
                    throw new RuntimeException(
                            "Could not interpolate arrival/departure time on stop " + i
                            + " (missing final stop time) on trip " + st0.getTrip());
                }
                numInterpStops = j - i;
                int arrivalTime;
                if (st.isArrivalTimeSet()) {
                    arrivalTime = st.getArrivalTime();
                } else {
                    arrivalTime = st.getDepartureTime();
                }
                interpStep = (arrivalTime - prevDepartureTime) / (numInterpStops + 1);
                if (interpStep < 0) {
                    throw new RuntimeException(
                            "trip goes backwards for some reason");
                }
                for (j = i; j < i + numInterpStops; ++j) {
                    //System.out.println("interpolating " + j + " between " + prevDepartureTime + " and " + arrivalTime);
                    departureTime = prevDepartureTime + interpStep * (j - i + 1);
                    st = stopTimes.get(j);
                    if (st.isArrivalTimeSet()) {
                        departureTime = st.getArrivalTime();
                    } else {
                        st.setArrivalTime(departureTime);
                    }
                    if (!st.isDepartureTimeSet()) {
                        st.setDepartureTime(departureTime);
                    }
                }
                i = j - 1;
            }
        }
    }

    private BasicTripPattern makeTripPattern(Graph graph, Trip trip, List<StopTime> stopTimes, FareContext fareContext) {
        BasicTripPattern tripPattern = new BasicTripPattern(trip, stopTimes, fareContext);

        TraverseMode mode = GtfsLibrary.getTraverseMode(trip.getRoute());
        int lastStop = stopTimes.size() - 1;

        int i;
        StopTime st1 = null;
        for (i = 0; i < lastStop; i++) {
            StopTime st0 = stopTimes.get(i);
            Stop s0 = st0.getStop();
            st1 = stopTimes.get(i + 1);
            Stop s1 = st1.getStop();
            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();

            String departId = id(s0.getId()) + "_" + id(trip.getId()) + "_"  + st0.getStopSequence() + "_D";

            String arriveId = id(s1.getId()) + "_" + id(trip.getId()) + "_" + st1.getStopSequence() + "_A";

            // create journey vertices

            Vertex startJourneyDepart = graph.addVertex(departId, s0.getName(), s0.getId().getId(), s0.getLon(), s0.getLat());

            Vertex endJourneyArrive = graph.addVertex(arriveId, s1.getName(), s1.getId().getId(), s1.getLon(), s1.getLat());
            Vertex startJourneyArrive;
            if (i != 0) {
                startJourneyArrive = graph.addVertex(
                        id(s0.getId()) + "_" + id(trip.getId())  + "_" + st0.getStopSequence() + "_A", s0.getName(), s0.getId().getId(), s0.getLon(), s0.getLat());

                PatternDwell dwell = new PatternDwell(startJourneyArrive, startJourneyDepart, i,
                        tripPattern);
                if (dwellTime == 0) {
                    potentiallyUselessDwells.add(dwell);
                }
                graph.addEdge(dwell);
            }

            PatternHop hop = new PatternHop(startJourneyDepart, endJourneyArrive, s0, s1, i,
                    tripPattern);
            hop.setFareContext(fareContext);
            hop.setGeometry(getHopGeometry(trip.getShapeId(), st0, st1, startJourneyDepart,
                    endJourneyArrive));

            int arrivalTime = st1.getArrivalTime();

            int departureTime = st0.getDepartureTime();

            int runningTime = arrivalTime - departureTime ;

            tripPattern.addHop(i, 0, departureTime, runningTime, arrivalTime, dwellTime,
                    trip);
            graph.addEdge(hop);

            Vertex startStation = graph.getVertex(departureVertexId(id(s0.getId())));
            Vertex endStation = graph.getVertex(arrivalVertexId(id(s1.getId())));

            PatternBoard boarding = new PatternBoard(startStation, startJourneyDepart, tripPattern,
                    i, mode);
            graph.addEdge(boarding);
            graph.addEdge(new PatternAlight(endJourneyArrive, endStation, tripPattern, i, mode));
        }

        tripPattern.setTripFlags(0, (trip.getWheelchairAccessible() != 0) ? TripPattern.FLAG_WHEELCHAIR_ACCESSIBLE : 0);

        return tripPattern;
    }

    private String arrivalVertexId(String id) {
        return id + "_arrive";
    }

    private String departureVertexId(String id) {
        return id + "_depart";
    }

    private void clearCachedData() {
        _log.debug("shapes=" + _geometriesByShapeId.size());
        _log.debug("segments=" + _geometriesByShapeSegmentKey.size());
        _geometriesByShapeId.clear();
        _distancesByShapeId.clear();
        _geometriesByShapeSegmentKey.clear();
        potentiallyUselessDwells.clear();
    }

    private void loadTransfers(Graph graph) {
        Collection<Transfer> transfers = _dao.getAllTransfers();
        TransferTable transferTable = graph.getTransferTable();
        for (Transfer t : transfers) {
            Stop fromStop = t.getFromStop();
            Stop toStop = t.getToStop();
            Vertex fromVertex = graph.getVertex(arrivalVertexId(id(fromStop.getId())));
            Vertex toVertex = graph.getVertex(departureVertexId(id(toStop.getId())));
            switch (t.getTransferType()) {
            case 1:
                // timed transfer 
                transferTable.setTransferTime(fromVertex, toVertex, TransferTable.TIMED_TRANSFER);
                break;
            case 2:
                // min transfer time
                transferTable.setTransferTime(fromVertex, toVertex, t.getMinTransferTime());
                break;
            case 3:
                // forbidden transfer
                transferTable.setTransferTime(fromVertex, toVertex, TransferTable.FORBIDDEN_TRANSFER);
                break;
            case 0:
            default: 
                // preferred transfer
                transferTable.setTransferTime(fromVertex, toVertex, TransferTable.PREFERRED_TRANSFER);
                break;
            }
        }
    }

    private void createSimpleHops(Graph graph, Trip trip, List<StopTime> stopTimes) {

        String tripId = id(trip.getId());
        ArrayList<Hop> hops = new ArrayList<Hop>();
        boolean tripWheelchairAccessible = trip.getWheelchairAccessible() != 0;

        for (int i = 0; i < stopTimes.size() - 1; i++) {
            StopTime st0 = stopTimes.get(i);
            Stop s0 = st0.getStop();
            StopTime st1 = stopTimes.get(i + 1);
            Stop s1 = st1.getStop();
            Vertex startStation = graph.getVertex(departureVertexId(id(s0.getId())));
            Vertex endStation = graph.getVertex(arrivalVertexId(id(s1.getId())));

            // create journey vertices
            Vertex startJourneyArrive = graph.addVertex(id(s0.getId()) + "_" + tripId + "_" + st0.getStopSequence() + "_A",  s0.getName(), s0.getId().getId(), s0.getLon(),
                    s0.getLat());
            Vertex startJourneyDepart = graph.addVertex(id(s0.getId()) + "_" + tripId + "_" + st0.getStopSequence() + "_D", s0.getName(), s0.getId().getId(), s0.getLon(),
                    s0.getLat());
            Vertex endJourneyArrive = graph.addVertex(id(s1.getId()) + "_" + tripId + "_" + st1.getStopSequence() + "_A",  s1.getName(), s1.getId().getId(), s1.getLon(), s1
                    .getLat());

            Dwell dwell = new Dwell(startJourneyArrive, startJourneyDepart, st0);
            graph.addEdge(dwell);
            Hop hop = new Hop(startJourneyDepart, endJourneyArrive, st0, st1);
            hop.setFareContext(fareContext);
            hop.setGeometry(getHopGeometry(trip.getShapeId(), st0, st1, startJourneyDepart,
                    endJourneyArrive));
            hops.add(hop);

            if (st0.getPickupType() != 1) {
                Board boarding = new Board(startStation, startJourneyDepart, hop,
                        tripWheelchairAccessible && s0.getWheelchairBoarding() != 0,
                        st0.getStop().getZoneId(), trip, fareContext);
                graph.addEdge(boarding);
            }
            if (st0.getDropOffType() != 1) {
                graph.addEdge(new Alight(endJourneyArrive, endStation, hop, tripWheelchairAccessible
                        && s1.getWheelchairBoarding() != 0,
                        st0.getStop().getZoneId(), trip, fareContext));
            }
        }
    }

    private Geometry getHopGeometry(AgencyAndId shapeId, StopTime st0, StopTime st1,
            Vertex startJourney, Vertex endJourney) {

        if (shapeId == null || shapeId.getId() == null || shapeId.getId().equals(""))
            return null;

        double startDistance = st0.getShapeDistTraveled();
        double endDistance = st1.getShapeDistTraveled();

        boolean hasShapeDist = st0.isShapeDistTraveledSet() && st1.isShapeDistTraveledSet();

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

        Geometry geometry = _geometriesByShapeSegmentKey.get(key);
        if (geometry == null) {

            geometry = locationIndexedLine.extractLine(startIndex, endIndex);

            // Pack the resulting line string
            CoordinateSequence sequence = new PackedCoordinateSequence.Float(geometry
                    .getCoordinates(), 2);
            geometry = _factory.createLineString(sequence);

            _geometriesByShapeSegmentKey.put(key, (LineString) geometry);
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
            if (!point.isDistTraveledSet())
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

        double prevDistance = distances[index - 1];
        if (prevDistance == distances[index]) {
            return new LinearLocation(index - 1, 1.0);
        }
        double indexPart = (distance - distances[index - 1])
                / (distances[index] - prevDistance);
        return new LinearLocation(index - 1, indexPart);
    }

    private StopTime cloneStopTime(StopTime original) {
        StopTime anew = new StopTime();
        anew.setTrip(original.getTrip());
        anew.setStopSequence(original.getStopSequence());
        anew.setStopHeadsign(original.getStopHeadsign());
        anew.setStop(original.getStop());
        anew.setRouteShortName(original.getRouteShortName());
        anew.setPickupType(original.getPickupType());
        anew.setId(original.getId());
        anew.setDropOffType(original.getDropOffType());

        if(original.isShapeDistTraveledSet())
            anew.setShapeDistTraveled(original.getShapeDistTraveled());
        if(original.isArrivalTimeSet())
            anew.setArrivalTime(original.getArrivalTime());
        if(original.isDepartureTimeSet())
            anew.setDepartureTime(original.getDepartureTime());

        return anew;
    }
}
