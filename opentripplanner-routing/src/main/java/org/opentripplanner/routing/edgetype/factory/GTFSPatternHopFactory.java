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

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.Pathway;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Transfer;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.GraphBuilderAnnotation;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.GraphBuilderAnnotation.Variety;
import org.opentripplanner.routing.edgetype.Alight;
import org.opentripplanner.routing.edgetype.BasicTripPattern;
import org.opentripplanner.routing.edgetype.Board;
import org.opentripplanner.routing.edgetype.Dwell;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.FrequencyAlight;
import org.opentripplanner.routing.edgetype.FrequencyBasedTripPattern;
import org.opentripplanner.routing.edgetype.FrequencyBoard;
import org.opentripplanner.routing.edgetype.FrequencyDwell;
import org.opentripplanner.routing.edgetype.FrequencyHop;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.opentripplanner.routing.vertextype.TransitVertex;
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
 * A ScheduledStopPattern is an intermediate object used when processing GTFS files. It represents an ordered
 * list of stops and a service ID. Any two trips with the same stops in the same order, and that
 * operate on the same days, can be combined using a TripPattern to save memory.
 */

class ScheduledStopPattern {
    ArrayList<Stop> stops;
    ArrayList<Integer> pickups;
    ArrayList<Integer> dropoffs;

    AgencyAndId calendarId;

    public ScheduledStopPattern(ArrayList<Stop> stops, ArrayList<Integer> pickups, ArrayList<Integer> dropoffs, AgencyAndId calendarId) {
        this.stops = stops;
        this.pickups = pickups;
        this.dropoffs = dropoffs;
        this.calendarId = calendarId;
    }

    public boolean equals(Object other) {
        if (other instanceof ScheduledStopPattern) {
            ScheduledStopPattern pattern = (ScheduledStopPattern) other;
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
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof InterliningTrip) {
            return compareTo((InterliningTrip) o) == 0;
        }
        return false;
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

    private FareServiceFactory fareServiceFactory;

    private HashMap<BlockIdAndServiceId, List<InterliningTrip>> tripsForBlock = new HashMap<BlockIdAndServiceId, List<InterliningTrip>>();

    private Map<InterlineSwitchoverKey, PatternInterlineDwell> interlineDwells = new HashMap<InterlineSwitchoverKey, PatternInterlineDwell>();

    HashMap<ScheduledStopPattern, BasicTripPattern> patterns = new HashMap<ScheduledStopPattern, BasicTripPattern>();

    /* maps replacing label lookup */
    private Map<Stop, Vertex> stopNodes = new HashMap<Stop, Vertex>();
    Map<Stop, TransitStopArrive> stopArriveNodes = new HashMap<Stop, TransitStopArrive>();
    Map<Stop, TransitStopDepart> stopDepartNodes = new HashMap<Stop, TransitStopDepart>();
    Map<T2<Stop, Trip>, Vertex> patternArriveNodes = new HashMap<T2<Stop, Trip>, Vertex>(); 
    Map<T2<Stop, Trip>, Vertex> patternDepartNodes = new HashMap<T2<Stop, Trip>, Vertex>(); // exemplar trip

    private HashSet<Stop> stops = new HashSet<Stop>();

    private int defaultStreetToStopTime;

    public GTFSPatternHopFactory(GtfsContext context) {
        _dao = context.getDao();
    }

    public static ScheduledStopPattern stopPatternfromTrip(Trip trip, GtfsRelationalDao dao) {
        ArrayList<Stop> stops = new ArrayList<Stop>();
        ArrayList<Integer> pickups = new ArrayList<Integer>();
        ArrayList<Integer> dropoffs = new ArrayList<Integer>();
        for (StopTime stoptime : dao.getStopTimesForTrip(trip)) {
            stops.add(stoptime.getStop());
            pickups.add(stoptime.getPickupType());
            dropoffs.add(stoptime.getDropOffType());
        }
        ScheduledStopPattern pattern = new ScheduledStopPattern(stops, pickups, dropoffs, trip.getServiceId());
        return pattern;
    }

    /**
     * Generate the edges. Assumes that there are already vertices in the graph for the stops.
     */
    public void run(Graph graph) {
        if (fareServiceFactory == null) {
            fareServiceFactory = new DefaultFareServiceFactory();
        }
        fareServiceFactory.setDao(_dao);

        // Load stops
        loadStops(graph);
        loadPathways(graph);

        loadAgencies(graph);
        
        // Load hops
        _log.debug("Loading hops");

        clearCachedData();

        /*
         * For each trip, create either pattern edges, the entries in a trip pattern's list of
         * departures, or simple hops
         */

        // Load hops
        Collection<Trip> trips = _dao.getAllTrips();

        int index = 0;

        HashMap<Trip, List<Frequency>> tripFrequencies = new HashMap<Trip, List<Frequency>>();
        for(Frequency freq : _dao.getAllFrequencies()) {
            List<Frequency> freqs = tripFrequencies.get(freq.getTrip());
            if(freqs == null) {
                freqs = new ArrayList<Frequency>();
                tripFrequencies.put(freq.getTrip(), freqs);
            }
            freqs.add(freq);
        }

        for (Trip trip : trips) {

            index++;
            if (index % 100000 == 0)
                _log.debug("trips=" + index + "/" + trips.size());

            List<StopTime> stopTimes = getNonduplicateStopTimesForTrip(trip);
            filterStopTimes(stopTimes, graph);
            interpolateStopTimes(stopTimes);
            if (stopTimes.size() < 2) {
                _log.warn(GraphBuilderAnnotation.register(graph, Variety.TRIP_DEGENERATE, trip));
                continue;
            }

            List<Frequency> frequencies = tripFrequencies.get(trip);

            ScheduledStopPattern stopPattern = stopPatternfromTrip(trip, _dao);
            BasicTripPattern tripPattern = patterns.get(stopPattern);
            String blockId = trip.getBlockId();

            if(frequencies != null) {
                //before creating frequency-based trips, check for
                //single-instance frequencies.
                Frequency frequency = frequencies.get(0);
                if (frequencies.size() > 1 || frequency.getStartTime() != stopTimes.get(0).getDepartureTime() ||
                        frequency.getEndTime() - frequency.getStartTime() > frequency.getHeadwaySecs()) {

                    FrequencyBasedTripPattern frequencyPattern = makeFrequencyPattern(graph, trip,
                            stopTimes);
                    if (frequencyPattern == null) {
                        continue;
                    }

                    frequencyPattern.createRanges(frequencies);
                continue;
                }
            }

            boolean simple = false;

            if (tripPattern == null) {
                tripPattern = makeTripPattern(graph, trip, stopTimes);
                if (tripPattern == null) {
                    continue;
                }
                patterns.put(stopPattern, tripPattern);
                if (blockId != null && !blockId.equals("")) {
                    addTripToInterliningMap(trip, stopTimes, tripPattern);
                }
            } else {
                int insertionPoint = tripPattern.getDepartureTimeInsertionPoint(stopTimes.get(0)
                        .getDepartureTime());
                if (insertionPoint < 0) {
                    insertionPoint = -(insertionPoint + 1);
                    // There's already a departure at this time on this trip pattern. This means
                    // that either (a) this will have all the same stop times as that one, and thus
                    // will be a duplicate of it, or (b) it will have different stops, and thus
                    // break the assumption that trips are non-overlapping.
                    if (!tripPattern.stopTimesIdentical(stopTimes, insertionPoint)) {
                        _log.warn(GraphBuilderAnnotation.register(graph,
                                Variety.TRIP_DUPLICATE_DEPARTURE, trip.getId(),
                                tripPattern.getTrip(insertionPoint)));
                        simple = true;
                        createSimpleHops(graph, trip, stopTimes);
                    } else {
                        _log.warn(GraphBuilderAnnotation.register(graph, Variety.TRIP_DUPLICATE,
                                trip.getId(), tripPattern.getTrip(insertionPoint)));
                        simple = true;
                    }
                } else {
                    // try to insert this trip at this location
                    StopTime st1 = null;
                    int i;
                    for (i = 0; i < stopTimes.size() - 1; i++) {
                        StopTime st0 = stopTimes.get(i);
                        st1 = stopTimes.get(i + 1);

                        int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
                        int runningTime = st1.getArrivalTime() - st0.getDepartureTime();

                        if (runningTime < 0) {
                            _log.warn(GraphBuilderAnnotation.register(graph,
                                    Variety.NEGATIVE_HOP_TIME, st0, st1));
                            // back out hops and give up
                            for (i = i - 1; i >= 0; --i) {
                                tripPattern.removeHop(i, insertionPoint);
                            }
                            simple = true;
                            break;
                        }
                        if (dwellTime < 0) {
                            _log.warn(GraphBuilderAnnotation.register(graph,
                                    Variety.NEGATIVE_DWELL_TIME, st0));
                            dwellTime = 0;
                        }

                        try {
                            tripPattern.addHop(i, insertionPoint, st0.getDepartureTime(),
                                    runningTime, st1.getArrivalTime(), dwellTime,
                                    st0.getStopHeadsign(), trip);
                        } catch (TripOvertakingException e) {
                            _log.warn(GraphBuilderAnnotation.register(graph,
                                    Variety.TRIP_OVERTAKING, e.overtaker, e.overtaken, e.stopIndex));
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
                    tripPattern.setTripFlags(insertionPoint,
                            ((trip.getWheelchairAccessible() == 1) ? TripPattern.FLAG_WHEELCHAIR_ACCESSIBLE
                                    : 0)
                                    | (((trip.getRoute().getBikesAllowed() == 2 && trip
                                    .getTripBikesAllowed() != 1) || trip
                                    .getTripBikesAllowed() == 2) ? TripPattern.FLAG_BIKES_ALLOWED
                                            : 0));
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
                
                if (st0.getPickupType() == 1) {
                    // do not create an interline dwell when
                    // the last stop on the arriving trip does not
                    // allow pickups, since this probably means
                    // that, while two trips share a block,
                    // riders cannot stay on the vehicle during the
                    // deadhead
                    continue;
                }

                Trip fromExemplar = fromInterlineTrip.tripPattern.exemplar;
                Trip toExemplar = toInterlineTrip.tripPattern.exemplar;

                PatternInterlineDwell dwell;
                // do we already have a PID for this dwell?
                InterlineSwitchoverKey dwellKey = new InterlineSwitchoverKey(s0, s1, fromInterlineTrip.tripPattern, toInterlineTrip.tripPattern);
                dwell = getInterlineDwell(dwellKey);
                if (dwell == null) {
                    // create the dwell
                    Vertex startJourney = patternArriveNodes.get(new T2<Stop, Trip>(s0,fromExemplar));
                    Vertex endJourney = patternDepartNodes.get(new T2<Stop, Trip>(s1,toExemplar));
                    dwell = new PatternInterlineDwell(startJourney, endJourney, toTrip);
                    putInterlineDwell(dwellKey, dwell);
                }
                int dwellTime = st1.getDepartureTime() - st0.getArrivalTime();
                dwell.addTrip(fromTrip.getId(), toTrip.getId(), dwellTime, 
                        fromInterlineTrip.getPatternIndex(), toInterlineTrip.getPatternIndex());

            }
        }

        loadTransfers(graph);
        deleteUselessDwells(graph);
        clearCachedData();
        graph.putService(FareService.class, fareServiceFactory.makeFareService());
      }

    private FrequencyBasedTripPattern makeFrequencyPattern(Graph graph, Trip trip,
            List<StopTime> stopTimes) {
        
        FrequencyBasedTripPattern pattern = new FrequencyBasedTripPattern(trip, stopTimes.size());
        TraverseMode mode = GtfsLibrary.getTraverseMode(trip.getRoute());
        int lastStop = stopTimes.size() - 1;

        int i;
        StopTime st1 = null;
        TransitVertex psv0arrive, psv1arrive = null;
        TransitVertex psv0depart;
        ArrayList<Edge> createdEdges = new ArrayList<Edge>();
        ArrayList<Vertex> createdVertices = new ArrayList<Vertex>();
        
        int offset = stopTimes.get(0).getDepartureTime();
        for (i = 0; i < lastStop; i++) {           
            StopTime st0 = stopTimes.get(i);
            Stop s0 = st0.getStop();
            st1 = stopTimes.get(i + 1);
            Stop s1 = st1.getStop();

            int arrivalTime = st1.getArrivalTime() - offset;
            int departureTime = st0.getDepartureTime() - offset;

            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
            int runningTime = arrivalTime - departureTime;

            if (runningTime < 0) {
                _log.warn(GraphBuilderAnnotation.register(graph,
                        Variety.NEGATIVE_HOP_TIME, st0, st1));
                //back out hops and give up
                for (Edge e: createdEdges) {
                    e.getFromVertex().removeOutgoing(e);
                    e.getToVertex().removeIncoming(e);
                }
                for (Vertex v : createdVertices) {
                    graph.removeVertexAndEdges(v);
                }
                return null;
            }

            // create journey vertices

            if (i != 0) {
                //dwells are possible on stops after the first
                psv0arrive = psv1arrive;
                
                if (dwellTime == 0) {
                    //if dwell time is zero, we would like to not create psv0depart
                    //use psv0arrive instead
                    psv0depart = psv0arrive;
                } else {

                    psv0depart = new PatternDepartVertex(graph, trip, st0);
                    createdVertices.add(psv0depart);
                    FrequencyDwell dwell = new FrequencyDwell(psv0arrive, psv0depart, i, pattern);
                    createdEdges.add(dwell);
                }
            } else {
                psv0depart = new PatternDepartVertex(graph, trip, st0);
                createdVertices.add(psv0depart);
            }

            psv1arrive = new PatternArriveVertex(graph, trip, st1);
            createdVertices.add(psv1arrive);

            FrequencyHop hop = new FrequencyHop(psv0depart, psv1arrive, s0, s1, i,
                    pattern);
            createdEdges.add(hop);
            hop.setGeometry(getHopGeometry(graph, trip.getShapeId(), st0, st1, psv0depart,
                    psv1arrive));

            pattern.addHop(i, departureTime, runningTime, arrivalTime, dwellTime,
                    st0.getStopHeadsign());

            TransitStopDepart stopDepart = stopDepartNodes.get(s0);
            TransitStopArrive stopArrive = stopArriveNodes.get(s1);

            Edge board = new FrequencyBoard(stopDepart, psv0depart, pattern, i, mode);
            Edge alight = new FrequencyAlight(psv1arrive, stopArrive, pattern, i, mode);
            createdEdges.add(board);
            createdEdges.add(alight);
        }

        pattern.setTripFlags(((trip.getWheelchairAccessible() == 1) ? TripPattern.FLAG_WHEELCHAIR_ACCESSIBLE : 0)
        | (((trip.getRoute().getBikesAllowed() == 2 && trip.getTripBikesAllowed() != 1)
            || trip.getTripBikesAllowed() == 2) ? TripPattern.FLAG_BIKES_ALLOWED : 0));

        return pattern;
    }

    /**
     * scan through the given list, looking for clearly incorrect series of stoptimes 
     * and unsetting / annotating them. unsetting the arrival/departure time of clearly incorrect
     * stoptimes will cause them to be interpolated in the next step. 
     *  
     * @param stopTimes the stoptimes to be filtered (from a single trip)
     * @param graph the graph where annotations will be registered
     */
    private void filterStopTimes(List<StopTime> stopTimes, Graph graph) {
        if (stopTimes.size() < 2)
            return;
        StopTime st0 = stopTimes.get(0);
        if (!st0.isDepartureTimeSet() && st0.isArrivalTimeSet()) {
            /* set depature time if it is missing */
            st0.setDepartureTime(st0.getArrivalTime());
        }
        for (int i = 1; i < stopTimes.size(); i++) {
            boolean st1bogus = false;
            StopTime st1 = stopTimes.get(i);
            if (!st1.isDepartureTimeSet() && st1.isArrivalTimeSet()) {
                /* set depature time if it is missing */
                st1.setDepartureTime(st1.getArrivalTime());
            }
            /* do not process non-timepoint stoptimes, 
             * which are of course identical to other adjacent non-timepoint stoptimes */
            if ( ! (st1.isArrivalTimeSet() && st1.isDepartureTimeSet())) {
                continue;
            }
            int runningTime = st1.getArrivalTime() - st0.getDepartureTime();
            double hopDistance = DistanceLibrary.fastDistance(
                   st0.getStop().getLon(), st0.getStop().getLat(),
                   st1.getStop().getLon(), st1.getStop().getLat());
            double hopSpeed = hopDistance/runningTime;
            /* zero-distance hops are probably not harmful, though they could be better represented as dwell times
            if (hopDistance == 0) {
                _log.warn(GraphBuilderAnnotation.register(graph, 
                        Variety.HOP_ZERO_DISTANCE, runningTime, 
                        st1.getTrip().getId(), 
                        st1.getStopSequence()));
            } 
            */
            // sanity-check the hop
            if (st0.getArrivalTime() == st1.getArrivalTime() ||
                st0.getDepartureTime() == st1.getDepartureTime()) {
                _log.trace("{} {}", st0, st1);
                // series of identical stop times at different stops
                _log.trace(GraphBuilderAnnotation.register(graph, 
                          Variety.HOP_ZERO_TIME, hopDistance, 
                          st1.getTrip().getRoute(), 
                          st1.getTrip().getId(), st1.getStopSequence()));
                // clear stoptimes that are obviously wrong, causing them to later be interpolated
/* FIXME (lines commented out because they break routability in multi-feed NYC for some reason -AMB) */
//                st1.clearArrivalTime();
//                st1.clearDepartureTime();
                st1bogus = true;
            } else if (hopSpeed > 45) {
                // 45 m/sec ~= 100 miles/hr
                // elapsed time of 0 will give speed of +inf
                _log.trace(GraphBuilderAnnotation.register(graph, 
                          Variety.HOP_SPEED, hopSpeed, hopDistance,
                          st0.getTrip().getRoute(), 
                          st0.getTrip().getId(), st0.getStopSequence()));
            }
            // st0 should reflect the last stoptime that was not clearly incorrect
            if ( ! st1bogus)  
                st0 = st1;
        } // END for loop over stop times
    }

    private void loadAgencies(Graph graph) {
        for (Agency agency : _dao.getAllAgencies()) {
            graph.addAgencyId(agency.getId());
        }
    }

    private void putInterlineDwell(InterlineSwitchoverKey key, PatternInterlineDwell dwell) {
        interlineDwells.put(key, dwell);
    }

    private PatternInterlineDwell getInterlineDwell(InterlineSwitchoverKey key) {
        return interlineDwells.get(key);
    }

    private void loadPathways(Graph graph) {
        for (Pathway pathway : _dao.getAllPathways()) {
            Vertex fromVertex = stopNodes.get(pathway.getFromStop());
            Vertex toVertex = stopNodes.get(pathway.getToStop());
            if (pathway.isWheelchairTraversalTimeSet()) {
                new PathwayEdge(fromVertex, toVertex, pathway.getTraversalTime());
            } else {
                new PathwayEdge(fromVertex, toVertex, pathway.getTraversalTime(), pathway.getWheelchairTraversalTime());
            }
        }
    }

    private void loadStops(Graph graph) {
        for (Stop stop : _dao.getAllStops()) {
            if (stops.contains(stop)) {
                continue;
            }
            stops.add(stop);
            //add a vertex representing the stop
            TransitStop stopVertex = new TransitStop(graph, stop);
            stopVertex.setStreetToStopTime(defaultStreetToStopTime);
            stopNodes.put(stop, stopVertex);
            
            if (stop.getLocationType() != 2) {
                //add a vertex representing arriving at the stop
                TransitStopArrive arrive = new TransitStopArrive(graph, stop);
                stopArriveNodes.put(stop, arrive);

                //add a vertex representing departing from the stop
                TransitStopDepart depart = new TransitStopDepart(graph, stop);
                stopDepartNodes.put(stop, depart);

                //add edges from arrive to stop and stop to depart
                new PreAlightEdge(arrive, stopVertex);
                new PreBoardEdge(stopVertex, depart);
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

            Vertex v = dwell.getFromVertex();
            v.mergeFrom(graph, dwell.getToVertex());
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

    /**
     * scan through the given list of stoptimes, interpolating the missing ones.
     * this is currently done by assuming equidistant stops and constant speed.
     * while we may not be able to improve the constant speed assumption,
     * TODO: use route matching (or shape distance etc.) to improve inter-stop distances
     *  
     * @param stopTimes the stoptimes to be filtered (from a single trip)
     */
    private void interpolateStopTimes(List<StopTime> stopTimes) {
        int lastStop = stopTimes.size() - 1;
        int numInterpStops = -1;
        int departureTime = -1, prevDepartureTime = -1;
        int interpStep = 0;

        int i;
        for (i = 0; i < lastStop; i++) {
            StopTime st0 = stopTimes.get(i);

            prevDepartureTime = departureTime;
            departureTime = st0.getDepartureTime();

            /* Interpolate, if necessary, the times of non-timepoint stops */
            /* genuine interpolation needed */
            if (!(st0.isDepartureTimeSet() && st0.isArrivalTimeSet())) {
                // figure out how many such stops there are in a row.
                int j;
                StopTime st = null;
                for (j = i + 1; j < lastStop + 1; ++j) {
                    st = stopTimes.get(j);
                    if ((st.isDepartureTimeSet() && st.getDepartureTime() != departureTime)
                            || (st.isArrivalTimeSet() && st.getArrivalTime() != departureTime)) {
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

    private BasicTripPattern makeTripPattern(Graph graph, Trip trip, List<StopTime> stopTimes) {
        BasicTripPattern tripPattern = new BasicTripPattern(trip, stopTimes);

        TraverseMode mode = GtfsLibrary.getTraverseMode(trip.getRoute());
        int lastStop = stopTimes.size() - 1;

        int i;
        StopTime st1 = null;
        PatternArriveVertex psv0arrive, psv1arrive = null;
        PatternDepartVertex psv0depart;
        ArrayList<Edge> createdEdges = new ArrayList<Edge>();
        ArrayList<Vertex> createdVertices = new ArrayList<Vertex>();
        for (i = 0; i < lastStop; i++) {           
            StopTime st0 = stopTimes.get(i);
            Stop s0 = st0.getStop();
            st1 = stopTimes.get(i + 1);
            Stop s1 = st1.getStop();

            int arrivalTime = st1.getArrivalTime();
            int departureTime = st0.getDepartureTime();

            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
            int runningTime = arrivalTime - departureTime ;

            if (runningTime < 0) {
                _log.warn(GraphBuilderAnnotation.register(graph,
                        Variety.NEGATIVE_HOP_TIME, st0, st1));
                //back out hops and give up
                for (Edge e: createdEdges) {
                    e.getFromVertex().removeOutgoing(e);
                    e.getToVertex().removeIncoming(e);
                }
                for (Vertex v : createdVertices) {
                    graph.removeVertexAndEdges(v);
                }
                return null;
            }

            // create journey vertices

            psv0depart = new PatternDepartVertex(graph, tripPattern, st0);
            createdVertices.add(psv0depart);
            patternDepartNodes.put(new T2<Stop, Trip>(s0, trip), psv0depart);
            
            if (i != 0) {
                psv0arrive = psv1arrive;
                PatternDwell dwell = new PatternDwell(psv0arrive, psv0depart, i, tripPattern);
                createdEdges.add(dwell);
                if (dwellTime == 0) {
                    potentiallyUselessDwells.add(dwell);
                }
            }

            psv1arrive = new PatternArriveVertex(graph, tripPattern, st1);
            createdVertices.add(psv1arrive);
            patternArriveNodes.put(new T2<Stop, Trip>(s1, trip), psv1arrive);

            PatternHop hop = new PatternHop(psv0depart, psv1arrive, s0, s1, i,
                    tripPattern);
            createdEdges.add(hop);
            hop.setGeometry(getHopGeometry(graph, trip.getShapeId(), st0, st1, psv0depart,
                    psv1arrive));

            tripPattern.addHop(i, 0, departureTime, runningTime, arrivalTime, dwellTime,
                    st0.getStopHeadsign(), trip);

            TransitStopDepart stopDepart = stopDepartNodes.get(s0);
            TransitStopArrive stopArrive = stopArriveNodes.get(s1);

            Edge board = new PatternBoard(stopDepart, psv0depart, tripPattern, i, mode);
            Edge alight = new PatternAlight(psv1arrive, stopArrive, tripPattern, i, mode);
            createdEdges.add(board);
            createdEdges.add(alight);
        }

        tripPattern.setTripFlags(0, 
                                ((trip.getWheelchairAccessible() == 1) ? TripPattern.FLAG_WHEELCHAIR_ACCESSIBLE : 0)
                                | (((trip.getRoute().getBikesAllowed() == 2 && trip.getTripBikesAllowed() != 1)
                                    || trip.getTripBikesAllowed() == 2) ? TripPattern.FLAG_BIKES_ALLOWED : 0));

        return tripPattern;
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
            Vertex fromVertex = stopArriveNodes.get(fromStop);
            Vertex toVertex = stopDepartNodes.get(toStop);
            switch (t.getTransferType()) {
            case 1:
                // timed (synchronized) transfer 
                // Handle with edges that bypass the street network.
                // from and to vertex here are stop_arrive and stop_depart vertices
                new TimedTransferEdge(fromVertex, toVertex);
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

        ArrayList<Hop> hops = new ArrayList<Hop>();
        boolean tripWheelchairAccessible = trip.getWheelchairAccessible() == 1;

        PatternStopVertex psv0arrive, psv0depart, psv1arrive = null;
        ArrayList<Edge> created = new ArrayList<Edge>();
        
        for (int i = 0; i < stopTimes.size() - 1; i++) {
            StopTime st0 = stopTimes.get(i);
            Stop s0 = st0.getStop();
            StopTime st1 = stopTimes.get(i + 1);
            Stop s1 = st1.getStop();

            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
            int runningTime = st1.getArrivalTime() - st0.getDepartureTime();
            if (runningTime < 0) {
                _log.warn(GraphBuilderAnnotation.register(graph,
                        Variety.NEGATIVE_HOP_TIME, st0, st1));
                //back out trip and give up
                for (Edge e: created) {
                    e.getFromVertex().removeOutgoing(e);
                    e.getToVertex().removeIncoming(e);
                }
                return;
            }
            if (dwellTime < 0) {
                _log.warn(GraphBuilderAnnotation.register(graph, Variety.NEGATIVE_DWELL_TIME, st0));
                dwellTime = 0;
            }

            Vertex startStation = stopDepartNodes.get(s0);
            Vertex endStation = stopArriveNodes.get(s1);

            // create and connect journey vertices
            if (psv1arrive == null) { 
                // first iteration
                psv0arrive = new PatternArriveVertex(graph, trip, st0); 
            } else { 
                // subsequent iterations
                psv0arrive = psv1arrive; 
            }
            psv0depart = new PatternDepartVertex(graph, trip, st0);
            psv1arrive = new PatternArriveVertex(graph, trip, st1);

            new Dwell(psv0arrive, psv0depart, st0);
            Hop hop = new Hop(psv0depart, psv1arrive, st0, st1, trip);
            created.add(hop);
            hop.setGeometry(getHopGeometry(graph, trip.getShapeId(), st0, st1, psv0depart,
                    psv1arrive));
            hops.add(hop);

            if (st0.getPickupType() != 1) {
                Edge board = new Board(startStation, psv0depart, hop,
                    tripWheelchairAccessible && s0.getWheelchairBoarding() == 1,
                    st0.getStop().getZoneId(), trip, st0.getPickupType());
                created.add(board);
            }
            if (st0.getDropOffType() != 1) {
                Edge alight = new Alight(psv1arrive, endStation, hop, 
                    tripWheelchairAccessible && s1.getWheelchairBoarding() == 1,
                    st0.getStop().getZoneId(), trip, st0.getDropOffType());
                created.add(alight);
            }
        }
    }

    private Geometry getHopGeometry(Graph graph, AgencyAndId shapeId, StopTime st0, StopTime st1,
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

            if (distances == null) {
                _log.warn(GraphBuilderAnnotation.register(graph, 
                        Variety.BOGUS_SHAPE_GEOMETRY, shapeId));
                return null;
            } else {
                LinearLocation startIndex = getSegmentFraction(distances, startDistance);
                LinearLocation endIndex = getSegmentFraction(distances, endDistance);

                LineString line = getLineStringForShapeId(shapeId);
                LocationIndexedLine lol = new LocationIndexedLine(line);

                return getSegmentGeometry(shapeId, lol, startIndex, endIndex, startDistance,
                        endDistance);
            }
        }

        LineString line = getLineStringForShapeId(shapeId);
        if (line == null) {
            _log.warn(GraphBuilderAnnotation.register(graph, 
                    Variety.BOGUS_SHAPE_GEOMETRY, shapeId));
            return null;
        }
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
    
    /* 
     * If a shape appears in more than one feed, the shape points will be loaded several
     * times, and there will be duplicates in the DAO. Filter out duplicates and repeated
     * coordinates because 1) they are unnecessary, and 2) they define 0-length line segments
     * which cause JTS location indexed line to return a segment location of NaN, 
     * which we do not want.
     */
    private List<ShapePoint> getUniqueShapePointsForShapeId(AgencyAndId shapeId) {
        List<ShapePoint> points = _dao.getShapePointsForShapeId(shapeId);
        ArrayList<ShapePoint> filtered = new ArrayList<ShapePoint>(points.size());
        ShapePoint last = null;
        for (ShapePoint sp : points) {
            if (last == null || last.getSequence() != sp.getSequence()) {
                if (last != null && 
                    last.getLat() == sp.getLat() && 
                    last.getLon() == sp.getLon()) {
                    _log.trace("pair of identical shape points (skipping): {} {}", last, sp);
                } else {
                    filtered.add(sp);
                }
            }
            last = sp;
        }
        if (filtered.size() != points.size()) {
            filtered.trimToSize();
            return filtered;
        } else {
            return points;
        }
    }

    private LineString getLineStringForShapeId(AgencyAndId shapeId) {

        LineString geometry = _geometriesByShapeId.get(shapeId);

        if (geometry != null) 
            return geometry;

        List<ShapePoint> points = getUniqueShapePointsForShapeId(shapeId);
        if (points.size() < 2) {
            return null;
        }
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

    private List<StopTime> getNonduplicateStopTimesForTrip(Trip trip) {
        List<StopTime> unfiltered = _dao.getStopTimesForTrip(trip);
        ArrayList<StopTime> filtered = new ArrayList<StopTime>(unfiltered.size());
        for (StopTime st : unfiltered) {
            if (filtered.size() > 0) {
                StopTime lastStopTime = filtered.get(filtered.size() - 1);
                if (lastStopTime.getStop().equals(st.getStop())) {
                    lastStopTime.setDepartureTime(st.getDepartureTime());
                } else {
                    filtered.add(st);
                }
            } else {
                filtered.add(st);
            }
        }
        if (filtered.size() == unfiltered.size()) {
            return unfiltered;
        }
        return filtered;
    }

    public void setFareServiceFactory(FareServiceFactory fareServiceFactory) {
        this.fareServiceFactory = fareServiceFactory;
    }

    /**
     * Create transfer edges between stops which are listed in transfers.txt.
     * This is not usually useful, but it's nice for the NYC subway system, where
     * it's important to provide in-station transfers for fare computation.
     */
    public void createStationTransfers(Graph graph) {

        /* connect stops to their parent stations
         * TODO: provide a cost for these edges when stations and
         * stops have different locations 
         */
        for (Stop stop : _dao.getAllStops()) {
            String parentStation = stop.getParentStation();
            if (parentStation != null) {
                Vertex stopVertex = stopNodes.get(stop);

                String agencyId = stop.getId().getAgencyId();
                AgencyAndId parentStationId = new AgencyAndId(agencyId, parentStation);

                Stop parentStop = _dao.getStopForId(parentStationId);
                Vertex parentStopVertex = stopNodes.get(parentStop);

                new FreeEdge(parentStopVertex, stopVertex);
                new FreeEdge(stopVertex, parentStopVertex);

                Vertex stopArriveVertex = stopArriveNodes.get(stop);
                Vertex parentStopArriveVertex = stopArriveNodes.get(parentStop);

                new FreeEdge(parentStopArriveVertex, stopArriveVertex);
                new FreeEdge(stopArriveVertex, parentStopArriveVertex);

                Vertex stopDepartVertex = stopDepartNodes.get(stop);
                Vertex parentStopDepartVertex = stopDepartNodes.get(parentStop);

                new FreeEdge(parentStopDepartVertex, stopDepartVertex);
                new FreeEdge(stopDepartVertex, parentStopDepartVertex);

            }
        }
        for (Transfer transfer : _dao.getAllTransfers()) {

            int type = transfer.getTransferType();
            if (type == 3)
                continue;

            Vertex fromv = stopArriveNodes.get(transfer.getFromStop());
            Vertex tov = stopDepartNodes.get(transfer.getToStop());

            if (fromv.equals(tov))
                continue;

            double distance = DistanceLibrary.distance(fromv.getCoordinate(), tov.getCoordinate());
            int time;
            if (transfer.getTransferType() == 2) {
                time = transfer.getMinTransferTime();
            } else {
                time = (int) distance; // fixme: handle timed transfers
            }

            TransferEdge transferEdge = new TransferEdge(fromv, tov, distance, time);
            CoordinateSequence sequence = new PackedCoordinateSequence.Float(new Coordinate[] {
                    fromv.getCoordinate(), tov.getCoordinate() }, 2);
            Geometry geometry = _factory.createLineString(sequence);
            transferEdge.setGeometry(geometry);
        }
    }

    public int getDefaultStreetToStopTime() {
        return defaultStreetToStopTime;
    }

    public void setDefaultStreetToStopTime(int defaultStreetToStopTime) {
        this.defaultStreetToStopTime = defaultStreetToStopTime;
    }
}
