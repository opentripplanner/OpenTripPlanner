package org.opentripplanner.graph_builder.module.geometry;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.math3.util.FastMath;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.BogusShapeDistanceTraveled;
import org.opentripplanner.graph_builder.issues.BogusShapeGeometry;
import org.opentripplanner.graph_builder.issues.BogusShapeGeometryCaught;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Once transit model entities have been loaded into the graph, this post-processes them to extract and prepare
 * geometries. It also does some other postprocessing involving fares and interlined blocks.
 */
public class GeometryAndBlockProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(GeometryAndBlockProcessor.class);

    private DataImportIssueStore issueStore;

    private static GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    private OtpTransitService transitService;

    private Map<ShapeSegmentKey, LineString> geometriesByShapeSegmentKey = new HashMap<ShapeSegmentKey, LineString>();

    private Map<FeedScopedId, LineString> geometriesByShapeId = new HashMap<FeedScopedId, LineString>();

    private Map<FeedScopedId, double[]> distancesByShapeId = new HashMap<>();

    private FareServiceFactory fareServiceFactory;

    private final double maxStopToShapeSnapDistance;

    private final int maxInterlineDistance;

    public GeometryAndBlockProcessor (GtfsContext context) {
        this(context.getTransitService(), null, -1, -1);
    }

    public GeometryAndBlockProcessor (
            // TODO OTP2 - Operate on the builder, not the transit service and move the executon of
            //           - this to where the builder is in context.
            OtpTransitService transitService,
            // TODO OTP2 - This does not belong here - Do geometry and blocks have anything with
            //           - a FareService.
            FareServiceFactory fareServiceFactory,
            double maxStopToShapeSnapDistance,
            int maxInterlineDistance
    ) {
        this.transitService = transitService;
        this.fareServiceFactory = fareServiceFactory != null ? fareServiceFactory : new DefaultFareServiceFactory();
        this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance > 0 ? maxStopToShapeSnapDistance : 150;
        this.maxInterlineDistance = maxInterlineDistance > 0 ? maxInterlineDistance : 200;
    }

    // TODO OTP2 - Instead of exposing the graph (the entire world) to this class, this class should
    //           - Create a datastructure and return it, then that should be injected into the graph.
    public void run(Graph graph) {
        run(graph, new DataImportIssueStore(false));
    }

    /** Generate the edges. Assumes that there are already vertices in the graph for the stops. */
    @SuppressWarnings("Convert2MethodRef")
    public void run(Graph graph, DataImportIssueStore issueStore) {
        this.issueStore = issueStore;

        fareServiceFactory.processGtfs(transitService);

        /* Assign 0-based numeric codes to all GTFS service IDs. */
        for (FeedScopedId serviceId : transitService.getAllServiceIds()) {
            // TODO: FIX Service code collision for multiple feeds.
            graph.getServiceCodes().put(serviceId, graph.getServiceCodes().size());
        }

        LOG.info("Processing geometries and blocks on graph...");

        // Wwe have to build the hop geometries before we throw away the modified stopTimes, saving
        // only the tripTimes (which don't have enough information to build a geometry). So we keep
        // them here. In the current design, a trip pattern does not have a single geometry, but
        // one per hop, so we store them in an array.
        Map<TripPattern, LineString[]> geometriesByTripPattern = Maps.newHashMap();

        Collection<TripPattern> tripPatterns = transitService.getTripPatterns();

        /* Generate unique short IDs for all the TableTripPatterns. */
        if (!TripPattern.idsAreUniqueAndNotNull(tripPatterns)) {
            TripPattern.generateUniqueIds(tripPatterns);
        }

        /* Generate unique human-readable names for all the TableTripPatterns. */
        TripPattern.generateUniqueNames(tripPatterns, issueStore);

        /* Loop over all new TripPatterns, creating edges, setting the service codes and geometries, etc. */
        ProgressTracker progress = ProgressTracker.track(
                "Generate TripPattern geometries",
                100,
                tripPatterns.size()
        );
        LOG.info(progress.startMessage());

        for (TripPattern tripPattern : tripPatterns) {
            for (Trip trip : tripPattern.getTrips()) {
                // create geometries if they aren't already created
                // note that this is not only done on new trip patterns, because it is possible that
                // there would be a trip pattern with no geometry yet because it failed some of these tests
                if (!geometriesByTripPattern.containsKey(tripPattern) && trip.getShapeId() != null
                        && trip.getShapeId().getId() != null && !trip.getShapeId().getId().equals("")) {
                    // save the geometry to later be applied to the hops
                    geometriesByTripPattern.put(tripPattern,
                            createGeometry(trip.getShapeId(), transitService.getStopTimesForTrip(trip)));
                }
            }
            //Keep lambda! A method-ref would causes incorrect class and line number to be logged
            progress.step(m -> LOG.info(m));
        }
        LOG.info(progress.completeMessage());

        /* Loop over all new TripPatterns setting the service codes and geometries, etc. */
        for (TripPattern tripPattern : tripPatterns) {
            LineString[] hopGeometries = geometriesByTripPattern.get(tripPattern);
            if (hopGeometries != null) {
                // Make a single unified geometry, and also store the per-hop split geometries.
                tripPattern.setHopGeometries(hopGeometries);
            }
            tripPattern.setServiceCodes(graph.getServiceCodes()); // TODO this could be more elegant

            // Store the tripPattern in the Graph so it will be serialized and usable in routing.
            graph.tripPatternForId.put(tripPattern.getId(), tripPattern);
        }

        /* Identify interlined trips and create the necessary edges. */
        interline(tripPatterns, graph);

        /* Is this the wrong place to do this? It should be done on all feeds at once, or at deserialization. */
        // it is already done at deserialization, but standalone mode allows using graphs without serializing them.
        for (TripPattern tableTripPattern : tripPatterns) {
            tableTripPattern.scheduledTimetable.finish();
        }

        graph.putService(FareService.class, fareServiceFactory.makeFareService());
    }

    /**
     * Identify interlined trips (where a physical vehicle continues on to another logical trip)
     * and update the TripPatterns accordingly.
     */
    private void interline(Collection<TripPattern> tripPatterns, Graph graph) {

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
                if (!Strings.isNullOrEmpty(trip.getBlockId())) {
                    tripTimesForBlock.put(new BlockIdAndServiceId(trip), tripTimes);
                    // For space efficiency, only record times that are part of a block.
                    patternForTripTimes.put(tripTimes, pattern);
                }
            }
        }

        // Associate pairs of TripPatterns with lists of trips that continue from one pattern to the other.
        Multimap<P2<TripPattern>, P2<Trip>> interlines = ArrayListMultimap.create();

        // Sort trips within each block by first departure time, then iterate over trips in this block and service,
        // linking them. Has no effect on single-trip blocks.
        SERVICE_BLOCK:
        for (BlockIdAndServiceId block : tripTimesForBlock.keySet()) {
            List<TripTimes> blockTripTimes = tripTimesForBlock.get(block);
            Collections.sort(blockTripTimes);
            TripTimes prev = null;
            for (TripTimes curr : blockTripTimes) {
                if (prev != null) {
                    if (prev.getDepartureTime(prev.getNumStops() - 1) > curr.getArrivalTime(0)) {
                        LOG.error(
                                "Trip times within block {} are not increasing on service {} after trip {}.",
                                block.blockId, block.serviceId, prev.trip.getId());
                        continue SERVICE_BLOCK;
                    }
                    TripPattern prevPattern = patternForTripTimes.get(prev);
                    TripPattern currPattern = patternForTripTimes.get(curr);
                    Stop fromStop = prevPattern.getStop(prevPattern.getStops().size() - 1);
                    Stop toStop = currPattern.getStop(0);
                    double teleportationDistance = SphericalDistanceLibrary.fastDistance(
                            fromStop.getLat(),
                            fromStop.getLon(),
                            toStop.getLat(),
                            toStop.getLon()
                    );
                    if (teleportationDistance > maxInterlineDistance) {
                        // FIXME Trimet data contains a lot of these -- in their data, two trips sharing a block ID just
                        // means that they are served by the same vehicle, not that interlining is automatically allowed.
                        // see #1654
                        // LOG.error(graph.addBuilderAnnotation(new InterliningTeleport(prev.trip, block.blockId, (int)teleportationDistance)));
                        // Only skip this particular interline edge; there may be other valid ones in the block.
                    } else {
                        interlines.put(new P2<>(prevPattern, currPattern),
                                new P2<>(prev.trip, curr.trip));
                    }
                }
                prev = curr;
            }
        }

        // Copy all interline relationships into the field holding them in the graph.
        // TODO: verify whether we need to be keeping track of patterns at all here, or could just accumulate trip-trip relationships.
        for (P2<TripPattern> patterns : interlines.keySet()) {
            for (P2<Trip> trips : interlines.get(patterns)) {
                graph.interlinedTrips.put(trips.first, trips.second);
            }
        }
        LOG.info("Done finding interlining trips.");
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
    private LineString[] createGeometry(FeedScopedId shapeId, List<StopTime> stopTimes) {

        if (hasShapeDist(shapeId, stopTimes)) {
            // this trip has shape_dist in stop_times
            return getHopGeometriesViaShapeDistTravelled(stopTimes, shapeId);
        }

        LineString shapeLineString = getLineStringForShapeId(shapeId);
        if (shapeLineString == null) {
            // this trip has a shape_id, but no such shape exists, and no shape_dist in stop_times
            // create straight line segments between stops for each hop
            return createStraightLineHopeGeometries(stopTimes, shapeId);
        }

        List<LinearLocation> locations = getLinearLocations(stopTimes, shapeLineString);
        if (locations == null) {
            // this only happens on shape which have points very far from
            // their stop sequence. So we'll fall back to trivial stop-to-stop
            // linking, even though theoretically we could do better.
            return createStraightLineHopeGeometries(stopTimes, shapeId);
        }

        return getGeometriesByShape(stopTimes, shapeId, shapeLineString, locations);
    }

    private boolean hasShapeDist(FeedScopedId shapeId, List<StopTime> stopTimes) {
        StopTime st0 = stopTimes.get(0);
        return st0.isShapeDistTraveledSet() && getDistanceForShapeId(shapeId) != null;
    }

    private LineString[] getGeometriesByShape(
        List<StopTime> stopTimes, FeedScopedId shapeId, LineString shape,
        List<LinearLocation> locations
    ) {
        LineString[] geoms = new LineString[stopTimes.size() - 1];
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
            LineString geometry = geometriesByShapeSegmentKey.get(key);

            if (geometry == null) {
                LocationIndexedLine locationIndexed = new LocationIndexedLine(shape);
                geometry = (LineString) locationIndexed.extractLine(startLocation, endLocation);

                // Pack the resulting line string
                CoordinateSequence sequence = new PackedCoordinateSequence.Double(geometry
                        .getCoordinates(), 2);
                geometry = geometryFactory.createLineString(sequence);
            }
            geoms[i] = geometry;
        }
        return geoms;
    }

    private List<LinearLocation> getLinearLocations(List<StopTime> stopTimes, LineString shape) {
        // This trip does not have shape_dist in stop_times, but does have an associated shape.
        ArrayList<IndexedLineSegment> segments = new ArrayList<>();
        for (int i = 0 ; i < shape.getNumPoints() - 1; ++i) {
            segments.add(new IndexedLineSegment(i, shape.getCoordinateN(i), shape.getCoordinateN(i + 1)));
        }
        // Find possible segment matches for each stop.
        List<List<IndexedLineSegment>> possibleSegmentsForStop = new ArrayList<>();
        int minSegmentIndex = 0;
        for (int i = 0; i < stopTimes.size() ; ++i) {
            Stop stop = stopTimes.get(i).getStop();
            Coordinate coord = new Coordinate(stop.getLon(), stop.getLat());
            List<IndexedLineSegment> stopSegments = new ArrayList<>();
            double bestDistance = Double.MAX_VALUE;
            IndexedLineSegment bestSegment = null;
            int maxSegmentIndex = -1;
            int index = -1;
            int minSegmentIndexForThisStop = -1;
            for (IndexedLineSegment segment : segments) {
                index++;
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
            if (stopSegments.size() == 0 && bestSegment != null) {
                //no segments within 150m
                //fall back to nearest segment
                stopSegments.add(bestSegment);
                minSegmentIndex = bestSegment.index;
            } else {
                minSegmentIndex = minSegmentIndexForThisStop;
                stopSegments.sort(new IndexedLineSegmentComparator(coord));
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

        return getStopLocations(possibleSegmentsForStop, stopTimes, 0, -1);
    }

    private LineString[] createStraightLineHopeGeometries(
        List<StopTime> stopTimes, FeedScopedId shapeId
    ) {
        LineString[] geoms = new LineString[stopTimes.size() - 1];
        StopTime st0;
        for (int i = 0; i < stopTimes.size() - 1; ++i) {
            st0 = stopTimes.get(i);
            StopTime st1 = stopTimes.get(i + 1);
            LineString geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
            geoms[i] = geometry;
            //this warning is not strictly correct, but will do
            issueStore.add(new BogusShapeGeometryCaught(shapeId, st0, st1));
        }
        return geoms;
    }

    private LineString[] getHopGeometriesViaShapeDistTravelled(
        List<StopTime> stopTimes, FeedScopedId shapeId
    ) {
        LineString[] geoms = new LineString[stopTimes.size() - 1];
        StopTime st0;
        for (int i = 0; i < stopTimes.size() - 1; ++i) {
            st0 = stopTimes.get(i);
            StopTime st1 = stopTimes.get(i + 1);
            geoms[i] = getHopGeometryViaShapeDistTraveled(shapeId, st0, st1);
        }
        return geoms;
    }

    /**
     * Find a consistent, increasing list of LinearLocations along a shape for a set of stops.
     * Handles loops routes.
     */
    private List<LinearLocation> getStopLocations(List<List<IndexedLineSegment>> possibleSegmentsForStop,
            List<StopTime> stopTimes, int index, int prevSegmentIndex) {

        if (index == stopTimes.size()) {
            return new LinkedList<>();
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

    private LineString getHopGeometryViaShapeDistTraveled(FeedScopedId shapeId, StopTime st0, StopTime st1) {

        double startDistance = st0.getShapeDistTraveled();
        double endDistance = st1.getShapeDistTraveled();

        ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);
        LineString geometry = geometriesByShapeSegmentKey.get(key);
        if (geometry != null)
            return geometry;

        double[] distances = getDistanceForShapeId(shapeId);

        if (distances == null) {
            issueStore.add(new BogusShapeGeometry(shapeId));
            return null;
        } else {
            LinearLocation startIndex = getSegmentFraction(distances, startDistance);
            LinearLocation endIndex = getSegmentFraction(distances, endDistance);

            if (equals(startIndex, endIndex)) {
                //bogus shape_dist_traveled
                issueStore.add(new BogusShapeDistanceTraveled(st1));
                return createSimpleGeometry(st0.getStop(), st1.getStop());
            }
            LineString line = getLineStringForShapeId(shapeId);
            LocationIndexedLine lol = new LocationIndexedLine(line);

            geometry = getSegmentGeometry(
                    shapeId, lol, startIndex, endIndex, startDistance, endDistance, st0, st1
            );

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

        return geometryFactory.createLineString(sequence);
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

    private LineString getSegmentGeometry(FeedScopedId shapeId,
            LocationIndexedLine locationIndexedLine, LinearLocation startIndex,
            LinearLocation endIndex, double startDistance, double endDistance,
            StopTime st0, StopTime st1) {

        ShapeSegmentKey key = new ShapeSegmentKey(shapeId, startDistance, endDistance);

        LineString geometry = geometriesByShapeSegmentKey.get(key);
        if (geometry == null) {

            geometry = (LineString) locationIndexedLine.extractLine(startIndex, endIndex);

            // Pack the resulting line string
            CoordinateSequence sequence = new PackedCoordinateSequence.Double(geometry
                    .getCoordinates(), 2);
            geometry = geometryFactory.createLineString(sequence);

            if (!isValid(geometry, st0.getStop(), st1.getStop())) {
                issueStore.add(new BogusShapeGeometryCaught(shapeId, st0, st1));
                //fall back to trivial geometry
                geometry = createSimpleGeometry(st0.getStop(), st1.getStop());
            }
            geometriesByShapeSegmentKey.put(key, geometry);
        }

        return geometry;
    }

    /**
     * If a shape appears in more than one feed, the shape points will be loaded several
     * times, and there will be duplicates in the DAO. Filter out duplicates and repeated
     * coordinates because 1) they are unnecessary, and 2) they define 0-length line segments
     * which cause JTS location indexed line to return a segment location of NaN,
     * which we do not want.
     */
    private List<ShapePoint> getUniqueShapePointsForShapeId(FeedScopedId shapeId) {
        List<ShapePoint> points = transitService.getShapePointsForShapeId(shapeId);
        ArrayList<ShapePoint> filtered = new ArrayList<>(points.size());
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
            return new ArrayList<>(points);
        }
    }

    private LineString getLineStringForShapeId(FeedScopedId shapeId) {

        LineString geometry = geometriesByShapeId.get(shapeId);

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

        // If we don't have distances here, we can't calculate them ourselves because we can't
        // assume the units will match
        if (!hasAllDistances) {
            distances = null;
        }

        CoordinateSequence sequence = new PackedCoordinateSequence.Double(coordinates, 2);
        geometry = geometryFactory.createLineString(sequence);
        geometriesByShapeId.put(shapeId, geometry);
        distancesByShapeId.put(shapeId, distances);

        return geometry;
    }

    private double[] getDistanceForShapeId(FeedScopedId shapeId) {
        getLineStringForShapeId(shapeId);
        return distancesByShapeId.get(shapeId);
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

    public void setFareServiceFactory(FareServiceFactory fareServiceFactory) {
        this.fareServiceFactory = fareServiceFactory;
    }
}
