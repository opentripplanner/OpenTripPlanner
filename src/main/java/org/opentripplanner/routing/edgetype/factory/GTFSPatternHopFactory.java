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

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
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
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.annotation.BogusShapeDistanceTraveled;
import org.opentripplanner.graph_builder.annotation.BogusShapeGeometry;
import org.opentripplanner.graph_builder.annotation.BogusShapeGeometryCaught;
import org.opentripplanner.graph_builder.annotation.HopSpeedFast;
import org.opentripplanner.graph_builder.annotation.HopSpeedSlow;
import org.opentripplanner.graph_builder.annotation.HopZeroTime;
import org.opentripplanner.graph_builder.annotation.NegativeDwellTime;
import org.opentripplanner.graph_builder.annotation.NegativeHopTime;
import org.opentripplanner.graph_builder.annotation.NonStationParentStation;
import org.opentripplanner.graph_builder.annotation.RepeatedStops;
import org.opentripplanner.graph_builder.annotation.TripDegenerate;
import org.opentripplanner.graph_builder.annotation.TripUndefinedService;
import org.opentripplanner.graph_builder.annotation.*;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.StationStopEdge;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.impl.OnBoardDepartServiceImpl;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.opentripplanner.routing.services.OnBoardDepartService;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStation;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/* TODO Move this stuff into the geometry library */
class IndexedLineSegment {
    private static final double RADIUS = SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M;
    int index;
    Coordinate start;
    Coordinate end;
    private double lineLength;

    public IndexedLineSegment(int index, Coordinate start, Coordinate end) {
        this.index = index;
        this.start = start;
        this.end = end;
        this.lineLength = SphericalDistanceLibrary.fastDistance(start, end);
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
        double distanceFromStart = SphericalDistanceLibrary.fastDistance(start, coord);
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
        double distanceToStart = SphericalDistanceLibrary.fastDistance(coord, start);
        double distanceToEnd = SphericalDistanceLibrary.fastDistance(coord, end);

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
        double distanceFromEnd = SphericalDistanceLibrary.fastDistance(end, coord);
        double alongTrackDistance = FastMath.acos(FastMath.cos(distanceFromEnd / RADIUS)
            / FastMath.cos(inverseCrossTrackError / RADIUS))
            * RADIUS;
        return alongTrackDistance;
    }

    public double fraction(Coordinate coord) {
        double cte = crossTrackError(coord);
        double distanceToStart = SphericalDistanceLibrary.fastDistance(coord, start);
        double distanceToEnd = SphericalDistanceLibrary.fastDistance(coord, end);

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
        double distanceFromStart = SphericalDistanceLibrary.fastDistance(start, coord);
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

    private GtfsFeedId _feedId;

    private GtfsRelationalDao _dao;

    private CalendarService _calendarService;
    
    private Map<ShapeSegmentKey, LineString> _geometriesByShapeSegmentKey = new HashMap<ShapeSegmentKey, LineString>();

    private Map<AgencyAndId, LineString> _geometriesByShapeId = new HashMap<AgencyAndId, LineString>();

    private Map<AgencyAndId, double[]> _distancesByShapeId = new HashMap<AgencyAndId, double[]>();
    
    private FareServiceFactory fareServiceFactory;

    private Multimap<StopPattern, TripPattern> tripPatterns = HashMultimap.create();

    private GtfsStopContext context = new GtfsStopContext();

    public int subwayAccessTime = 0;

    private double maxStopToShapeSnapDistance = 150;

    public GTFSPatternHopFactory(GtfsContext context) {
        this._feedId = context.getFeedId();
        this._dao = context.getDao();
        this._calendarService = context.getCalendarService();
    }
    
    public GTFSPatternHopFactory() {
        this._feedId = null;
        this._dao = null;
        this._calendarService = null;
    }

    /** Generate the edges. Assumes that there are already vertices in the graph for the stops. */
    public void run(Graph graph) {
        if (fareServiceFactory == null) {
            fareServiceFactory = new DefaultFareServiceFactory();
        }
        fareServiceFactory.processGtfs(_dao);
        
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
         * Timetable/TripPattern.
         */
        ListMultimap<Trip, Frequency> frequenciesForTrip = ArrayListMultimap.create();        
        for(Frequency freq : _dao.getAllFrequencies()) {
            frequenciesForTrip.put(freq.getTrip(), freq);
        }
        
        /* Then loop over all trips, handling each one as a frequency-based or scheduled trip. */
        int freqCount = 0;
        int nonFreqCount = 0;
        
        /* The hops don't actually exist when we build their geometries, but we have to build their geometries
         * below, before we throw away the modified stopTimes, saving only the tripTimes (which don't have enough
         * information to build a geometry). So we keep them here.
         *
         *  A trip pattern actually does not have a single geometry, but one per hop, so we store an array.
         *  FIXME _why_ doesn't it have a single geometry?
         */
        Map<TripPattern, LineString[]> geometriesByTripPattern = Maps.newHashMap();
        
        TRIP : for (Trip trip : trips) {
            if (++tripCount % 100000 == 0) {
                LOG.debug("loading trips {}/{}", tripCount, trips.size());
            }

            // TODO: move to a validator module
            if ( ! _calendarService.getServiceIds().contains(trip.getServiceId())) {
                LOG.warn(graph.addBuilderAnnotation(new TripUndefinedService(trip)));
                continue TRIP; // Invalid trip, skip it, it will break later
            }

            /* Fetch the stop times for this trip. Copy the list since it's immutable. */
            List<StopTime> stopTimes = new ArrayList<StopTime>(_dao.getStopTimesForTrip(trip));

            /* GTFS stop times frequently contain duplicate, missing, or incorrect entries. Repair them. */
            TIntList removedStopSequences = removeRepeatedStops(stopTimes);
            if (!removedStopSequences.isEmpty()) {
                LOG.warn(graph.addBuilderAnnotation(new RepeatedStops(trip, removedStopSequences)));
            }
            filterStopTimes(stopTimes, graph);
            interpolateStopTimes(stopTimes);   
            
            /* If after filtering this trip does not contain at least 2 stoptimes, it does not serve any purpose. */
            if (stopTimes.size() < 2) {
                LOG.warn(graph.addBuilderAnnotation(new TripDegenerate(trip)));
                continue TRIP;
            }

            /* Try to get the direction id for the trip, set to -1 if not found */
            int directionId;
            try {
                directionId = Integer.parseInt(trip.getDirectionId());
            } catch (NumberFormatException e) {
                LOG.debug("Trip {} does not have direction id, defaults to -1");
                directionId = -1;
            }

            /* Get the existing TripPattern for this filtered StopPattern, or create one. */
            StopPattern stopPattern = new StopPattern(stopTimes);
            TripPattern tripPattern = findOrCreateTripPattern(stopPattern, trip.getRoute(), directionId);

            /* Create a TripTimes object for this list of stoptimes, which form one trip. */
            TripTimes tripTimes = new TripTimes(trip, stopTimes, graph.deduplicator);

            /* If this trip is referenced by one or more lines in frequencies.txt, wrap it in a FrequencyEntry. */
            List<Frequency> frequencies = frequenciesForTrip.get(trip);
            if (frequencies != null && !(frequencies.isEmpty())) {
                for (Frequency freq : frequencies) {
                    tripPattern.add(new FrequencyEntry(freq, tripTimes));
                    freqCount++;
                }
                // TODO replace: createGeometry(graph, trip, stopTimes, hops);
            }

            /* This trip was not frequency-based. Add the TripTimes directly to the TripPattern's scheduled timetable. */
            else {
                tripPattern.add(tripTimes);
                nonFreqCount++;
            }
            
            // create geometries if they aren't already created
            // note that this is not only done on new trip patterns, because it is possible that
            // there would be a trip pattern with no geometry yet because it failed some of these tests
            if (!geometriesByTripPattern.containsKey(tripPattern) && 
                    trip.getShapeId() != null && trip.getShapeId().getId() != null &&
                    !trip.getShapeId().getId().equals("")) {
                // save the geometry to later be applied to the hops
                geometriesByTripPattern.put(tripPattern,  createGeometry(graph, trip, stopTimes));
            }


        } // end foreach TRIP
        LOG.info("Added {} frequency-based and {} single-trip timetable entries.", freqCount, nonFreqCount);
        graph.hasFrequencyService = graph.hasFrequencyService || freqCount > 0;
        graph.hasScheduledService = graph.hasScheduledService || nonFreqCount > 0;

        /* Generate unique human-readable names for all the TableTripPatterns. */
        TripPattern.generateUniqueNames(tripPatterns.values());

        /* Generate unique short IDs for all the TableTripPatterns. */
        TripPattern.generateUniqueIds(tripPatterns.values());

        /* Loop over all new TripPatterns, creating edges, setting the service codes and geometries, etc. */
        for (TripPattern tripPattern : tripPatterns.values()) {
            tripPattern.makePatternVerticesAndEdges(graph, context.stationStopNodes);
            // Add the geometries to the hop edges.
            LineString[] geom = geometriesByTripPattern.get(tripPattern);
            if (geom != null) {
                for (int i = 0; i < tripPattern.hopEdges.length; i++) {
                    tripPattern.hopEdges[i].setGeometry(geom[i]);
                }
                // Make a geometry for the whole TripPattern from all its constituent hops.
                // This happens only if geometry is found in geometriesByTripPattern,
                // because that means that geometry was created from shapes instead "as crow flies"
                tripPattern.makeGeometry();
            }
            tripPattern.setServiceCodes(graph.serviceCodes); // TODO this could be more elegant

            /* Iterate over all stops in this pattern recording mode information. */
            TraverseMode mode = GtfsLibrary.getTraverseMode(tripPattern.route);
            for (TransitStop tstop : tripPattern.stopVertices) {
                tstop.addMode(mode);
                if (mode == TraverseMode.SUBWAY) {
                    tstop.setStreetToStopTime(subwayAccessTime);
                }
                graph.addTransitMode(mode);
            }

        }

        /* Identify interlined trips and create the necessary edges. */
        interline(tripPatterns.values(), graph);

        /* Interpret the transfers explicitly defined in transfers.txt. */
        loadTransfers(graph);

        /* Is this the wrong place to do this? It should be done on all feeds at once, or at deserialization. */
        // it is already done at deserialization, but standalone mode allows using graphs without serializing them.
        for (TripPattern tableTripPattern : tripPatterns.values()) {
            tableTripPattern.scheduledTimetable.finish();
        }
        
        clearCachedData(); // eh?
        graph.putService(FareService.class, fareServiceFactory.makeFareService());
        graph.putService(OnBoardDepartService.class, new OnBoardDepartServiceImpl());
    }

    private TripPattern findOrCreateTripPattern(StopPattern stopPattern, Route route, int directionId) {
        for(TripPattern tripPattern : tripPatterns.get(stopPattern)) {
            if(tripPattern.route.equals(route) && tripPattern.directionId == directionId) {
                return tripPattern;
            }
        }

        TripPattern tripPattern = new TripPattern(route, stopPattern);
        tripPattern.directionId = directionId;
        tripPatterns.put(stopPattern, tripPattern);
        return tripPattern;
    }

    /**
     * Identify interlined trips (where a physical vehicle continues on to another logical trip)
     * and update the TripPatterns accordingly. This must be called after all the pattern edges and vertices
     * are already created, because it creates interline dwell edges between existing pattern arrive/depart vertices.
     */
    private void interline (Collection<TripPattern> tripPatterns, Graph graph) {

        /* Record which Pattern each interlined TripTimes belongs to. */
        Map<TripTimes, TripPattern> patternForTripTimes = Maps.newHashMap();

        /* TripTimes grouped by the block ID and service ID of their trips. Must be a ListMultimap to allow sorting. */
        ListMultimap<BlockIdAndServiceId, TripTimes> tripTimesForBlock = ArrayListMultimap.create();

        LOG.info("Finding interlining trips based on block IDs.");
        for (TripPattern pattern : tripPatterns) {
            Timetable timetable = pattern.scheduledTimetable;
            /* TODO: Block semantics seem undefined for frequency trips, so skip them? */
            for (TripTimes tripTimes : timetable.tripTimes) {
                Trip trip = tripTimes.trip;
                if ( ! Strings.isNullOrEmpty(trip.getBlockId())) {
                    tripTimesForBlock.put(new BlockIdAndServiceId(trip), tripTimes);
                    // For space efficiency, only record times that are part of a block.
                    patternForTripTimes.put(tripTimes, pattern);
                }
            }
        }

        /* Associate pairs of TripPatterns with lists of trips that continue from one pattern to the other. */
        Multimap<P2<TripPattern>, P2<Trip>> interlines = ArrayListMultimap.create();

        /*
          Sort trips within each block by first departure time, then iterate over trips in this block and service,
          linking them. Has no effect on single-trip blocks.
         */
        SERVICE_BLOCK :
        for (BlockIdAndServiceId block : tripTimesForBlock.keySet()) {
            List<TripTimes> blockTripTimes = tripTimesForBlock.get(block);
            Collections.sort(blockTripTimes);
            TripTimes prev = null;
            for (TripTimes curr : blockTripTimes) {
                if (prev != null) {
                    if (prev.getDepartureTime(prev.getNumStops() - 1) > curr.getArrivalTime(0)) {
                        LOG.error("Trip times within block {} are not increasing on service {} after trip {}.",
                                block.blockId, block.serviceId, prev.trip.getId());
                        continue SERVICE_BLOCK;
                    }
                    TripPattern prevPattern = patternForTripTimes.get(prev);
                    TripPattern currPattern = patternForTripTimes.get(curr);
                    Stop fromStop = prevPattern.getStop(prevPattern.getStops().size() - 1);
                    Stop toStop   = currPattern.getStop(0);
                    double teleportationDistance = SphericalDistanceLibrary.fastDistance(
                                        fromStop.getLat(), fromStop.getLon(), toStop.getLat(), toStop.getLon());
                    if (teleportationDistance > 200) {
                        // FIXME Trimet data contains a lot of these -- in their data, two trips sharing a block ID just
                        // means that they are served by the same vehicle, not that interlining is automatically allowed.
                        // see #1654
                        // LOG.error(graph.addBuilderAnnotation(new InterliningTeleport(prev.trip, block.blockId, (int)teleportationDistance)));
                        // Only skip this particular interline edge; there may be other valid ones in the block.
                    } else {
                        interlines.put(new P2<TripPattern>(prevPattern, currPattern), new P2<Trip>(prev.trip, curr.trip));
                    }
                }
                prev = curr;
            }
        }

        /*
          Create the PatternInterlineDwell edges linking together TripPatterns.
          All the pattern vertices and edges must already have been created.
         */
        for (P2<TripPattern> patterns : interlines.keySet()) {
            TripPattern prevPattern = patterns.first;
            TripPattern nextPattern = patterns.second;
            // This is a single (uni-directional) edge which may be traversed forward and backward.
            PatternInterlineDwell edge = new PatternInterlineDwell(prevPattern, nextPattern);
            for (P2<Trip> trips : interlines.get(patterns)) {
                edge.add(trips.first, trips.second);
            }
        }
        LOG.info("Done finding interlining trips and creating the corresponding edges.");
    }

    /**
     * Creates a set of geometries for a single trip, considering the GTFS shapes.txt,
     * The geometry is broken down into one geometry per inter-stop segment ("hop"). We also need a shape for the entire
     * trip and tripPattern, but given the complexity of the existing code for generating hop geometries, we will create
     * the full-trip geometry by simply concatenating the hop geometries.
     *
     * This geometry will in fact be used for an entire set of trips in a trip pattern. Technically one of the trips
     * with exactly the same sequence of stops could follow a different route on the streets, but that's very uncommon.
     */
    private LineString[] createGeometry(Graph graph, Trip trip, List<StopTime> stopTimes) {
        AgencyAndId shapeId = trip.getShapeId();
        
        // One less geometry than stoptime as array indexes represetn hops not stops (fencepost problem).
        LineString[] geoms = new LineString[stopTimes.size() - 1];
        
        // Detect presence or absence of shape_dist_traveled on a per-trip basis
        StopTime st0 = stopTimes.get(0);
        boolean hasShapeDist = st0.isShapeDistTraveledSet();
        if (hasShapeDist) {
            // this trip has shape_dist in stop_times
            for (int i = 0; i < stopTimes.size() - 1; ++i) {
                st0 = stopTimes.get(i);
                StopTime st1 = stopTimes.get(i + 1);
                geoms[i] = getHopGeometryViaShapeDistTraveled(graph, shapeId, st0, st1);
            }
            return geoms;
        }
        LineString shape = getLineStringForShapeId(shapeId);
        if (shape == null) {
            // this trip has a shape_id, but no such shape exists, and no shape_dist in stop_times
            // create straight line segments between stops for each hop
            for (int i = 0; i < stopTimes.size() - 1; ++i) {
                st0 = stopTimes.get(i);
                StopTime st1 = stopTimes.get(i + 1);
                LineString geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
                geoms[i] = geometry;
            }
            return geoms;
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
                geoms[i] = geometry;
                //this warning is not strictly correct, but will do
                LOG.warn(graph.addBuilderAnnotation(new BogusShapeGeometryCaught(shapeId, st0, st1)));
            }
            return geoms;
        }

        Iterator<LinearLocation> locationIt = locations.iterator();
        LinearLocation endLocation = locationIt.next();
        double distanceSoFar = 0;
        int last = 0;
        for (int i = 0; i < stopTimes.size() - 1; ++i) {
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
            geoms[i] = geometry;
        }
        
        return geoms;
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

        /* If the feed does not specify any timepoints, we want to mark all times that are present as timepoints. */
        boolean hasTimepoints = false;
        for (StopTime stopTime : stopTimes) {
            if (stopTime.getTimepoint() == 1) {
                hasTimepoints = true;
                break;
            }
        }
        // TODO verify that the first (and last?) stop should always be considered a timepoint.
        if (!hasTimepoints) st0.setTimepoint(1);

        /* Indicates that stop times in this trip are being shifted forward one day. */
        boolean midnightCrossed = false;
        
        for (int i = 1; i < stopTimes.size(); i++) {
            boolean st1bogus = false;
            StopTime st1 = stopTimes.get(i);

            /* If the feed did not specify any timepoints, mark all times that are present as timepoints. */
            if ( !hasTimepoints && (st1.isDepartureTimeSet() || st1.isArrivalTimeSet())) {
                st1.setTimepoint(1);
            }

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
            double hopDistance = SphericalDistanceLibrary.fastDistance(
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
            graph.addAgency(_feedId.getId(), agency);
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
                LOG.error("Skipping stop {} because we already loaded an identical ID.", stop.getId());
                continue;
            }
            context.stops.add(stop.getId());

            int locationType = stop.getLocationType();

            //add a vertex representing the stop
            if (locationType == 1) {
                context.stationStopNodes.put(stop, new TransitStation(graph, stop));
            } else {
                TransitStop stopVertex = new TransitStop(graph, stop);
                context.stationStopNodes.put(stop, stopVertex);
                if (locationType != 2) {
                    // Add a vertex representing arriving at the stop
                    TransitStopArrive arrive = new TransitStopArrive(graph, stop, stopVertex);
                    // FIXME no need for this context anymore, we just put references to these nodes in the stop vertices themselves.
                    context.stopArriveNodes.put(stop, arrive);
                    stopVertex.arriveVertex = arrive;

                    // Add a vertex representing departing from the stop
                    TransitStopDepart depart = new TransitStopDepart(graph, stop, stopVertex);
                    // FIXME no need for this context anymore, we just put references to these nodes in the stop vertices themselves.
                    context.stopDepartNodes.put(stop, depart);
                    stopVertex.departVertex = depart;

                    // Add edges from arrive to stop and stop to depart
                    new PreAlightEdge(arrive, stopVertex);
                    new PreBoardEdge(stopVertex, depart);
                }
            }
        }
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

    
    private LineString getHopGeometryViaShapeDistTraveled(Graph graph, AgencyAndId shapeId, StopTime st0, StopTime st1) {

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
        if (SphericalDistanceLibrary.fastDistance(startCoord, geometryStartCoord) > maxStopToShapeSnapDistance) {
            return false;
        } else if (SphericalDistanceLibrary.fastDistance(endCoord, geometryEndCoord) > maxStopToShapeSnapDistance) {
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
    private TIntList removeRepeatedStops (List<StopTime> stopTimes) {
        boolean filtered = false;
        StopTime prev = null;
        Iterator<StopTime> it = stopTimes.iterator();
        TIntList stopSequencesRemoved = new TIntArrayList();
        while (it.hasNext()) {
            StopTime st = it.next();
            if (prev != null) {
                if (prev.getStop().equals(st.getStop())) {
                    // OBA gives us unmodifiable lists, but we have copied them.

                    // Merge the two stop times, making sure we're not throwing out a stop time with times in favor of an
                    // interpolated stop time
                    // keep the arrival time of the previous stop, unless it didn't have an arrival time, in which case
                    // replace it with the arrival time of this stop time
                    // This is particularly important at the last stop in a route (see issue #2220)
                    if (prev.getArrivalTime() == StopTime.MISSING_VALUE) prev.setArrivalTime(st.getArrivalTime());

                    // prefer to replace with the departure time of this stop time, unless this stop time has no departure time
                    if (st.getDepartureTime() != StopTime.MISSING_VALUE) prev.setDepartureTime(st.getDepartureTime());

                    it.remove();
                    stopSequencesRemoved.add(st.getStopSequence());
                }
            }
            prev = st;
        }
        return stopSequencesRemoved;
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
     * transfers (rather than or in addition to transfers through the street netowrk),
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

            double distance = SphericalDistanceLibrary.distance(fromv.getCoordinate(), tov.getCoordinate());
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
