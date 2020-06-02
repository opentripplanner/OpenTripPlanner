package org.opentripplanner.graph_builder.module.ned;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.referencing.operation.TransformException;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.graph_builder.annotation.ElevationFlattened;
import org.opentripplanner.graph_builder.module.extra_elevation_data.ElevationPoint;
import org.opentripplanner.graph_builder.annotation.Graphwide;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetWithElevationEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.PolylineEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.opentripplanner.util.ElevationUtils.computeEllipsoidToGeoidDifference;

/**
 * THIS CLASS IS MULTI-THREADED
 * (When configured to do so, it uses parallel streams to distribute elevation calculation tasks for edges.)
 *
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} plugin that applies elevation data to street
 * data that has already been loaded into a (@link Graph}, creating elevation profiles for each Street encountered
 * in the Graph. Data sources that could be used include auto-downloaded and cached National Elevation Dataset (NED)
 * raster data or a GeoTIFF file. The elevation profiles are stored as {@link PackedCoordinateSequence} objects, where
 * each (x,y) pair represents one sample, with the x-coord representing the distance along the edge measured from the
 * start, and the y-coord representing the sampled elevation at that point (both in meters).
 */
public class ElevationModule implements GraphBuilderModule {

    private static final Logger log = LoggerFactory.getLogger(ElevationModule.class);

    /** The elevation data to be used in calculating elevations. */
    private final ElevationGridCoverageFactory gridCoverageFactory;
    /* Whether or not to attempt reading in a file of cached elevations */
    private final boolean readCachedElevations;
    /* Whether or not to attempt writing out a file of cached elevations */
    private final boolean writeCachedElevations;
    /* The file of cached elevations */
    private final File cachedElevationsFile;
    /* Whether or not to include geoid difference values in individual elevation calculations */
    private final boolean includeEllipsoidToGeoidDifference;
    /*
     * Whether or not to use multi-threading when calculating the elevations. For unknown reasons that seem to depend on
     * data and machine settings, it might be faster to use a single processor.
     */
    private final boolean multiThreadElevationCalculations;

    /**
     * A map of PackedCoordinateSequence values identified by Strings of encoded polylines.
     *
     * Note: Since this map has a key of only the encoded polylines, it is assumed that all other inputs are the same as
     * those that occurred in the graph build that produced this data.
     */
    private HashMap<String, PackedCoordinateSequence> cachedElevations;

    // Keep track of the proportion of elevation fetch operations that fail so we can issue warnings. AtomicInteger is
    // used to provide thread-safe updating capabilities.
    private AtomicInteger nEdgesProcessed = new AtomicInteger(0);
    private final AtomicInteger nPointsEvaluated = new AtomicInteger(0);
    private final AtomicInteger nPointsOutsideDEM = new AtomicInteger(0);
    /** keeps track of the total amount of elevation edges for logging purposes */
    private int totalElevationEdges = Integer.MAX_VALUE;

    // the first coordinate in the first StreetWithElevationEdge which is used for initializing coverage instances
    private Coordinate examplarCoordinate;

    /**
     * The distance between samples in meters. Defaults to 10m, the approximate resolution of 1/3
     * arc-second NED data.
     */
    private double distanceBetweenSamplesM = 10;

    /**
     * Unit conversion multiplier for elevation values. No conversion needed if the elevation values
     * are defined in meters in the source data. If, for example, decimetres are used in the source data,
     * this should be set to 0.1 in build-config.json.
     */
    private final double elevationUnitMultiplier;

    /** the graph being built */
    private Graph graph;

    /** A concurrent hashmap used for storing geoid difference values at various coordinates */
    private final ConcurrentHashMap<Integer, Double> geoidDifferenceCache = new ConcurrentHashMap<>();

    /** Used only when the ElevationModule is requested to be ran with a single thread */
    private Coverage singleThreadedCoverageInterpolator;

    private ThreadLocal<Coverage> coverageInterpolatorThreadLocal = new ThreadLocal<>();

    /** used only for testing purposes */
    public ElevationModule(ElevationGridCoverageFactory factory) {
        this(
            factory,
            null,
            false,
            false,
            1,
            true,
            false
        );
    }

    public ElevationModule(
        ElevationGridCoverageFactory factory,
        File cacheDirectory,
        boolean readCachedElevations,
        boolean writeCachedElevations,
        double elevationUnitMultiplier,
        boolean includeEllipsoidToGeoidDifference,
        boolean multiThreadElevationCalculations
    ) {
        gridCoverageFactory = factory;
        cachedElevationsFile = cacheDirectory != null ? new File(cacheDirectory, "cached_elevations.obj") : null;
        this.readCachedElevations = readCachedElevations;
        this.writeCachedElevations = writeCachedElevations;
        this.elevationUnitMultiplier = elevationUnitMultiplier;
        this.includeEllipsoidToGeoidDifference = includeEllipsoidToGeoidDifference;
        this.multiThreadElevationCalculations = multiThreadElevationCalculations;
    }

    /**
     * Use Kryo to serialize/deserialize cached elevations
     */
    private Kryo makeKryo() {
        Kryo kryo = new Kryo();
        kryo.register(HashMap.class);
        kryo.register(PackedCoordinateSequence.Double.class);
        return kryo;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        this.graph = graph;
        gridCoverageFactory.fetchData(graph);

        if (readCachedElevations) {
            // try to load in the cached elevation data
            try {
                Kryo kryo = makeKryo();
                Input input = new Input(new FileInputStream(cachedElevationsFile));
                cachedElevations = (HashMap<String, PackedCoordinateSequence>) kryo.readClassAndObject(input);
                log.info("Cached elevation data loaded into memory!");
            } catch (FileNotFoundException e) {
                log.warn(graph.addBuilderAnnotation(new Graphwide(
                    String.format("Cached elevations file could not be read in due to error: %s!", e.getMessage()))));
            }
        }
        log.info("Setting street elevation profiles from digital elevation model...");

        // At first, set the totalElevationEdges to the total number of edges in the graph.
        totalElevationEdges = graph.countEdges();
        List<StreetWithElevationEdge> streetsWithElevationEdges = new LinkedList<>();
        for (Vertex gv : graph.getVertices()) {
            for (Edge ee : gv.getOutgoing()) {
                if (ee instanceof StreetWithElevationEdge) {
                    if (multiThreadElevationCalculations) {
                        // Multi-threaded execution requested, check and prepare a few things that are used only during
                        // multi-threaded runs.
                        if (examplarCoordinate == null) {
                            // store the first coordinate of the first StreetEdge for later use in initializing coverage
                            // instances
                            examplarCoordinate = ee.getGeometry().getCoordinates()[0];
                        }
                    }
                    streetsWithElevationEdges.add((StreetWithElevationEdge) ee);
                }
            }
        }
        // update this value to the now-known amount of edges that are StreetWithElevation edges
        totalElevationEdges = streetsWithElevationEdges.size();

        if (multiThreadElevationCalculations) {
            // Multi-threaded execution
            streetsWithElevationEdges.parallelStream().forEach(ee -> processEdgeWithProgress(ee));
        } else {
            // If using just a single thread, process each edge inline
            for (StreetWithElevationEdge ee : streetsWithElevationEdges) {
                processEdgeWithProgress(ee);
            }
        }

        double failurePercentage = nPointsOutsideDEM.get() / nPointsEvaluated.get() * 100;
        if (failurePercentage > 50) {
            log.warn(graph.addBuilderAnnotation(new Graphwide(
                String.format(
                    "Fetching elevation failed at %d/%d points (%d%%)",
                    nPointsOutsideDEM, nPointsEvaluated, failurePercentage
                )
            )));
            log.warn("Elevation is missing at a large number of points. DEM may be for the wrong region. " +
                "If it is unprojected, perhaps the axes are not in (longitude, latitude) order.");
        }

        // Iterate again to find edges that had elevation calculated.
        LinkedList<StreetEdge> edgesWithCalculatedElevations = new LinkedList<>();
        for (StreetWithElevationEdge edgeWithElevation : streetsWithElevationEdges) {
            if (edgeWithElevation.hasPackedElevationProfile() && !edgeWithElevation.isElevationFlattened()) {
                edgesWithCalculatedElevations.add(edgeWithElevation);
            }
        }

        if (writeCachedElevations) {
            // write information from edgesWithElevation to a new cache file for subsequent graph builds
            HashMap<String, PackedCoordinateSequence> newCachedElevations = new HashMap<>();
            for (StreetEdge streetEdge : edgesWithCalculatedElevations) {
                newCachedElevations.put(PolylineEncoder.createEncodings(streetEdge.getGeometry()).getPoints(),
                    streetEdge.getElevationProfile());
            }
            try {
                Kryo kryo = makeKryo();
                Output out = new Output(new BufferedOutputStream(new FileOutputStream(cachedElevationsFile)));
                kryo.writeClassAndObject(out, newCachedElevations);
                out.close();
            } catch (IOException e) {
                log.error(e.getMessage());
                log.error(graph.addBuilderAnnotation(new Graphwide("Failed to write cached elevation file!")));
            }
        }
        @SuppressWarnings("unchecked")
        HashMap<Vertex, Double> extraElevation = (HashMap<Vertex, Double>) extra.get(ElevationPoint.class);
        assignMissingElevations(graph, edgesWithCalculatedElevations, extraElevation);
    }

    class ElevationRepairState {
        /* This uses an intuitionist approach to elevation inspection */
        public StreetEdge backEdge;

        public ElevationRepairState backState;

        public Vertex vertex;

        public double distance;

        public double initialElevation;

        public ElevationRepairState(StreetEdge backEdge, ElevationRepairState backState,
                Vertex vertex, double distance, double initialElevation) {
            this.backEdge = backEdge;
            this.backState = backState;
            this.vertex = vertex;
            this.distance = distance;
            this.initialElevation = initialElevation;
        }
    }

    /**
     * Assign missing elevations by interpolating from nearby points with known
     * elevation; also handle osm ele tags
     */
    private void assignMissingElevations(Graph graph, List<StreetEdge> edgesWithElevation, HashMap<Vertex, Double> knownElevations) {

        log.debug("Assigning missing elevations");

        BinHeap<ElevationRepairState> pq = new BinHeap<ElevationRepairState>();

        // elevation for each vertex (known or interpolated)
        // knownElevations will be null if there are no ElevationPoints in the data
        // for instance, with the Shapefile loader.)
        HashMap<Vertex, Double> elevations; 
        if (knownElevations != null)
            elevations = (HashMap<Vertex, Double>) knownElevations.clone();
        else
            elevations = new HashMap<Vertex, Double>();

        // If including the EllipsoidToGeoidDifference, subtract these from the known elevations found in OpenStreetMap
        // data.
        if (includeEllipsoidToGeoidDifference) {
            elevations.forEach((vertex, elevation) -> {
                try {
                    elevations.put(
                        vertex, elevation - getApproximateEllipsoidToGeoidDifference(vertex.getY(), vertex.getX())
                    );
                } catch (TransformException e) {
                    log.error(
                        "Error processing elevation for known elevation at vertex: {} due to error: {}",
                        vertex,
                        e
                    );
                }
            });
        }

        HashSet<Vertex> closed = new HashSet<Vertex>();

        // initialize queue with all vertices which already have known elevation
        for (StreetEdge e : edgesWithElevation) {
            PackedCoordinateSequence profile = e.getElevationProfile();

            if (!elevations.containsKey(e.getFromVertex())) {
                double firstElevation = profile.getOrdinate(0, 1);
                ElevationRepairState state = new ElevationRepairState(null, null,
                        e.getFromVertex(), 0, firstElevation);
                pq.insert(state, 0);
                elevations.put(e.getFromVertex(), firstElevation);
            }

            if (!elevations.containsKey(e.getToVertex())) {
                double lastElevation = profile.getOrdinate(profile.size() - 1, 1);
                ElevationRepairState state = new ElevationRepairState(null, null, e.getToVertex(),
                        0, lastElevation);
                pq.insert(state, 0);
                elevations.put(e.getToVertex(), lastElevation);
            }
        }

        // Grow an SPT outward from vertices with known elevations into regions where the
        // elevation is not known. when a branch hits a region with known elevation, follow the
        // back pointers through the region of unknown elevation, setting elevations via interpolation.
        while (!pq.empty()) {
            ElevationRepairState state = pq.extract_min();

            if (closed.contains(state.vertex)) continue;
            closed.add(state.vertex);

            ElevationRepairState curState = state;
            Vertex initialVertex = null;
            while (curState != null) {
                initialVertex = curState.vertex;
                curState = curState.backState;
            }

            double bestDistance = Double.MAX_VALUE;
            double bestElevation = 0;
            for (Edge e : state.vertex.getOutgoing()) {
                if (!(e instanceof StreetEdge)) {
                    continue;
                }
                StreetEdge edge = (StreetEdge) e;
                Vertex tov = e.getToVertex();
                if (tov == initialVertex)
                    continue;

                Double elevation = elevations.get(tov);
                if (elevation != null) {
                    double distance = e.getDistance();
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestElevation = elevation;
                    }
                } else {
                    // continue
                    ElevationRepairState newState = new ElevationRepairState(edge, state, tov,
                            e.getDistance() + state.distance, state.initialElevation);
                    pq.insert(newState, e.getDistance() + state.distance);
                }
            } // end loop over outgoing edges

            for (Edge e : state.vertex.getIncoming()) {
                if (!(e instanceof StreetEdge)) {
                    continue;
                }
                StreetEdge edge = (StreetEdge) e;
                Vertex fromv = e.getFromVertex();
                if (fromv == initialVertex)
                    continue;
                Double elevation = elevations.get(fromv);
                if (elevation != null) {
                    double distance = e.getDistance();
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestElevation = elevation;
                    }
                } else {
                    // continue
                    ElevationRepairState newState = new ElevationRepairState(edge, state, fromv,
                            e.getDistance() + state.distance, state.initialElevation);
                    pq.insert(newState, e.getDistance() + state.distance);
                }
            } // end loop over incoming edges

            //limit elevation propagation to at max 2km; this prevents an infinite loop
            //in the case of islands missing elevation (and some other cases)
            if (bestDistance == Double.MAX_VALUE && state.distance > 2000) {
                log.warn("While propagating elevations, hit 2km distance limit at " + state.vertex);
                bestDistance = state.distance;
                bestElevation = state.initialElevation;
            }
            if (bestDistance != Double.MAX_VALUE) {
                // we have found a second vertex with elevation, so we can interpolate the elevation
                // for this point
                double totalDistance = bestDistance + state.distance;
                // trace backwards, setting states as we go
                while (true) {
                    // watch out for division by 0 here, which will propagate NaNs 
                    // all the way out to edge lengths 
                    if (totalDistance == 0)
                        elevations.put(state.vertex, bestElevation);
                    else {
                        double elevation = (bestElevation * state.distance + 
                               state.initialElevation * bestDistance) / totalDistance;
                        elevations.put(state.vertex, elevation);
                    }
                    if (state.backState == null)
                        break;
                    bestDistance += state.backEdge.getDistance();
                    state = state.backState;
                    if (elevations.containsKey(state.vertex))
                        break;
                }

            }
        } // end loop over states

        // do actual assignments
        for (Vertex v : graph.getVertices()) {
            Double fromElevation = elevations.get(v);
            for (Edge e : v.getOutgoing()) {
                if (e instanceof StreetWithElevationEdge) {
                    StreetWithElevationEdge edge = ((StreetWithElevationEdge) e);

                    Double toElevation = elevations.get(edge.getToVertex());

                    if (fromElevation == null || toElevation == null) {
                        if (!edge.isElevationFlattened() && !edge.isSlopeOverride())
                            log.warn("Unexpectedly missing elevation for edge " + edge);
                        continue;
                    }

                    if (edge.getElevationProfile() != null && edge.getElevationProfile().size() > 2) {
                        continue;
                    }

                    Coordinate[] coords = new Coordinate[2];
                    coords[0] = new Coordinate(0, fromElevation);
                    coords[1] = new Coordinate(edge.getDistance(), toElevation);

                    PackedCoordinateSequence profile = new PackedCoordinateSequence.Double(coords);

                    if (edge.setElevationProfile(profile, true)) {
                        log.trace(graph.addBuilderAnnotation(new ElevationFlattened(edge)));
                    }
                }
            }
        }
    }

    /**
     * Calculate the elevation for a single street edge. After the calculation is complete, update the current progress.
     */
    private void processEdgeWithProgress(StreetWithElevationEdge ee) {
        processEdge(ee);
        int curNumProcessed = nEdgesProcessed.addAndGet(1);
        if (curNumProcessed % 50_000 == 0) {
            log.info("set elevation on {}/{} edges", curNumProcessed, totalElevationEdges);
        }
    }

    /**
     * Calculate the elevation for a single street edge, creating and assigning the elevation profile.
     *
     * @param ee the street edge
     */
    private void processEdge(StreetWithElevationEdge ee) {
        // First, check if the edge already has been calculated or if it exists in a pre-calculated cache. Checking
        // with this method avoids potentially waiting for a lock to be released for calculating the thread-specific
        // coverage.
        if (ee.hasPackedElevationProfile()) {
            return; /* already set up */
        }

        // first try to find a cached value if possible
        Geometry edgeGeometry = ee.getGeometry();
        if (cachedElevations != null) {
            PackedCoordinateSequence coordinateSequence = cachedElevations.get(
                PolylineEncoder.createEncodings(edgeGeometry).getPoints()
            );
            if (coordinateSequence != null) {
                // found a cached value! Set the elevation profile with the pre-calculated data.
                setEdgeElevationProfile(ee, coordinateSequence, graph);
                return;
            }
        }

        // Needs full calculation. Calculate with a thread-specific coverage instance to avoid waiting for any locks on
        // coverage instances in other threads.
        Coverage coverage = getThreadSpecificCoverageInterpolator();

        // did not find a cached value, calculate
        // If any of the coordinates throw an error when trying to lookup their value, immediately bail and do not
        // process the elevation on the edge
        try {
            Coordinate[] coords = edgeGeometry.getCoordinates();

            List<Coordinate> coordList = new LinkedList<Coordinate>();

            // initial sample (x = 0)
            coordList.add(new Coordinate(0, getElevation(coverage, coords[0])));

            // iterate through coordinates calculating the edge length and creating intermediate elevation coordinates at
            // the regularly specified interval
            double edgeLenM = 0;
            double sampleDistance = distanceBetweenSamplesM;
            double previousDistance = 0;
            double x1 = coords[0].x, y1 = coords[0].y, x2, y2;
            for (int i = 0; i < coords.length - 1; i++) {
                x2 = coords[i + 1].x;
                y2 = coords[i + 1].y;
                double curSegmentDistance = SphericalDistanceLibrary.distance(y1, x1, y2, x2);
                edgeLenM += curSegmentDistance;
                while (edgeLenM > sampleDistance) {
                    // if current edge length is longer than the current sample distance, insert new elevation coordinates
                    // as needed until sample distance has caught up

                    // calculate percent of current segment that distance is between
                    double pctAlongSeg = (sampleDistance - previousDistance) / curSegmentDistance;
                    // add an elevation coordinate
                    coordList.add(
                        new Coordinate(
                            sampleDistance,
                            getElevation(
                                coverage,
                                new Coordinate(
                                    x1 + (pctAlongSeg * (x2 - x1)),
                                    y1 + (pctAlongSeg * (y2 - y1))
                                )
                            )
                        )
                    );
                    sampleDistance += distanceBetweenSamplesM;
                }
                previousDistance = edgeLenM;
                x1 = x2;
                y1 = y2;
            }

            // remove final-segment sample if it is less than half the distance between samples
            if (edgeLenM - coordList.get(coordList.size() - 1).x < distanceBetweenSamplesM / 2) {
                coordList.remove(coordList.size() - 1);
            }

            // final sample (x = edge length)
            coordList.add(new Coordinate(edgeLenM, getElevation(coverage, coords[coords.length - 1])));

            // construct the PCS
            Coordinate coordArr[] = new Coordinate[coordList.size()];
            PackedCoordinateSequence elevPCS = new PackedCoordinateSequence.Double(
                    coordList.toArray(coordArr));

            setEdgeElevationProfile(ee, elevPCS, graph);
        } catch (PointOutsideCoverageException | TransformException e) {
            log.debug("Error processing elevation for edge: {} due to error: {}", ee, e);
        }
    }

    /**
     * Gets a coverage interpolator instance specific to the current thread. If using multiple threads, get the coverage
     * interpolator instance associated with the ElevationWorkerThread. Otherwise, use a class field.
     *
     * For unknown reasons, the interpolation of heights at coordinates is a synchronized method in the commonly used
     * Interpolator2D class. Therefore, it is critical to use a dedicated Coverage interpolator instance for each thread
     * to avoid other threads waiting for a lock to be released on the Coverage interpolator instance.
     *
     * This method will get/lazy-create a thread-specific Coverage interpolator instance. Since these interpolator
     * instances take some time to create, they are lazy-created instead of created upfront because it could lock all
     * other threads even if other threads don't need an interpolator right away if they happen to process a lot of
     * cached data initially.
     */
    private Coverage getThreadSpecificCoverageInterpolator() {
        if (multiThreadElevationCalculations) {
            Coverage coverage = coverageInterpolatorThreadLocal.get();
            if (coverage == null) {
                synchronized (gridCoverageFactory) {
                    coverage = gridCoverageFactory.getGridCoverage();
                    // The Coverage instance relies on some synchronized static methods shared across all threads that
                    // can cause deadlocks if not fully initialized. Therefore, make a single request for the first
                    // point on the edge to initialize these other items.
                    double[] dummy = new double[1];
                    coverage.evaluate(
                        new DirectPosition2D(GeometryUtils.WGS84_XY, examplarCoordinate.x, examplarCoordinate.y),
                        dummy
                    );
                    coverageInterpolatorThreadLocal.set(coverage);
                }
            }
            return coverage;
        } else {
            if (singleThreadedCoverageInterpolator == null) {
                singleThreadedCoverageInterpolator = gridCoverageFactory.getGridCoverage();
            }
            return singleThreadedCoverageInterpolator;
        }
    }

    private void setEdgeElevationProfile(StreetWithElevationEdge ee, PackedCoordinateSequence elevPCS, Graph graph) {
        if(ee.setElevationProfile(elevPCS, false)) {
            synchronized (graph) {
                log.trace(graph.addBuilderAnnotation(new ElevationFlattened(ee)));
            }
        }
    }

    /**
     * Method for retrieving the elevation at a given Coordinate.
     *
     * @param coverage the specific Coverage instance to use in order to avoid competition between threads
     * @param c the coordinate (NAD83)
     * @return elevation in meters
     */
    private double getElevation(Coverage coverage, Coordinate c) throws PointOutsideCoverageException, TransformException {
        return getElevation(coverage, c.x, c.y);
    }

    /**
     * Method for retrieving the elevation at a given (x, y) pair.
     *
     * @param coverage the specific Coverage instance to use in order to avoid competition between threads
     * @param x the query longitude (NAD83)
     * @param y the query latitude (NAD83)
     * @return elevation in meters
     */
    private double getElevation(Coverage coverage, double x, double y) throws PointOutsideCoverageException, TransformException {
        double values[] = new double[1];
        try {
            // We specify a CRS here because otherwise the coordinates are assumed to be in the coverage's native CRS.
            // That assumption is fine when the coverage happens to be in longitude-first WGS84 but we want to support
            // GeoTIFFs in various projections. Note that GeoTools defaults to strict EPSG axis ordering of (lat, long)
            // for DefaultGeographicCRS.WGS84, but OTP is using (long, lat) throughout and assumes unprojected DEM
            // rasters to also use (long, lat).
            coverage.evaluate(new DirectPosition2D(GeometryUtils.WGS84_XY, x, y), values);
        } catch (PointOutsideCoverageException e) {
            nPointsOutsideDEM.incrementAndGet();
            throw e;
        }
        nPointsEvaluated.incrementAndGet();
        return (values[0] * elevationUnitMultiplier) -
            (includeEllipsoidToGeoidDifference ? getApproximateEllipsoidToGeoidDifference(y, x) : 0);
    }

    /**
     * The Calculation of the EllipsoidToGeoidDifference is a very expensive operation, so the resulting values are
     * cached based on the coordinate values up to 2 significant digits. Two significant digits are often more than enough
     * for most parts of the world, but is useful for certain areas that have dramatic changes. Since the values are
     * computed once and cached, it has almost no affect on performance to have this level of detail.
     * See this image for an approximate mapping of these difference values:
     * https://earth-info.nga.mil/GandG/images/ww15mgh2.gif
     *
     * @param y latitude
     * @param x longitude
     */
    private double getApproximateEllipsoidToGeoidDifference(double y, double x) throws TransformException {
        int geoidDifferenceCoordinateValueMultiplier = 100;
        int xVal = (int) Math.round(x * geoidDifferenceCoordinateValueMultiplier);
        int yVal = (int) Math.round(y * geoidDifferenceCoordinateValueMultiplier);
        // create a hash value that can be used to look up the value for the given rounded coordinate. The expected
        // value of xVal should never be less than -18000 (-180 * 100) or more than 18000 (180 * 100), so multiply the
        // yVal by a prime number of a magnitude larger so that there won't be any hash collisions.
        int hash = yVal * 104729 + xVal;
        Double difference = geoidDifferenceCache.get(hash);
        if (difference == null) {
            difference = computeEllipsoidToGeoidDifference(
                yVal / (1.0 * geoidDifferenceCoordinateValueMultiplier),
                xVal / (1.0 * geoidDifferenceCoordinateValueMultiplier)
            );
            geoidDifferenceCache.put(hash, difference);
        }
        return difference;
    }

    @Override
    public void checkInputs() {
        gridCoverageFactory.checkInputs();

        // check for the existence of cached elevation data.
        if (readCachedElevations) {
            if (Files.exists(cachedElevationsFile.toPath())) {
                log.info("Cached elevations file found!");
            } else {
                log.warn("No cached elevations file found or read access not allowed! Unable to load in cached elevations. This could take a while...");
            }
        } else {
            log.warn("Not using cached elevations! This could take a while...");
        }
    }

}