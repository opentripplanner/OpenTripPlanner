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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.FastMath;
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
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.gbannotation.BogusShapeDistanceTraveled;
import org.opentripplanner.gbannotation.BogusShapeGeometry;
import org.opentripplanner.gbannotation.BogusShapeGeometryCaught;
import org.opentripplanner.gbannotation.HopSpeedFast;
import org.opentripplanner.gbannotation.HopSpeedSlow;
import org.opentripplanner.gbannotation.HopZeroTime;
import org.opentripplanner.gbannotation.NegativeDwellTime;
import org.opentripplanner.gbannotation.NegativeHopTime;
import org.opentripplanner.gbannotation.StopAtEntrance;
import org.opentripplanner.gbannotation.TripDegenerate;
import org.opentripplanner.gbannotation.TripUndefinedService;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.ServiceIdToNumberService;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.FrequencyAlight;
import org.opentripplanner.routing.edgetype.FrequencyBasedTripPattern;
import org.opentripplanner.routing.edgetype.FrequencyBoard;
import org.opentripplanner.routing.edgetype.FrequencyDwell;
import org.opentripplanner.routing.edgetype.FrequencyHop;
import org.opentripplanner.routing.edgetype.HopEdge;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.ScheduledStopPattern;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.opentripplanner.routing.vertextype.PatternArriveVertex;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
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

class InterliningTrip  implements Comparable<InterliningTrip> {
    public Trip trip;
    public StopTime firstStopTime;
    public StopTime lastStopTime;
    TableTripPattern tripPattern;

    InterliningTrip(Trip trip, List<StopTime> stopTimes, TableTripPattern tripPattern) {
        this.trip = trip;
        this.firstStopTime = stopTimes.get(0);
        this.lastStopTime = stopTimes.get(stopTimes.size() - 1);
        this.tripPattern = tripPattern;
    }

    public int getPatternIndex() {
        return tripPattern.getTripIndex(trip);
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
    public TableTripPattern pattern1, pattern2;

    public InterlineSwitchoverKey(Stop s0, Stop s1, 
        TableTripPattern pattern1, TableTripPattern pattern2) {
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

class IndexedLineSegment {
    private static final double RADIUS = SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M;
    int index;
    Coordinate start;
    Coordinate end;
    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();
    private double lineLength;

    public IndexedLineSegment(int index, Coordinate start, Coordinate end) {
        this.index = index;
        this.start = start;
        this.end = end;
        this.lineLength = distanceLibrary.fastDistance(start, end);
    }

    // in radians
    static double bearing(Coordinate c1, Coordinate c2) {
        double deltaLon = (c2.x - c1.x) * FastMath.PI / 180;
        double lat1Radians = c1.y * FastMath.PI / 180;
        double lat2Radians = c2.y * FastMath.PI / 180;
        double y = FastMath.sin(deltaLon) * FastMath.cos(lat2Radians);
        double x = FastMath.cos(lat1Radians)*FastMath.sin(lat2Radians) -
                FastMath.sin(lat1Radians)*FastMath.cos(lat2Radians)*FastMath.cos(deltaLon);
        return FastMath.atan2(y, x);
    }

    double crossTrackError(Coordinate coord) {
        double distanceFromStart = distanceLibrary.fastDistance(start, coord);
        double bearingToCoord = bearing(start, coord);
        double bearingToEnd = bearing(start, end);
        return FastMath.asin(FastMath.sin(distanceFromStart / RADIUS)
            * FastMath.sin(bearingToCoord - bearingToEnd))
            * RADIUS;
    }

    double distance(Coordinate coord) {
        double cte = crossTrackError(coord);
        double atd = alongTrackDistance(coord, cte);
        double inverseAtd = inverseAlongTrackDistance(coord, -cte);
        double distanceToStart = distanceLibrary.fastDistance(coord, start);
        double distanceToEnd = distanceLibrary.fastDistance(coord, end);

        if (distanceToStart < distanceToEnd) {
            //we might be behind the line start
            if (inverseAtd > lineLength) {
                //we are behind line start
                return distanceToStart;
            } else {
                //we are within line
                return Math.abs(cte);
            }
        } else {
            //we might be after line end
            if (atd > lineLength) {
                //we are behind line end, so we that's the nearest point
                return distanceToEnd;
            } else {
                //we are within line
                return Math.abs(cte);
            }
        }
    }

    private double inverseAlongTrackDistance(Coordinate coord, double inverseCrossTrackError) {
        double distanceFromEnd = distanceLibrary.fastDistance(end, coord);
        double alongTrackDistance = FastMath.acos(FastMath.cos(distanceFromEnd / RADIUS)
            / FastMath.cos(inverseCrossTrackError / RADIUS))
            * RADIUS;
        return alongTrackDistance;
    }

    public double fraction(Coordinate coord) {
        double cte = crossTrackError(coord);
        double distanceToStart = distanceLibrary.fastDistance(coord, start);
        double distanceToEnd = distanceLibrary.fastDistance(coord, end);

        if (cte < distanceToStart && cte < distanceToEnd) {
            double atd = alongTrackDistance(coord, cte);
            return atd / lineLength;
        } else {
            if (distanceToStart < distanceToEnd) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    private double alongTrackDistance(Coordinate coord, double crossTrackError) {
        double distanceFromStart = distanceLibrary.fastDistance(start, coord);
        double alongTrackDistance = FastMath.acos(FastMath.cos(distanceFromStart / RADIUS)
            / FastMath.cos(crossTrackError / RADIUS))
            * RADIUS;
        return alongTrackDistance;
    }
}

class IndexedLineSegmentComparator implements Comparator<IndexedLineSegment> {

    private Coordinate coord;

    public IndexedLineSegmentComparator(Coordinate coord) {
        this.coord = coord;
    }

    @Override
    public int compare(IndexedLineSegment a, IndexedLineSegment b) {
        return (int) FastMath.signum(a.distance(coord) - b.distance(coord));
    }
}

/**
 * Generates a set of edges from GTFS.
 */
public class GTFSPatternHopFactory {

    private static final Logger _log = LoggerFactory.getLogger(GTFSPatternHopFactory.class);

    private static GeometryFactory _geometryFactory = GeometryUtils.getGeometryFactory();

    private GtfsRelationalDao _dao;

    private CalendarService _calendarService;
    
    private Map<ShapeSegmentKey, LineString> _geometriesByShapeSegmentKey = new HashMap<ShapeSegmentKey, LineString>();

    private Map<AgencyAndId, LineString> _geometriesByShapeId = new HashMap<AgencyAndId, LineString>();

    private Map<AgencyAndId, double[]> _distancesByShapeId = new HashMap<AgencyAndId, double[]>();
    
    private boolean _deleteUselessDwells = true;

    private ArrayList<PatternDwell> potentiallyUselessDwells = new ArrayList<PatternDwell> ();

    private FareServiceFactory fareServiceFactory;

    private HashMap<BlockIdAndServiceId, List<InterliningTrip>> tripsForBlock = new HashMap<BlockIdAndServiceId, List<InterliningTrip>>();

    private Map<InterlineSwitchoverKey, PatternInterlineDwell> interlineDwells = new HashMap<InterlineSwitchoverKey, PatternInterlineDwell>();

    HashMap<ScheduledStopPattern, TableTripPattern> patterns = new HashMap<ScheduledStopPattern, TableTripPattern>();

    private GtfsStopContext context = new GtfsStopContext();

    private int defaultStreetToStopTime;

    private static final DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private double maxStopToShapeSnapDistance = 150;

    public GTFSPatternHopFactory(GtfsContext context) {
        this._dao = context.getDao();
        this._calendarService = context.getCalendarService();
    }

    
//  // There's already a departure at this time on this trip pattern. This means
//  // that either (a) this will have all the same stop times as that one, and thus
//  // will be a duplicate of it, or (b) it will have different stops, and thus
//  // break the assumption that trips are non-overlapping.
//  if (!tripPattern.stopTimesIdentical(stopTimes, insertionPoint)) {
//      _log.warn(GraphBuilderAnnotation.register(graph,
//              Variety.TRIP_DUPLICATE_DEPARTURE, trip.getId(),
//              tripPattern.getTrip(insertionPoint)));
//      simple = true;
//      createSimpleHops(graph, trip, stopTimes);
//  } else {
//      _log.warn(GraphBuilderAnnotation.register(graph, Variety.TRIP_DUPLICATE,
//              trip.getId(), tripPattern.getTrip(insertionPoint)));
//      simple = true;
//  }


/* check stoptimes for negative hops and dwells (midnight crossings?) */
//for (int i = 0; i < stopTimes.size() - 1; i++) {
//StopTime st0 = stopTimes.get(i);
//StopTime st1 = stopTimes.get(i + 1);
//
//int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
//int runningTime = st1.getArrivalTime() - st0.getDepartureTime();
//
//if (runningTime < 0) {
//  _log.warn(GraphBuilderAnnotation.register(graph, Variety.NEGATIVE_HOP_TIME, st0, st1));
//  
//  break;
//}
//if (dwellTime < 0) {
//  _log.warn(GraphBuilderAnnotation.register(graph,
//          Variety.NEGATIVE_DWELL_TIME, st0));
//  dwellTime = 0;
//}
//
//try {
//  tripPattern.addHop(i, insertionPoint, st0.getDepartureTime(),
//          runningTime, st1.getArrivalTime(), dwellTime,
//          st0.getStopHeadsign(), trip);
//} catch (TripOvertakingException e) {
//  _log.warn(GraphBuilderAnnotation.register(graph,
//          Variety.TRIP_OVERTAKING, e.overtaker, e.overtaken, e.stopIndex));
//  createSimpleHops(graph, trip, stopTimes);
//  simple = true;
//  break;
//}
//}


    
    
    /** Generate the edges. Assumes that there are already vertices in the graph for the stops. */
    public void run(Graph graph) {
        if (fareServiceFactory == null) {
            fareServiceFactory = new DefaultFareServiceFactory();
        }
        fareServiceFactory.setDao(_dao);

        loadStops(graph);
        loadPathways(graph);
        loadAgencies(graph);
        clearCachedData();

        _log.debug("building hops from trips");
        Collection<Trip> trips = _dao.getAllTrips();
        int tripCount = 0;

        /* first, record which trips are used by one or more frequency entries */
        HashMap<Trip, List<Frequency>> tripFrequencies = new HashMap<Trip, List<Frequency>>();
        for(Frequency freq : _dao.getAllFrequencies()) {
            List<Frequency> freqs = tripFrequencies.get(freq.getTrip());
            if(freqs == null) {
                freqs = new ArrayList<Frequency>();
                tripFrequencies.put(freq.getTrip(), freqs);
            }
            freqs.add(freq);
        }

        /* then loop over all trips handling each one as a frequency-based or scheduled trip */
        TRIP : for (Trip trip : trips) {

            tripCount++;
            if (tripCount % 100000 == 0)
                _log.debug("trips=" + tripCount + "/" + trips.size());
            
            if ( ! _calendarService.getServiceIds().contains(trip.getServiceId())) {
                _log.warn(graph.addBuilderAnnotation(new TripUndefinedService(trip)));
            }


            /* GTFS stop times frequently contain duplicate, missing, or incorrect entries */
            List<StopTime> stopTimes = getNonduplicateStopTimesForTrip(trip); // duplicate stopIds
            filterStopTimes(stopTimes, graph); // duplicate times (0-time), negative, fast or slow hops
            interpolateStopTimes(stopTimes); // interpolate between timepoints
            if (stopTimes.size() < 2) {
                _log.warn(graph.addBuilderAnnotation(new TripDegenerate(trip)));
                continue TRIP;
            }
            
            /* check to see if this trip is used by one or more frequency entries */
            List<Frequency> frequencies = tripFrequencies.get(trip);
            if(frequencies != null) {
                // before creating frequency-based trips, check for single-instance frequencies.
                Collections.sort(frequencies, new Comparator<Frequency>() {

                    @Override
                    public int compare(Frequency o1, Frequency o2) {
                        return o1.getStartTime() - o2.getStartTime();
                    }
                });

                Frequency frequency = frequencies.get(0);
                if (frequencies.size() > 1 || 
                    frequency.getStartTime() != stopTimes.get(0).getDepartureTime() ||
                    frequency.getEndTime() - frequency.getStartTime() > frequency.getHeadwaySecs()) {
                    T2<FrequencyBasedTripPattern,List<FrequencyHop>> patternAndHops =
                            makeFrequencyPattern(graph, trip, stopTimes);
                    List<FrequencyHop> hops = patternAndHops.getSecond();
                    FrequencyBasedTripPattern frequencyPattern = patternAndHops.getFirst();
                    if (frequencyPattern != null) 
                        frequencyPattern.createRanges(frequencies);
                    createGeometry(graph, trip, stopTimes, hops);
                    continue TRIP;
                } // else fall through and treat this as a normal trip
            }

            /* this trip is not frequency-based, add it to the corresponding trip pattern */
            // maybe rename ScheduledStopPattern to TripPatternKey?
            ScheduledStopPattern stopPattern = ScheduledStopPattern.fromTrip(trip, stopTimes);
            TableTripPattern tripPattern = patterns.get(stopPattern);
            if (tripPattern == null) {
                // it's the first time we are encountering this stops+pickups+serviceId combination
                T2<TableTripPattern, List<PatternHop>> patternAndHops = makePatternVerticesAndEdges(graph, trip, stopPattern, stopTimes);
                List<PatternHop> hops = patternAndHops.getSecond();
                createGeometry(graph, trip, stopTimes, hops);
                tripPattern = patternAndHops.getFirst();
                patterns.put(stopPattern, tripPattern);
            } 
            tripPattern.addTrip(trip, stopTimes);

            /* record which block trips belong to so they can be linked up later */
            String blockId = trip.getBlockId();
            if (blockId != null && !blockId.equals("")) {
                addTripToInterliningMap(trip, stopTimes, tripPattern);
            }
        } // END for loop over trips

        /* link up interlined trips (where a vehicle continues on to another logical trip) */
        for (List<InterliningTrip> blockTrips : tripsForBlock.values()) {

            if (blockTrips.size() == 1) { 
                continue; // skip blocks of only a single trip
            }
            Collections.sort(blockTrips); // sort trips within the block by first arrival time 
            
            /* iterate over trips in this block/schedule, linking them */
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
                    /* do not create an interline dwell when the last stop on the arriving trip does
                     * not allow pickups, since this probably means that, while two trips share a
                     * block, riders cannot stay on the vehicle during the deadhead */
                    continue;
                }

                Trip fromExemplar = fromInterlineTrip.tripPattern.exemplar;
                Trip toExemplar = toInterlineTrip.tripPattern.exemplar;

                // make a key representing all interline dwells between these same vertices
                InterlineSwitchoverKey dwellKey = new InterlineSwitchoverKey(
                        s0, s1, fromInterlineTrip.tripPattern, toInterlineTrip.tripPattern);
                // do we already have a PatternInterlineDwell edge for this dwell?
                PatternInterlineDwell dwell = getInterlineDwell(dwellKey);
                if (dwell == null) { 
                    // create the dwell because it does not exist yet
                    Vertex startJourney = context.patternArriveNodes.get(new T2<Stop, Trip>(s0, fromExemplar));
                    Vertex endJourney = context.patternDepartNodes.get(new T2<Stop, Trip>(s1, toExemplar));
                    // toTrip is just an exemplar; dwell edges can contain many trip connections
                    dwell = new PatternInterlineDwell(startJourney, endJourney, toTrip);
                    interlineDwells.put(dwellKey, dwell);
                }
                int dwellTime = st1.getDepartureTime() - st0.getArrivalTime();
                dwell.addTrip(fromTrip.getId(), toTrip.getId(), dwellTime,
                        fromInterlineTrip.getPatternIndex(), toInterlineTrip.getPatternIndex());
            }
        } // END loop over interlining blocks 
        
        loadTransfers(graph);
        if (_deleteUselessDwells) 
            deleteUselessDwells(graph);
//        /* this is the wrong place to do this: it should be done on all feeds at once, or at deserialization*/
//        _log.info("begin indexing large patterns");
//        for (TableTripPattern tp : context.tripPatternIds.keySet()) {
//            tp.finish();
//        }
        _log.info("end indexing large patterns");
        clearCachedData();
        graph.putService(FareService.class, fareServiceFactory.makeFareService());
        graph.putService(ServiceIdToNumberService.class, new ServiceIdToNumberService(context.serviceIds));
    }

    static int cg = 0;
    private <T extends Edge & HopEdge> void createGeometry(Graph graph, Trip trip,
            List<StopTime> stopTimes, List<T> hops) {

        cg += 1;
        AgencyAndId shapeId = trip.getShapeId();
        if (shapeId == null || shapeId.getId() == null || shapeId.getId().equals(""))
            return; // this trip has no associated shape_id, bail out
        // TODO: is this right? don't we want to use the straight-line logic below?
        
        /* Detect presence or absence of shape_dist_traveled on a per-trip basis */
        StopTime st0 = stopTimes.get(0);
        boolean hasShapeDist = st0.isShapeDistTraveledSet();
        if (hasShapeDist) { 
            // this trip has shape_dist in stop_times
            for (int i = 0; i < hops.size(); ++i) {
                Edge hop = hops.get(i);
                st0 = stopTimes.get(i);
                StopTime st1 = stopTimes.get(i + 1);
                ((HopEdge)hop).setGeometry(getHopGeometryViaShapeDistTraveled(graph, shapeId, st0, st1, hop.getFromVertex(), hop.getToVertex()));
            }
            return;
        }
        LineString shape = getLineStringForShapeId(shapeId);
        if (shape == null) {
            // this trip has a shape_id, but no such shape exists, and no shape_dist in stop_times
            // create straight line segments between stops for each hop
            for (int i = 0; i < stopTimes.size() - 1; ++i) {
                st0 = stopTimes.get(i);
                StopTime st1 = stopTimes.get(i + 1);
                LineString geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
                hops.get(i).setGeometry(geometry);
            }
            return;
        }
        // This trip does not have shape_dist in stop_times, but does have an associated shape.
        ArrayList<IndexedLineSegment> segments = new ArrayList<IndexedLineSegment>();
        for (int i = 0 ; i < shape.getNumPoints() - 1; ++i) {
            segments.add(new IndexedLineSegment(i, shape.getCoordinateN(i), shape.getCoordinateN(i + 1)));
        }
        // Find possible segment matches for each stop.
        List<List<IndexedLineSegment>> possibleSegmentsForStop = new ArrayList<List<IndexedLineSegment>>();
        int minSegmentIndex = 0;
        for (int i = 0; i < stopTimes.size() ; ++i) {
            Stop stop = stopTimes.get(i).getStop();
            Coordinate coord = new Coordinate(stop.getLon(), stop.getLat());
            List<IndexedLineSegment> stopSegments = new ArrayList<IndexedLineSegment>();
            double bestDistance = Double.MAX_VALUE;
            IndexedLineSegment bestSegment = null;
            int maxSegmentIndex = -1;
            int index = -1;
            int minSegmentIndexForThisStop = -1;
            for (IndexedLineSegment segment : segments) {
                index ++;
                if (segment.index < minSegmentIndex) {
                    continue;
                }
                double distance = segment.distance(coord);
                if (distance < maxStopToShapeSnapDistance) {
                    stopSegments.add(segment);
                    maxSegmentIndex = index;
                    if (minSegmentIndexForThisStop == -1)
                        minSegmentIndexForThisStop = index;
                } else if (distance < bestDistance) {
                    bestDistance = distance;
                    bestSegment = segment;
                    if (maxSegmentIndex != -1) {
                        maxSegmentIndex = index;
                    }
                }
            }
            if (stopSegments.size() == 0) {
                //no segments within 150m
                //fall back to nearest segment
                stopSegments.add(bestSegment);
                minSegmentIndex = bestSegment.index;
            } else {
                minSegmentIndex = minSegmentIndexForThisStop;
                Collections.sort(stopSegments, new IndexedLineSegmentComparator(coord));
            }

            for (int j = i - 1; j >= 0; j --) {
                for (Iterator<IndexedLineSegment> it = possibleSegmentsForStop.get(j).iterator(); it.hasNext(); ) {
                    IndexedLineSegment segment = it.next();
                    if (segment.index > maxSegmentIndex) {
                        it.remove();
                    }
                }
            }
            possibleSegmentsForStop.add(stopSegments);
        }

        List<LinearLocation> locations = getStopLocations(possibleSegmentsForStop, stopTimes, 0, -1);

        if (locations == null) {
            // this only happens on shape which have points very far from
            // their stop sequence. So we'll fall back to trivial stop-to-stop
            // linking, even though theoretically we could do better.

            for (int i = 0; i < stopTimes.size() - 1; ++i) {
                st0 = stopTimes.get(i);
                StopTime st1 = stopTimes.get(i + 1);
                LineString geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
                hops.get(i).setGeometry(geometry);
                //this warning is not strictly correct, but will do
                _log.warn(graph.addBuilderAnnotation(new BogusShapeGeometryCaught(shapeId, st0, st1)));
            }
            return;
        }

        Iterator<LinearLocation> locationIt = locations.iterator();
        LinearLocation endLocation = locationIt.next();
        double distanceSoFar = 0;
        int last = 0;
        for (int i = 0; i < hops.size(); ++i) {
            Edge hop = hops.get(i);
            LinearLocation startLocation = endLocation;
            endLocation = locationIt.next();

            //convert from LinearLocation to distance
            //advance distanceSoFar up to start of segment containing startLocation;
            //it does not matter at all if this is accurate so long as it is consistent
            for (int j = last; j < startLocation.getSegmentIndex(); ++j) {
                Coordinate from = shape.getCoordinateN(j);
                Coordinate to = shape.getCoordinateN(j + 1);
                double xd = from.x - to.x;
                double yd = from.y - to.y;
                distanceSoFar += FastMath.sqrt(xd * xd + yd * yd);
            }
            last = startLocation.getSegmentIndex();

            double startIndex = distanceSoFar + startLocation.getSegmentFraction() * startLocation.getSegmentLength(shape);
            //advance distanceSoFar up to start of segment containing endLocation
            for (int j = last; j < endLocation.getSegmentIndex(); ++j) {
                Coordinate from = shape.getCoordinateN(j);
                Coordinate to = shape.getCoordinateN(j + 1);
                double xd = from.x - to.x;
                double yd = from.y - to.y;
                distanceSoFar += FastMath.sqrt(xd * xd + yd * yd);
            }
            last = startLocation.getSegmentIndex();
            double endIndex = distanceSoFar + endLocation.getSegmentFraction() * endLocation.getSegmentLength(shape);

            ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startIndex, endIndex);
            LineString geometry = _geometriesByShapeSegmentKey.get(key);

            if (geometry == null) {
                LocationIndexedLine locationIndexed = new LocationIndexedLine(shape);
                geometry = (LineString) locationIndexed.extractLine(startLocation, endLocation);

                // Pack the resulting line string
                CoordinateSequence sequence = new PackedCoordinateSequence.Double(geometry
                        .getCoordinates(), 2);
                geometry = _geometryFactory.createLineString(sequence);
            }
            ((HopEdge)hop).setGeometry(geometry);
        }
    }

    /**
     * Find a consistent, increasing list of LinearLocations along a shape for a set of stops.
     * Handles loops routes.
     * @return
     */
    private List<LinearLocation> getStopLocations(List<List<IndexedLineSegment>> possibleSegmentsForStop,
            List<StopTime> stopTimes, int index, int prevSegmentIndex) {

        if (index == stopTimes.size()) {
            return new LinkedList<LinearLocation>();
        }

        StopTime st = stopTimes.get(index);
        Stop stop = st.getStop();
        Coordinate stopCoord = new Coordinate(stop.getLon(), stop.getLat());

        for (IndexedLineSegment segment : possibleSegmentsForStop.get(index)) {
            if (segment.index < prevSegmentIndex) {
                //can't go backwards along line
                continue;
            }
            List<LinearLocation> locations = getStopLocations(possibleSegmentsForStop, stopTimes, index + 1, segment.index);
            if (locations != null) {
                LinearLocation location = new LinearLocation(0, segment.index, segment.fraction(stopCoord));
                locations.add(0, location);
                return locations; //we found one!
            }
        }

        return null;
    }

    private T2<FrequencyBasedTripPattern, List<FrequencyHop>> makeFrequencyPattern(Graph graph, Trip trip,
            List<StopTime> stopTimes) {
        
        FrequencyBasedTripPattern pattern = new FrequencyBasedTripPattern(trip, stopTimes.size(), getServiceId(trip));
        TraverseMode mode = GtfsLibrary.getTraverseMode(trip.getRoute());
        int lastStop = stopTimes.size() - 1;

        int i;
        StopTime st1 = null;
        TransitVertex psv0arrive, psv1arrive = null;
        TransitVertex psv0depart;
        ArrayList<Edge> createdEdges = new ArrayList<Edge>();
        ArrayList<Vertex> createdVertices = new ArrayList<Vertex>();
        List<Stop> stops = new ArrayList<Stop>();
        int offset = stopTimes.get(0).getDepartureTime();
        ArrayList<FrequencyHop> hops = new ArrayList<FrequencyHop>();
        for (i = 0; i < lastStop; i++) {    
            StopTime st0 = stopTimes.get(i);
            Stop s0 = st0.getStop();
            stops.add(s0);
            st1 = stopTimes.get(i + 1);
            Stop s1 = st1.getStop();
            if (i == lastStop - 1)
                stops.add(s1);
            
            int arrivalTime = st1.getArrivalTime() - offset;
            int departureTime = st0.getDepartureTime() - offset;

            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
            int runningTime = arrivalTime - departureTime;

            if (runningTime < 0) {
                _log.warn(graph.addBuilderAnnotation(new NegativeHopTime(st0, st1)));
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
            hops.add(hop);

            String headsign = st0.getStopHeadsign();
            if (headsign == null) {
                headsign = trip.getTripHeadsign();
            }
            pattern.addHop(i, departureTime, runningTime, arrivalTime, dwellTime,
                    headsign);

            TransitStopDepart stopDepart = context.stopDepartNodes.get(s0);
            TransitStopArrive stopArrive = context.stopArriveNodes.get(s1);

            final int serviceId = getServiceId(trip);
            Edge board = new FrequencyBoard(stopDepart, psv0depart, pattern, i, mode, serviceId);
            Edge alight = new FrequencyAlight(psv1arrive, stopArrive, pattern, i, mode, serviceId);
            createdEdges.add(board);
            createdEdges.add(alight);
        }

        pattern.setStops(stops);

        int wheelchair = 0;
        if (trip.getWheelchairAccessible() == 1) {
            wheelchair = TableTripPattern.FLAG_WHEELCHAIR_ACCESSIBLE;
        }

        int bikes = 0;
        if ((trip.getRoute().getBikesAllowed() == 2 && trip.getTripBikesAllowed() != 1)
                || trip.getTripBikesAllowed() == 2) {
            bikes = TableTripPattern.FLAG_BIKES_ALLOWED;
        }

        pattern.setTripFlags(wheelchair | bikes);

        return new T2<FrequencyBasedTripPattern, List<FrequencyHop>>(pattern, hops);
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
        boolean midnightCrossed = false;
        final int HOUR = 60 * 60;

        for (int i = 1; i < stopTimes.size(); i++) {
            boolean st1bogus = false;
            StopTime st1 = stopTimes.get(i);

            if (midnightCrossed) {
                if (st1.isDepartureTimeSet())
                    st1.setDepartureTime(st1.getDepartureTime() + 24 * HOUR);
                if (st1.isArrivalTimeSet())
                    st1.setArrivalTime(st1.getArrivalTime() + 24 * HOUR);
            }
            if (!st1.isDepartureTimeSet() && st1.isArrivalTimeSet()) {
                /* set departure time if it is missing */
                st1.setDepartureTime(st1.getArrivalTime());
            }
            /* do not process non-timepoint stoptimes, 
             * which are of course identical to other adjacent non-timepoint stoptimes */
            if ( ! (st1.isArrivalTimeSet() && st1.isDepartureTimeSet())) {
                continue;
            }
            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime(); 
            if (dwellTime < 0) {
                _log.warn(graph.addBuilderAnnotation(new NegativeDwellTime(st0)));
                if (st0.getArrivalTime() > 23 * HOUR && st0.getDepartureTime() < 1 * HOUR) {
                    midnightCrossed = true;
                    st0.setDepartureTime(st0.getDepartureTime() + 24 * HOUR);
                } else {
                    st0.setDepartureTime(st0.getArrivalTime());
                }
            }
            int runningTime = st1.getArrivalTime() - st0.getDepartureTime();

            if (runningTime < 0) {
                _log.warn(graph.addBuilderAnnotation(new NegativeHopTime(new StopTime(st0), new StopTime(st1))));
                // negative hops are usually caused by incorrect coding of midnight crossings
                midnightCrossed = true;
                if (st0.getDepartureTime() > 23 * HOUR && st1.getArrivalTime() < 1 * HOUR) {
                    st1.setArrivalTime(st1.getArrivalTime() + 24 * HOUR);
                } else {
                    st1.setArrivalTime(st0.getDepartureTime());
                }
            }
            double hopDistance = distanceLibrary.fastDistance(
                   st0.getStop().getLat(), st0.getStop().getLon(),
                   st1.getStop().getLat(), st1.getStop().getLon());
            double hopSpeed = hopDistance/runningTime;
            /* zero-distance hops are probably not harmful, though they could be better 
             * represented as dwell times
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
                _log.trace(graph.addBuilderAnnotation(new HopZeroTime((float) hopDistance, 
                          st1.getTrip(), st1.getStopSequence())));
                // clear stoptimes that are obviously wrong, causing them to later be interpolated
/* FIXME (lines commented out because they break routability in multi-feed NYC for some reason -AMB) */
//                st1.clearArrivalTime();
//                st1.clearDepartureTime();
                st1bogus = true;
            } else if (hopSpeed > 45) {
                // 45 m/sec ~= 100 miles/hr
                // elapsed time of 0 will give speed of +inf
                _log.trace(graph.addBuilderAnnotation(new HopSpeedFast((float) hopSpeed, 
                        (float) hopDistance, st0.getTrip(), st0.getStopSequence())));
            } else if (hopSpeed < 0.1) {
                // 0.1 m/sec ~= 0.2 miles/hr
                _log.trace(graph.addBuilderAnnotation(new HopSpeedSlow((float) hopSpeed, 
                        (float) hopDistance, st0.getTrip(), st0.getStopSequence())));
            }
            // st0 should reflect the last stoptime that was not clearly incorrect
            if ( ! st1bogus)  
                st0 = st1;
        } // END for loop over stop times
    }
    
    private void loadAgencies(Graph graph) {
        for (Agency agency : _dao.getAllAgencies()) {
            graph.addAgency(agency);
        }
    }

    private PatternInterlineDwell getInterlineDwell(InterlineSwitchoverKey key) {
        return interlineDwells.get(key);
    }

    private void loadPathways(Graph graph) {
        for (Pathway pathway : _dao.getAllPathways()) {
            Vertex fromVertex = context.stopNodes.get(pathway.getFromStop());
            Vertex toVertex = context.stopNodes.get(pathway.getToStop());
            if (pathway.isWheelchairTraversalTimeSet()) {
                new PathwayEdge(fromVertex, toVertex, pathway.getTraversalTime(), pathway.getWheelchairTraversalTime());
            } else {
                new PathwayEdge(fromVertex, toVertex, pathway.getTraversalTime());
            }
        }
    }

    private void loadStops(Graph graph) {
        for (Stop stop : _dao.getAllStops()) {
            if (context.stops.contains(stop.getId())) {
                continue;
            }
            context.stops.add(stop.getId());
            //add a vertex representing the stop
            TransitStop stopVertex = new TransitStop(graph, stop);
            stopVertex.setStreetToStopTime(defaultStreetToStopTime);
            context.stopNodes.put(stop, stopVertex);
            
            if (stop.getLocationType() != 2) {
                //add a vertex representing arriving at the stop
                TransitStopArrive arrive = new TransitStopArrive(graph, stop, stopVertex);
                context.stopArriveNodes.put(stop, arrive);

                //add a vertex representing departing from the stop
                TransitStopDepart depart = new TransitStopDepart(graph, stop, stopVertex);
                context.stopDepartNodes.put(stop, depart);

                //add edges from arrive to stop and stop to depart
                new PreAlightEdge(arrive, stopVertex);
                new PreBoardEdge(stopVertex, depart);
            }
        }
    }
    
    /** Delete dwell edges that take no time, and merge their start and end vertices. */
    private void deleteUselessDwells(Graph graph) {
        int nDwells = potentiallyUselessDwells.size();
        int nDeleted = 0;
        for (PatternDwell dwell : potentiallyUselessDwells) {
            // useless arrival time arrays are now eliminated in TripTimes constructor (AMB) 
            if (dwell.allDwellsZero()) {
                dwell.getFromVertex().mergeFrom(graph, dwell.getToVertex());
                nDeleted += 1;
            }                
        }
        _log.debug("deleted {} dwell edges / {} candidates, merging arrival and departure vertices.", 
           nDeleted, nDwells);
    }

    private void addTripToInterliningMap(Trip trip, List<StopTime> stopTimes, TableTripPattern tripPattern) {
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

    /** 
     * The first time a particular ScheduledStopPattern (stops+pickups+serviceId combination) 
     * is encountered, an empty tripPattern object is created to hold the schedule information. This
     * method also creates the corresponding PatternStop vertices and PatternBoard/Hop/Alight edges.
     * StopTimes are passed in instead of Stops only because they are needed for shape distances.
     * The TripPattern returned is empty; trips should be added to the TripPattern later.
     */
    private T2<TableTripPattern, List<PatternHop>> makePatternVerticesAndEdges(Graph graph, Trip trip,
            ScheduledStopPattern stopPattern, List<StopTime> stopTimes) {

        TableTripPattern tripPattern = new TableTripPattern(trip, stopPattern, getServiceId(trip));
        // These indexes may be used to make an array-based TimetableResolver if the current 
        // hashmap-based implementation turns out to be insufficient.
        // Otherwise, they can be replaced with a simple list of tripPatterns, so that their 
        // scheduled timetables can be indexed and compacted once all trips are added.
        getTripPatternIndex(tripPattern);
        TraverseMode mode = GtfsLibrary.getTraverseMode(trip.getRoute());
        
        ArrayList<PatternHop> hops = new ArrayList<PatternHop>();

        // create journey vertices
        PatternArriveVertex psv0arrive, psv1arrive = null;
        PatternDepartVertex psv0depart;
        for (int hopIndex = 0; hopIndex < stopTimes.size() - 1; hopIndex++) {
            StopTime st0 = stopTimes.get(hopIndex);
            Stop s0 = st0.getStop();
            StopTime st1 = stopTimes.get(hopIndex + 1);
            Stop s1 = st1.getStop();
            psv0depart = new PatternDepartVertex(graph, tripPattern, st0);
            context.patternDepartNodes.put(new T2<Stop, Trip>(s0, trip), psv0depart);
            if (hopIndex != 0) {
                psv0arrive = psv1arrive;
                PatternDwell dwell = new PatternDwell(psv0arrive, psv0depart, hopIndex, tripPattern);
                if (st0.getArrivalTime() == st0.getDepartureTime())
                    potentiallyUselessDwells.add(dwell); // TODO: verify against old code
            }
            psv1arrive = new PatternArriveVertex(graph, tripPattern, st1);
            context.patternArriveNodes.put(new T2<Stop, Trip>(st1.getStop(), trip), psv1arrive);

            PatternHop hop = new PatternHop(psv0depart, psv1arrive, s0, s1, hopIndex);
            hops.add(hop);

            TransitStopDepart stopDepart = context.stopDepartNodes.get(s0);
            if (stopDepart == null) {
                s0 = _dao.getStopForId(new AgencyAndId(s0.getId().getAgencyId(), s0.getParentStation()));
                stopDepart = context.stopDepartNodes.get(s0);
                if (stopDepart == null) {
                    _log.warn(graph.addBuilderAnnotation(new StopAtEntrance(st0, false)));
                    continue;
                } else {
                    _log.warn(graph.addBuilderAnnotation(new StopAtEntrance(st0, true)));
                }
            }
            TransitStopArrive stopArrive = context.stopArriveNodes.get(s1);
            if (stopArrive == null) {
                s1 = _dao.getStopForId(new AgencyAndId(s1.getId().getAgencyId(), s1.getParentStation()));
                stopArrive = context.stopArriveNodes.get(s1);
                if (stopArrive == null) {
                    _log.warn(graph.addBuilderAnnotation(new StopAtEntrance(st1, false)));
                    continue;
                } else {
                    _log.warn(graph.addBuilderAnnotation(new StopAtEntrance(st1, true)));
                }
            }
            stopArrive.getStopVertex().addMode(mode);
            new TransitBoardAlight(stopDepart, psv0depart, hopIndex, mode);
            new TransitBoardAlight(psv1arrive, stopArrive, hopIndex, mode);
        }        
        
        return new T2<TableTripPattern, List<PatternHop>>(tripPattern, hops);
    }
    
    
    // originally in makeTripPattern method (duplicate trip adding code and flags code):
//        tripPattern.setTripFlags(0, 
//                ((trip.getWheelchairAccessible() == 1) ? TripPattern.FLAG_WHEELCHAIR_ACCESSIBLE : 0)
//                
//                | 
//                
//                (((trip.getRoute().getBikesAllowed() == 2 && trip.getTripBikesAllowed() != 1)
//                    || trip.getTripBikesAllowed() == 2) ? TripPattern.FLAG_BIKES_ALLOWED : 0));
//
//        tripPattern.addHop(i, 0, departureTime, runningTime, arrivalTime, dwellTime,
//                st0.getStopHeadsign(), trip);
//        StopTime st1 = null;
//        for (int i = 0; i < lastStop; i++) {           
//            StopTime st0 = stopTimes.get(i);
//            Stop s0 = st0.getStop();
//            st1 = stopTimes.get(i + 1);
//            Stop s1 = st1.getStop();
//
//            int arrivalTime = st1.getArrivalTime();
//            int departureTime = st0.getDepartureTime();
//
//            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
//            int runningTime = arrivalTime - departureTime ;
//
//            if (runningTime < 0) {
//                _log.warn(GraphBuilderAnnotation.register(graph,
//                        Variety.NEGATIVE_HOP_TIME, st0, st1));
//                //back out hops and give up
//                for (Edge e: createdEdges) {
//                    e.getFromVertex().removeOutgoing(e);
//                    e.getToVertex().removeIncoming(e);
//                }
//                for (Vertex v : createdVertices) {
//                    graph.removeVertexAndEdges(v);
//                }
//                return null;
//            }
//
//        }
//
//    }

    private int getServiceId(Trip trip) {
        AgencyAndId gtfsId = trip.getServiceId();
        Integer id = context.serviceIds.get(gtfsId);
        if (id == null) {
            id = context.serviceIds.size();
            context.serviceIds.put(gtfsId, id);
        }
        return id;
    }

    private int getTripPatternIndex(TableTripPattern pattern) {
        // we could probably get away with just a set of tripPatterns since we are not storing
        // indexes in the patterns themselves.
        Integer id = context.tripPatternIds.get(pattern);
        if (id == null) {
            id = context.serviceIds.size();
            context.tripPatternIds.put(pattern, id);
        }
        return id;
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
            Vertex fromVertex = context.stopArriveNodes.get(fromStop);
            Vertex toVertex = context.stopDepartNodes.get(toStop);
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

    
    private LineString getHopGeometryViaShapeDistTraveled(Graph graph, AgencyAndId shapeId, StopTime st0, StopTime st1,
            Vertex startJourney, Vertex endJourney) {

        double startDistance = st0.getShapeDistTraveled();
        double endDistance = st1.getShapeDistTraveled();

        ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);
        LineString geometry = _geometriesByShapeSegmentKey.get(key);
        if (geometry != null)
            return geometry;

        double[] distances = getDistanceForShapeId(shapeId);

        if (distances == null) {
            _log.warn(graph.addBuilderAnnotation(new BogusShapeGeometry(shapeId)));
            return null;
        } else {
            LinearLocation startIndex = getSegmentFraction(distances, startDistance);
            LinearLocation endIndex = getSegmentFraction(distances, endDistance);

            if (equals(startIndex, endIndex)) {
                //bogus shape_dist_traveled 
                graph.addBuilderAnnotation(new BogusShapeDistanceTraveled(st1));
                return createSimpleGeometry(st0.getStop(), st1.getStop());
            }
            LineString line = getLineStringForShapeId(shapeId);
            LocationIndexedLine lol = new LocationIndexedLine(line);

            geometry = getSegmentGeometry(graph, shapeId, lol, startIndex, endIndex, startDistance,
                    endDistance, st0, st1);

            return geometry;
        }
    }

    private static boolean equals(LinearLocation startIndex, LinearLocation endIndex) {
        return startIndex.getSegmentIndex() == endIndex.getSegmentIndex()
                && startIndex.getSegmentFraction() == endIndex.getSegmentFraction()
                && startIndex.getComponentIndex() == endIndex.getComponentIndex();
    }

    /** create a 2-point linestring (a straight line segment) between the two stops */
    private LineString createSimpleGeometry(Stop s0, Stop s1) {
        
        Coordinate[] coordinates = new Coordinate[] {
                new Coordinate(s0.getLon(), s0.getLat()),
                new Coordinate(s1.getLon(), s1.getLat())
        };
        CoordinateSequence sequence = new PackedCoordinateSequence.Double(coordinates, 2);
        
        return _geometryFactory.createLineString(sequence);        
    }

    private boolean isValid(Geometry geometry, Stop s0, Stop s1) {
        Coordinate[] coordinates = geometry.getCoordinates();
        if (coordinates.length < 2) {
            return false;
        }
        if (geometry.getLength() == 0) {
            return false;
        }
        for (Coordinate coordinate : coordinates) {
            if (Double.isNaN(coordinate.x) || Double.isNaN(coordinate.y)) {
                return false;
            }
        }
        Coordinate geometryStartCoord = coordinates[0];
        Coordinate geometryEndCoord = coordinates[coordinates.length - 1];
        
        Coordinate startCoord = new Coordinate(s0.getLon(), s0.getLat());
        Coordinate endCoord = new Coordinate(s1.getLon(), s1.getLat());
        if (distanceLibrary.fastDistance(startCoord, geometryStartCoord) > maxStopToShapeSnapDistance) {
            return false;
        } else if (distanceLibrary.fastDistance(endCoord, geometryEndCoord) > maxStopToShapeSnapDistance) {
            return false;
        }
        return true;
    }

    private LineString getSegmentGeometry(Graph graph, AgencyAndId shapeId,
            LocationIndexedLine locationIndexedLine, LinearLocation startIndex,
            LinearLocation endIndex, double startDistance, double endDistance, 
            StopTime st0, StopTime st1) {

        ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);

        LineString geometry = _geometriesByShapeSegmentKey.get(key);
        if (geometry == null) {

            geometry = (LineString) locationIndexedLine.extractLine(startIndex, endIndex);

            // Pack the resulting line string
            CoordinateSequence sequence = new PackedCoordinateSequence.Double(geometry
                    .getCoordinates(), 2);
            geometry = _geometryFactory.createLineString(sequence);
            
            if (!isValid(geometry, st0.getStop(), st1.getStop())) {
                _log.warn(graph.addBuilderAnnotation(new BogusShapeGeometryCaught(shapeId, st0, st1)));
                //fall back to trivial geometry
                geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
            }
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

        CoordinateSequence sequence = new PackedCoordinateSequence.Double(coordinates, 2);
        geometry = _geometryFactory.createLineString(sequence);
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

    /** Filter out (erroneous) series of stop times that refer to the same stop */
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
     * 1. Create edges between stops and their parent stations.
     * 2. Create transfer edges between stops which are listed in transfers.txt.
     * 
     * This is not usually useful, but it's nice for the NYC subway system, where
     * it's important to provide in-station transfers for fare computation.
     * 
     * NOTE: this method is only called when transfersTxtDefinesStationPaths is set to
     * True for a given GFTS feed. 
     */
    public void createStationTransfers(Graph graph) {

        /*  1. Connect stops to their parent stations. */
        for (Stop stop : _dao.getAllStops()) {
            String parentStation = stop.getParentStation();
            if (parentStation != null) {
                Vertex stopVertex = context.stopNodes.get(stop);

                String agencyId = stop.getId().getAgencyId();
                AgencyAndId parentStationId = new AgencyAndId(agencyId, parentStation);

                Stop parentStop = _dao.getStopForId(parentStationId);
                Vertex parentStopVertex = context.stopNodes.get(parentStop);

                new FreeEdge(parentStopVertex, stopVertex);
                new FreeEdge(stopVertex, parentStopVertex);

                // Stops with location_type=2 (entrances as defined in the pathways.txt 
                // proposal) have no arrive/depart vertices, hence the null checks.
                Vertex stopArriveVertex = context.stopArriveNodes.get(stop);
                Vertex parentStopArriveVertex = context.stopArriveNodes.get(parentStop);
                if (stopArriveVertex != null && parentStopArriveVertex != null) {
                    new FreeEdge(parentStopArriveVertex, stopArriveVertex);
                    new FreeEdge(stopArriveVertex, parentStopArriveVertex);
                }

                Vertex stopDepartVertex = context.stopDepartNodes.get(stop);
                Vertex parentStopDepartVertex = context.stopDepartNodes.get(parentStop);
                if (stopDepartVertex != null && parentStopDepartVertex != null) {
                    new FreeEdge(parentStopDepartVertex, stopDepartVertex);
                    new FreeEdge(stopDepartVertex, parentStopDepartVertex);
                }

                // TODO: provide a cost for these edges when stations and
                // stops have different locations 
            }
        }
        /* 2. Create transfer edges based on transfers.txt. */
        for (Transfer transfer : _dao.getAllTransfers()) {

            int type = transfer.getTransferType();
            if (type == 3) // type 3 = transfer not possible
                continue;
            if (transfer.getFromStop().equals(transfer.getToStop())) {
                continue;
            }
            Vertex fromv = context.stopArriveNodes.get(transfer.getFromStop());
            Vertex tov = context.stopDepartNodes.get(transfer.getToStop());

            double distance = distanceLibrary.distance(fromv.getCoordinate(), tov.getCoordinate());
            int time;
            if (transfer.getTransferType() == 2) {
                time = transfer.getMinTransferTime();
            } else {
                time = (int) distance; // fixme: handle timed transfers
            }

            TransferEdge transferEdge = new TransferEdge(fromv, tov, distance, time);
            CoordinateSequence sequence = new PackedCoordinateSequence.Double(new Coordinate[] {
                    fromv.getCoordinate(), tov.getCoordinate() }, 2);
            LineString geometry = _geometryFactory.createLineString(sequence);
            transferEdge.setGeometry(geometry);
        }
    }

    public int getDefaultStreetToStopTime() {
        return defaultStreetToStopTime;
    }

    public void setDefaultStreetToStopTime(int defaultStreetToStopTime) {
        this.defaultStreetToStopTime = defaultStreetToStopTime;
    }

    /**
     * You might not want to delete dwell edges when using realtime updates, because new dwells 
     * might be introduced via trip updates.
     */
    public void setDeleteUselessDwells(boolean delete) {
        this._deleteUselessDwells = delete;
    }

    public void setStopContext(GtfsStopContext context) {
        this.context = context;
    }


    public double getMaxStopToShapeSnapDistance() {
        return maxStopToShapeSnapDistance;
    }


    public void setMaxStopToShapeSnapDistance(double maxStopToShapeSnapDistance) {
        this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
    }

}
