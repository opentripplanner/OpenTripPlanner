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
import org.onebusaway.gtfs.model.Route;
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
import org.opentripplanner.gbannotation.BogusShapeDistanceTraveled;
import org.opentripplanner.gbannotation.BogusShapeGeometry;
import org.opentripplanner.gbannotation.BogusShapeGeometryCaught;
import org.opentripplanner.gbannotation.NonStationParentStation;
import org.opentripplanner.gbannotation.HopSpeedFast;
import org.opentripplanner.gbannotation.HopSpeedSlow;
import org.opentripplanner.gbannotation.HopZeroTime;
import org.opentripplanner.gbannotation.NegativeDwellTime;
import org.opentripplanner.gbannotation.NegativeHopTime;
import org.opentripplanner.gbannotation.RepeatedStops;
import org.opentripplanner.gbannotation.TripDegenerate;
import org.opentripplanner.gbannotation.TripUndefinedService;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.HopEdge;
import org.opentripplanner.routing.edgetype.StationStopEdge;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.impl.OnBoardDepartServiceImpl;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.opentripplanner.routing.services.OnBoardDepartService;
import org.opentripplanner.routing.trippattern.FrequencyTripTimes;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStation;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Maps;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

// Filtering out (removing) stoptimes from a trip forces us to either have two copies of that list,
// or do all the steps within one loop over trips. It would be clearer if there were multiple loops over the trips.

/** A wrapper class for Trips that allows them to be sorted. */
class InterliningTrip  implements Comparable<InterliningTrip> {
    public Trip trip;
    public StopTime firstStopTime;
    public StopTime lastStopTime;
    TripPattern tripPattern;

    InterliningTrip(Trip trip, List<StopTime> stopTimes, TripPattern tripPattern) {
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

/** 
 * This compound key object is used when grouping interlining trips together by (serviceId, blockId). 
 */
class BlockIdAndServiceId {
    public String blockId;
    public AgencyAndId serviceId;

    BlockIdAndServiceId(Trip trip) {
        this.blockId = trip.getBlockId();
        this.serviceId = trip.getServiceId();
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
    public TripPattern pattern1, pattern2;

    public InterlineSwitchoverKey(Stop s0, Stop s1, 
        TripPattern pattern1, TripPattern pattern2) {
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

/* TODO Move this stuff into the geometry library */
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

    private static final Logger LOG = LoggerFactory.getLogger(GTFSPatternHopFactory.class);

    private static final int SECONDS_IN_HOUR = 60 * 60; // rename to seconds in hour

    private static GeometryFactory _geometryFactory = GeometryUtils.getGeometryFactory();

    private GtfsRelationalDao _dao;

    private CalendarService _calendarService;
    
    private Map<ShapeSegmentKey, LineString> _geometriesByShapeSegmentKey = new HashMap<ShapeSegmentKey, LineString>();

    private Map<AgencyAndId, LineString> _geometriesByShapeId = new HashMap<AgencyAndId, LineString>();

    private Map<AgencyAndId, double[]> _distancesByShapeId = new HashMap<AgencyAndId, double[]>();
    
    private boolean _deleteUselessDwells = true;

    private ArrayList<PatternDwell> potentiallyUselessDwells = new ArrayList<PatternDwell> ();

    private FareServiceFactory fareServiceFactory;

    /* Need TripTimes rather than Trips, to allow sorting within blocks and linking up patterns. */
    private ListMultimap<BlockIdAndServiceId, TripTimes> tripTimesForBlock = ArrayListMultimap.create();

//    private Map<InterlineSwitchoverKey, PatternInterlineDwell> interlineDwells = new HashMap<InterlineSwitchoverKey, PatternInterlineDwell>();

    private Map<StopPattern, TripPattern> tripPatterns = Maps.newHashMap();

//    private Map<StopPattern, TripPattern> frequencyTripPatterns = Maps.newHashMap(); NOT USED, all patterns are mixed for the moment

    private GtfsStopContext context = new GtfsStopContext();

    private int defaultStreetToStopTime;

    private static final DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private double maxStopToShapeSnapDistance = 150;

    public GTFSPatternHopFactory(GtfsContext context) {
        this._dao = context.getDao();
        this._calendarService = context.getCalendarService();
    }
    
    public GTFSPatternHopFactory() {
        this._dao = null;
        this._calendarService = null;
    }

    /** Generate the edges. */
    public void run(Graph graph) {
        if (fareServiceFactory == null) {
            fareServiceFactory = new DefaultFareServiceFactory();
        }
        fareServiceFactory.setDao(_dao);
        
        // TODO: Why are we loading stops? The Javadoc above says this method assumes stops are aleady loaded.
        loadStops(graph);
        loadPathways(graph);
        loadAgencies(graph);
        // TODO: Why is there cached "data", and why are we clearing it? Due to a general lack of comments, I have no idea.
        // Perhaps it is to allow name collisions with previously loaded feeds.
        clearCachedData(); 

        /* Assign 0-based numeric codes to all GTFS service IDs. */
        for (AgencyAndId serviceId : _dao.getAllServiceIds()) {
            // TODO: FIX Service code collision for multiple feeds.
            graph.serviceCodes.put(serviceId, graph.serviceCodes.size());
        }
        
        LOG.debug("building hops from trips");
        Collection<Trip> trips = _dao.getAllTrips();
        int tripCount = 0;

        /* First, record which trips are used by one or more frequency entries.
         * These trips will be ignored for the purposes of non-frequency routing, and
         * all the frequency entries referencing the same trip can be added at once to the same
         * timetable.
         */
        ListMultimap<Trip, Frequency> frequenciesForTrip = ArrayListMultimap.create();        
        for(Frequency freq : _dao.getAllFrequencies()) {
            frequenciesForTrip.put(freq.getTrip(), freq);
        }
        
        /* Then loop over all trips, handling each one as a frequency-based or scheduled trip. */
        TRIP : for (Trip trip : trips) {

            if (++tripCount % 100000 == 0) {
                LOG.debug("loading trips {}/{}", tripCount, trips.size());
            }

            // TODO: move to a validator module
            if ( ! _calendarService.getServiceIds().contains(trip.getServiceId())) {
                LOG.warn(graph.addBuilderAnnotation(new TripUndefinedService(trip)));
            }

            /* Fetch the stop times for this trip. Copy the list since it's immutable. */
            List<StopTime> stopTimes = new ArrayList<StopTime>(_dao.getStopTimesForTrip(trip));

            /* GTFS stop times frequently contain duplicate, missing, or incorrect entries. Repair them. */
            if (removeRepeatedStops(stopTimes)) {
                LOG.warn(graph.addBuilderAnnotation(new RepeatedStops(trip)));
            }
            filterStopTimes(stopTimes, graph);
            interpolateStopTimes(stopTimes);   
            
            /* If after filtering this trip does not contain at least 2 stoptimes, it does not serve any purpose. */
            if (stopTimes.size() < 2) {
                LOG.warn(graph.addBuilderAnnotation(new TripDegenerate(trip)));
                continue TRIP;
            }

            /* Get the existing TripPattern for this filtered StopPattern, or create one. */
            StopPattern stopPattern = new StopPattern(stopTimes);
            TripPattern tripPattern = tripPatterns.get(stopPattern);
            if (tripPattern == null) {
                tripPattern = new TripPattern(trip.getRoute(), stopPattern);
                tripPatterns.put(stopPattern, tripPattern);
            }

            /* Check whether this trip is referenced by one or more frequency entries. */
            List<Frequency> frequencies = frequenciesForTrip.get(trip);
            if (frequencies != null && !(frequencies.isEmpty())) {
                for (Frequency freq : frequencies) {
                    tripPattern.add(new FrequencyTripTimes(trip, stopTimes, freq));
                }
                // TODO replace: createGeometry(graph, trip, stopTimes, hops);
            }

            /* This trip was not frequency-based, so it must be table-based. */
            else {
                TripTimes tripTimes = new TripTimes(trip, stopTimes);
                tripPattern.add(tripTimes);
                // For interlining, group TripTimes with all others sharing the same block ID.
                // Block semantics seem undefined for frequency trips.
                if (trip.getBlockId() != null && ! trip.getBlockId().equals("")) {
                    tripTimesForBlock.put(new BlockIdAndServiceId(trip), tripTimes);
                }
            }

        } // end foreach TRIP

        /* Generate unique names for all the TableTripPatterns. */
        TripPattern.generateUniqueNames(tripPatterns.values());

        /* Loop over all new TableTripPatterns, creating the vertices and edges for each pattern. */
        for (TripPattern tableTripPattern : tripPatterns.values()) {
            tableTripPattern.makePatternVerticesAndEdges(graph, context);
            tableTripPattern.setServiceCodes(graph.serviceCodes); // TODO this could be more elegant
        }

        /* Link up interlined trips (where a physical vehicle continues on to another logical trip). */
        Map<TripTimes, TripTimes> interlinedTrips = Maps.newHashMap();
        for (BlockIdAndServiceId block : tripTimesForBlock.keySet()) {
            List<TripTimes> blockTripTimes = tripTimesForBlock.get(block);
            /* Sort trips within the block by first departure time, then iterate over trips in this 
             * block and schedule, linking them. Has no effect on single-trip blocks. 
             * Storing TripTimes rather than trip, so we have both Pattern and Trip, 
             * and can perform real time lookup. */
            Collections.sort(blockTripTimes); 
            TripTimes last = null;
            for (TripTimes tripTimes : blockTripTimes) {
                if (last != null) {
                    interlinedTrips.put(last, tripTimes);
                }
                // TODO: Check for incoherence / trip times overlap.
                last = tripTimes;
            }
        }

     // FIXME MAKE PATTERN INTERLINE DWELL edges for patterns
     // do we already have a PatternInterlineDwell edge for this dwell?
//                 PatternInterlineDwell dwell = getInterlineDwell(dwellKey);
//                 if (dwell == null) { 
//                     // create the dwell because it does not exist yet
//                     Vertex startJourney = context.patternArriveNodes.get(new T2<Stop, Trip>(s0, fromExemplar));
//                     Vertex endJourney = context.patternDepartNodes.get(new T2<Stop, Trip>(s1, toExemplar));
//                     // toTrip is just an exemplar; dwell edges can contain many trip connections
//                     dwell = new PatternInterlineDwell(startJourney, endJourney, toTrip);
//                     interlineDwells.put(dwellKey, dwell);
//                 }
//                 int dwellTime = st1.getDepartureTime() - st0.getArrivalTime();
//                 dwell.addTrip(fromTrip, toTrip, dwellTime,
//                         fromInterlineTrip.getPatternIndex(), toInterlineTrip.getPatternIndex());

        loadTransfers(graph);
        if (_deleteUselessDwells) deleteUselessDwells(graph);

        /* Is this the wrong place to do this? It should be done on all feeds at once, or at deserialization. */
        // it is already done at deserialization, but standalone mode allows using graphs without serializing them.
        for (TripPattern tableTripPattern : tripPatterns.values()) {
            tableTripPattern.getScheduledTimetable().finish();
        }
        
        clearCachedData(); // eh?
        graph.putService(FareService.class, fareServiceFactory.makeFareService());
        graph.putService(OnBoardDepartService.class, new OnBoardDepartServiceImpl());
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
                LOG.warn(graph.addBuilderAnnotation(new BogusShapeGeometryCaught(shapeId, st0, st1)));
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


// TODO makeFrequencyPattern(Graph graph, Trip trip, List<StopTime> stopTimes) {


    /**
     * Scan through the given list, looking for clearly incorrect series of stoptimes and unsetting
     * them. This includes duplicate times (0-time hops), as well as negative, fast or slow hops.
     * Unsetting the arrival/departure time of clearly incorrect stoptimes will cause them to be
     * interpolated in the next step. Annotations are also added to the graph to reveal the problems
     * to the user.
     * 
     * @param stopTimes the stoptimes to be filtered (from a single trip)
     * @param graph the graph where annotations will be registered
     */
    private void filterStopTimes(List<StopTime> stopTimes, Graph graph) {
        
        if (stopTimes.size() < 2) return;
        StopTime st0 = stopTimes.get(0);

        /* Set departure time if it is missing */
        if (!st0.isDepartureTimeSet() && st0.isArrivalTimeSet()) {
            st0.setDepartureTime(st0.getArrivalTime());
        }
        
        /* Indicates that stop times in this trip are being shifted forward one day. */
        boolean midnightCrossed = false;
        
        for (int i = 1; i < stopTimes.size(); i++) {
            boolean st1bogus = false;
            StopTime st1 = stopTimes.get(i);

            if (midnightCrossed) {
                if (st1.isDepartureTimeSet())
                    st1.setDepartureTime(st1.getDepartureTime() + 24 * SECONDS_IN_HOUR);
                if (st1.isArrivalTimeSet())
                    st1.setArrivalTime(st1.getArrivalTime() + 24 * SECONDS_IN_HOUR);
            }
            /* Set departure time if it is missing. */
            // TODO: doc: what if arrival time is missing?
            if (!st1.isDepartureTimeSet() && st1.isArrivalTimeSet()) {
                st1.setDepartureTime(st1.getArrivalTime());
            }
            /* Do not process (skip over) non-timepoint stoptimes, leaving them in place for interpolation. */ 
            // All non-timepoint stoptimes in a series will have identical arrival and departure values of MISSING_VALUE.
            if ( ! (st1.isArrivalTimeSet() && st1.isDepartureTimeSet())) {
                continue;
            }
            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime(); 
            if (dwellTime < 0) {
                LOG.warn(graph.addBuilderAnnotation(new NegativeDwellTime(st0)));
                if (st0.getArrivalTime() > 23 * SECONDS_IN_HOUR && st0.getDepartureTime() < 1 * SECONDS_IN_HOUR) {
                    midnightCrossed = true;
                    st0.setDepartureTime(st0.getDepartureTime() + 24 * SECONDS_IN_HOUR);
                } else {
                    st0.setDepartureTime(st0.getArrivalTime());
                }
            }
            int runningTime = st1.getArrivalTime() - st0.getDepartureTime();

            if (runningTime < 0) {
                LOG.warn(graph.addBuilderAnnotation(new NegativeHopTime(new StopTime(st0), new StopTime(st1))));
                // negative hops are usually caused by incorrect coding of midnight crossings
                midnightCrossed = true;
                if (st0.getDepartureTime() > 23 * SECONDS_IN_HOUR && st1.getArrivalTime() < 1 * SECONDS_IN_HOUR) {
                    st1.setArrivalTime(st1.getArrivalTime() + 24 * SECONDS_IN_HOUR);
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
                LOG.warn(GraphBuilderAnnotation.register(graph, 
                        Variety.HOP_ZERO_DISTANCE, runningTime, 
                        st1.getTrip().getId(), 
                        st1.getStopSequence()));
            } 
            */
            // sanity-check the hop
            if (st0.getArrivalTime() == st1.getArrivalTime() ||
                st0.getDepartureTime() == st1.getDepartureTime()) {
                LOG.trace("{} {}", st0, st1);
                // series of identical stop times at different stops
                LOG.trace(graph.addBuilderAnnotation(new HopZeroTime((float) hopDistance, 
                          st1.getTrip(), st1.getStopSequence())));
                // clear stoptimes that are obviously wrong, causing them to later be interpolated
/* FIXME (lines commented out because they break routability in multi-feed NYC for some reason -AMB) */
//                st1.clearArrivalTime();
//                st1.clearDepartureTime();
                st1bogus = true;
            } else if (hopSpeed > 45) {
                // 45 m/sec ~= 100 miles/hr
                // elapsed time of 0 will give speed of +inf
                LOG.trace(graph.addBuilderAnnotation(new HopSpeedFast((float) hopSpeed, 
                        (float) hopDistance, st0.getTrip(), st0.getStopSequence())));
            } else if (hopSpeed < 0.1) {
                // 0.1 m/sec ~= 0.2 miles/hr
                LOG.trace(graph.addBuilderAnnotation(new HopSpeedSlow((float) hopSpeed, 
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

    private void loadPathways(Graph graph) {
        for (Pathway pathway : _dao.getAllPathways()) {
            Vertex fromVertex = context.stationStopNodes.get(pathway.getFromStop());
            Vertex toVertex = context.stationStopNodes.get(pathway.getToStop());
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

            int locationType = stop.getLocationType();

            //add a vertex representing the stop
            if (locationType == 1) {
                context.stationStopNodes.put(stop, new TransitStation(graph, stop));
            } else {
                TransitStop stopVertex = new TransitStop(graph, stop);
                stopVertex.setStreetToStopTime(defaultStreetToStopTime);
                context.stationStopNodes.put(stop, stopVertex);

                if (locationType != 2) {
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
        LOG.debug("deleted {} dwell edges / {} candidates, merging arrival and departure vertices.", 
           nDeleted, nDwells);
        if (nDeleted > 0) graph.setDwellsDeleted(true);
    }

    /**
     * Scan through the given list of stoptimes, interpolating the missing (unset) ones.
     * This is currently done by assuming equidistant stops and constant speed.
     * While we may not be able to improve the constant speed assumption, we can
     * TODO: use route matching (or shape distance etc.) to improve inter-stop distances
     *  
     * @param stopTimes the stoptimes (from a single trip) to be interpolated 
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

    private void clearCachedData() {
        LOG.debug("shapes=" + _geometriesByShapeId.size());
        LOG.debug("segments=" + _geometriesByShapeSegmentKey.size());
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
            Route fromRoute = t.getFromRoute();
            Route toRoute = t.getToRoute();
            Trip fromTrip = t.getFromTrip();
            Trip toTrip = t.getToTrip();
            Vertex fromVertex = context.stopArriveNodes.get(fromStop);
            Vertex toVertex = context.stopDepartNodes.get(toStop);
            switch (t.getTransferType()) {
            case 1:
                // timed (synchronized) transfer 
                // Handle with edges that bypass the street network.
                // from and to vertex here are stop_arrive and stop_depart vertices
                
                // only add edge when it doesn't exist already
                boolean hasTimedTransferEdge = false;
                for (Edge outgoingEdge : fromVertex.getOutgoing()) {
                    if (outgoingEdge instanceof TimedTransferEdge) {
                        if (outgoingEdge.getToVertex() == toVertex) {
                            hasTimedTransferEdge = true;
                            break;
                        }
                    }
                }
                if (!hasTimedTransferEdge) {
                    new TimedTransferEdge(fromVertex, toVertex);
                }
                // add to transfer table to handle specificity
                transferTable.addTransferTime(fromStop, toStop, fromRoute, toRoute, fromTrip, toTrip, StopTransfer.TIMED_TRANSFER);
                break;
            case 2:
                // min transfer time
                transferTable.addTransferTime(fromStop, toStop, fromRoute, toRoute, fromTrip, toTrip, t.getMinTransferTime());
                break;
            case 3:
                // forbidden transfer
                transferTable.addTransferTime(fromStop, toStop, fromRoute, toRoute, fromTrip, toTrip, StopTransfer.FORBIDDEN_TRANSFER);
                break;
            case 0:
            default: 
                // preferred transfer
                transferTable.addTransferTime(fromStop, toStop, fromRoute, toRoute, fromTrip, toTrip, StopTransfer.PREFERRED_TRANSFER);
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
            LOG.warn(graph.addBuilderAnnotation(new BogusShapeGeometry(shapeId)));
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
                LOG.warn(graph.addBuilderAnnotation(new BogusShapeGeometryCaught(shapeId, st0, st1)));
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
                    LOG.trace("pair of identical shape points (skipping): {} {}", last, sp);
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

    /**
     * Filter out any series of stop times that refer to the same stop. This is very inefficient in
     * an array-backed list, but we are assuming that this is a rare occurrence. The alternative is
     * to copy every list of stop times during filtering.
     * 
     * TODO: OBA GFTS makes the stoptime lists unmodifiable, so this will not work.
     * We need to copy any modified list. 
     * 
     * @return whether any repeated stops were filtered out.
     */
    private boolean removeRepeatedStops (List<StopTime> stopTimes) {
        boolean filtered = false;
        StopTime prev = null;
        Iterator<StopTime> it = stopTimes.iterator();
        while (it.hasNext()) {
            StopTime st = it.next();
            if (prev != null) {
                if (prev.getStop().equals(st.getStop())) {
                    // OBA gives us unmodifiable lists, but we have copied them.
                    prev.setDepartureTime(st.getDepartureTime());
                    it.remove();
                    filtered = true;
                }
            }
            prev = st;
        }
        return filtered;
    }

    public void setFareServiceFactory(FareServiceFactory fareServiceFactory) {
        this.fareServiceFactory = fareServiceFactory;
    }

    /** 
     * Create bidirectional, "free" edges (zero-time, low-cost edges) between stops and their 
     * parent stations. This is used to produce implicit transfers between all stops that are
     * part of the same parent station. It was introduced as a workaround to allow in-station 
     * transfers for underground/grade-separated transportation systems like the NYC subway (where
     * it's important to provide in-station transfers for fare computation).
     * 
     * This step used to be automatically applied whenever transfers.txt was used to create 
     * transfers (rathen than or in addition to transfers through the street netowrk), 
     * but has been separated out since it is really a separate process.
     */
    public void createParentStationTransfers () {
        for (Stop stop : _dao.getAllStops()) {
            String parentStation = stop.getParentStation();
            if (parentStation != null) {
                Vertex stopVertex = context.stationStopNodes.get(stop);

                String agencyId = stop.getId().getAgencyId();
                AgencyAndId parentStationId = new AgencyAndId(agencyId, parentStation);

                Stop parentStop = _dao.getStopForId(parentStationId);
                Vertex parentStopVertex = context.stationStopNodes.get(parentStop);

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
    }
    
    /**
     * Links the vertices representing parent stops to their child stops bidirectionally. This is
     * not intended to provide implicit transfers (i.e. child stop to parent station to another
     * child stop) but instead to allow beginning or ending a path (itinerary) at a parent station.
     * 
     * Currently this linking is only intended for use in the long distance path service. The
     * pathparsers should ensure that it is effectively ignored in other path services, and even in
     * the long distance path service anywhere but the beginning or end of a path.
     */
    public void linkStopsToParentStations(Graph graph) {
        for (Stop stop : _dao.getAllStops()) {
            String parentStation = stop.getParentStation();
            if (parentStation != null) {
                TransitStop stopVertex = (TransitStop) context.stationStopNodes.get(stop);
                String agencyId = stop.getId().getAgencyId();
                AgencyAndId parentStationId = new AgencyAndId(agencyId, parentStation);
                Stop parentStop = _dao.getStopForId(parentStationId);
                if(context.stationStopNodes.get(parentStop) instanceof TransitStation) {
                    TransitStation parentStopVertex = (TransitStation)
                            context.stationStopNodes.get(parentStop);
                    new StationStopEdge(parentStopVertex, stopVertex);
                    new StationStopEdge(stopVertex, parentStopVertex);
                } else {
                    LOG.warn(graph.addBuilderAnnotation(new NonStationParentStation(stopVertex)));
                }
            }
        }        
    }
    
    /**
     * Create transfer edges between stops which are listed in transfers.txt.
     * 
     * NOTE: this method is only called when transfersTxtDefinesStationPaths is set to
     * True for a given GFTS feed. 
     */
    public void createTransfersTxtTransfers() {

        /* Create transfer edges based on transfers.txt. */
        for (Transfer transfer : _dao.getAllTransfers()) {

            int type = transfer.getTransferType();
            if (type == 3) // type 3 = transfer not possible
                continue;
            if (transfer.getFromStop().equals(transfer.getToStop())) {
                continue;
            }
            TransitStationStop fromv = context.stationStopNodes.get(transfer.getFromStop());
            TransitStationStop tov = context.stationStopNodes.get(transfer.getToStop());

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
