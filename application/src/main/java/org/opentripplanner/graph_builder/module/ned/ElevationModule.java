package org.opentripplanner.graph_builder.module.ned;

import static org.opentripplanner.street.model.elevation.ElevationUtils.computeEllipsoidToGeoidDifference;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.geotools.api.coverage.Coverage;
import org.geotools.api.coverage.PointOutsideCoverageException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ElevationFlattened;
import org.opentripplanner.graph_builder.issues.ElevationProfileFailure;
import org.opentripplanner.graph_builder.issues.Graphwide;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetElevationExtensionBuilder;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * THIS CLASS IS MULTI-THREADED (When configured to do so, it uses parallel streams to distribute
 * elevation calculation tasks for edges.)
 * <p>
 * {@link GraphBuilderModule} plugin that applies
 * elevation data to street data that has already been loaded into a (@link Graph}, creating
 * elevation profiles for each Street encountered in the Graph. Data sources that could be used
 * include auto-downloaded and cached National Elevation Dataset (NED) raster data or a GeoTIFF
 * file. The elevation profiles are stored as {@link PackedCoordinateSequence} objects, where each
 * (x,y) pair represents one sample, with the x-coord representing the distance along the edge
 * measured from the start, and the y-coord representing the sampled elevation at that point (both
 * in meters).
 */
public class ElevationModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(ElevationModule.class);
  /**
   * The WGS84 CRS with longitude-first axis order. The first time a CRS lookup is
   * performed is surprisingly expensive (around 500ms), apparently due to  initializing
   * an HSQLDB JDBC connection. For this reason, the constant is defined in this
   * narrower scope rather than a shared utility class, where it was seen to incur the
   * initialization cost in a broader range of tests than is necessary.
   */
  private static final CoordinateReferenceSystem WGS84_XY;

  static {
    try {
      WGS84_XY = CRS.getAuthorityFactory(true).createCoordinateReferenceSystem("EPSG:4326");
    } catch (Exception ex) {
      LOG.error("Unable to create longitude-first WGS84 CRS", ex);
      throw new RuntimeException(
        "Could not create longitude-first WGS84 coordinate reference system."
      );
    }
  }

  /** The elevation data to be used in calculating elevations. */
  private final ElevationGridCoverageFactory gridCoverageFactory;
  /* Whether or not to attempt reading in a file of cached elevations */
  private final boolean readCachedElevations;
  /* Whether or not to attempt writing out a file of cached elevations */
  private final boolean writeCachedElevations;
  private final Graph graph;
  /* The file of cached elevations */
  private final File cachedElevationsFile;
  private final double maxElevationPropagationMeters;
  /* Whether or not to include geoid difference values in individual elevation calculations */
  private final boolean includeEllipsoidToGeoidDifference;
  /*
   * Whether or not to use multi-threading when calculating the elevations. For unknown reasons that seem to depend on
   * data and machine settings, it might be faster to use a single processor.
   */
  private final boolean multiThreadElevationCalculations;
  // Keep track of the proportion of elevation fetch operations that fail so we can issue warnings. AtomicInteger is
  // used to provide thread-safe updating capabilities.
  private final AtomicInteger nPointsEvaluated = new AtomicInteger(0);
  private final AtomicInteger nPointsOutsideDEM = new AtomicInteger(0);
  private final double distanceBetweenSamplesM;

  /** A concurrent hashmap used for storing geoid difference values at various coordinates */
  private final ConcurrentHashMap<Integer, Double> geoidDifferenceCache = new ConcurrentHashMap<>();
  private final ThreadLocal<Coverage> coverageInterpolatorThreadLocal = new ThreadLocal<>();
  private final DataImportIssueStore issueStore;
  /**
   * A map of PackedCoordinateSequence values identified by Strings of encoded polylines.
   * <p>
   * Note: Since this map has a key of only the encoded polylines, it is assumed that all other
   * inputs are the same as those that occurred in the graph build that produced this data.
   */
  private HashMap<String, PackedCoordinateSequence> cachedElevations;
  // the first coordinate in the first StreetWithElevationEdge which is used for initializing coverage instances
  private Coordinate examplarCoordinate;
  /** Used only when the ElevationModule is requested to be ran with a single thread */
  private Coverage singleThreadedCoverageInterpolator;
  private double minElevation = Double.MAX_VALUE;
  private double maxElevation = Double.MIN_VALUE;

  private final Map<Vertex, Double> elevationData;

  /** used only for testing purposes */
  public ElevationModule(ElevationGridCoverageFactory factory, Graph graph) {
    this(
      factory,
      graph,
      DataImportIssueStore.NOOP,
      null,
      new HashMap<>(),
      false,
      false,
      10,
      2000,
      true,
      false
    );
  }

  public ElevationModule(
    ElevationGridCoverageFactory factory,
    Graph graph,
    DataImportIssueStore issueStore,
    File cachedElevationsFile,
    Map<Vertex, Double> elevationData,
    boolean readCachedElevations,
    boolean writeCachedElevations,
    double distanceBetweenSamplesM,
    double maxElevationPropagationMeters,
    boolean includeEllipsoidToGeoidDifference,
    boolean multiThreadElevationCalculations
  ) {
    gridCoverageFactory = factory;
    this.graph = graph;
    this.issueStore = issueStore;
    this.cachedElevationsFile = cachedElevationsFile;
    this.elevationData = elevationData;
    this.readCachedElevations = readCachedElevations;
    this.writeCachedElevations = writeCachedElevations;
    this.maxElevationPropagationMeters = maxElevationPropagationMeters;
    this.includeEllipsoidToGeoidDifference = includeEllipsoidToGeoidDifference;
    this.multiThreadElevationCalculations = multiThreadElevationCalculations;
    this.distanceBetweenSamplesM = distanceBetweenSamplesM;
  }

  @Override
  public void buildGraph() {
    Instant start = Instant.now();
    gridCoverageFactory.fetchData(graph);

    graph.setDistanceBetweenElevationSamples(this.distanceBetweenSamplesM);

    // try to load in the cached elevation data
    if (readCachedElevations) {
      // try to load in the cached elevation data
      try {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(cachedElevationsFile));
        cachedElevations = (HashMap<String, PackedCoordinateSequence>) in.readObject();
        LOG.info("Cached elevation data loaded into memory!");
      } catch (IOException | ClassNotFoundException e) {
        issueStore.add(
          new Graphwide(
            String.format(
              "Cached elevations file could not be read in due to error: %s!",
              e.getMessage()
            )
          )
        );
      }
    }
    LOG.info("Setting street elevation profiles from digital elevation model...");

    List<StreetEdge> streetsWithElevationEdges = new LinkedList<>();

    for (Vertex gv : graph.getVertices()) {
      for (Edge ee : gv.getOutgoing()) {
        if (ee instanceof StreetEdge) {
          if (multiThreadElevationCalculations) {
            // Multi-threaded execution requested, check and prepare a few things that are used only during
            // multi-threaded runs.
            if (examplarCoordinate == null) {
              // store the first coordinate of the first StreetEdge for later use in initializing coverage
              // instances
              examplarCoordinate = ee.getGeometry().getCoordinates()[0];
            }
          }
          streetsWithElevationEdges.add((StreetEdge) ee);
        }
      }
    }

    // Keeps track of the total amount of elevation edges for logging purposes
    int totalElevationEdges = streetsWithElevationEdges.size();

    var progress = ProgressTracker.track("Set elevation", 25_000, totalElevationEdges);

    if (multiThreadElevationCalculations) {
      // Multi-threaded execution
      streetsWithElevationEdges
        .parallelStream()
        .forEach(ee -> processEdgeWithProgress(ee, progress));
    } else {
      // If using just a single thread, process each edge inline
      for (StreetEdge ee : streetsWithElevationEdges) {
        processEdgeWithProgress(ee, progress);
      }
    }

    int nPoints = nPointsEvaluated.get() + nPointsOutsideDEM.get();
    if (nPoints > 0) {
      double failurePercentage = ((double) nPointsOutsideDEM.get() / nPoints) * 100.0;
      if (failurePercentage > 50) {
        issueStore.add(
          new Graphwide(
            String.format(
              "Fetching elevation failed at %d/%d points (%.1f%%)",
              nPointsOutsideDEM.get(),
              nPoints,
              failurePercentage
            )
          )
        );
        LOG.warn(
          "Elevation is missing at a large number of points. DEM may be for the wrong region. " +
          "If it is unprojected, perhaps the axes are not in (longitude, latitude) order."
        );
      }
    }

    LOG.info(progress.completeMessage());

    // Iterate again to find edges that had elevation calculated.
    LinkedList<StreetEdge> edgesWithCalculatedElevations = new LinkedList<>();
    for (StreetEdge edgeWithElevation : streetsWithElevationEdges) {
      if (edgeWithElevation.hasElevationExtension() && !edgeWithElevation.isElevationFlattened()) {
        edgesWithCalculatedElevations.add(edgeWithElevation);
      }
    }

    if (writeCachedElevations) {
      // write information from edgesWithElevation to a new cache file for subsequent graph builds
      LOG.info("Writing elevation cache");
      HashMap<String, PackedCoordinateSequence> newCachedElevations = new HashMap<>();
      for (StreetEdge streetEdge : edgesWithCalculatedElevations) {
        newCachedElevations.put(
          EncodedPolyline.encode(streetEdge.getGeometry()).points(),
          streetEdge.getElevationProfile()
        );
      }
      try {
        ObjectOutputStream out = new ObjectOutputStream(
          new BufferedOutputStream(new FileOutputStream(cachedElevationsFile))
        );
        out.writeObject(newCachedElevations);
        out.close();
      } catch (IOException e) {
        issueStore.add(new Graphwide("Failed to write cached elevation file: " + e.getMessage()));
      }
    }

    @SuppressWarnings("unchecked")
    var elevationsForVertices = collectKnownElevationsForVertices(
      elevationData,
      edgesWithCalculatedElevations
    );

    new MissingElevationHandler(
      issueStore,
      elevationsForVertices,
      maxElevationPropagationMeters
    ).run();

    updateElevationMetadata(graph);

    LOG.info(
      "Finished elevation processing in {}",
      DurationUtils.durationToStr(Duration.between(start, Instant.now()))
    );
  }

  @Override
  public void checkInputs() {
    gridCoverageFactory.checkInputs();

    // check for the existence of cached elevation data.
    if (readCachedElevations) {
      if (Files.exists(cachedElevationsFile.toPath())) {
        LOG.info("Cached elevations file found!");
      } else {
        LOG.warn(
          "No cached elevations file found at {} or read access not allowed! Unable " +
          "to load in cached elevations. This could take a while...",
          cachedElevationsFile.toPath().toAbsolutePath()
        );
      }
    } else {
      LOG.warn("Not using cached elevations! This could take a while...");
    }
  }

  private void updateElevationMetadata(Graph graph) {
    if (nPointsOutsideDEM.get() < nPointsEvaluated.get()) {
      graph.hasElevation = true;
      graph.minElevation = minElevation;
      graph.maxElevation = maxElevation;
    }
  }

  private Map<Vertex, Double> collectKnownElevationsForVertices(
    Map<Vertex, Double> knownElevations,
    List<StreetEdge> edgesWithElevation
  ) {
    // knownElevations will be null if there are no ElevationPoints in the data
    // for instance, with the Shapefile loader.)
    var elevations = knownElevations != null
      ? new HashMap<>(knownElevations)
      : new HashMap<Vertex, Double>();

    // If including the EllipsoidToGeoidDifference, subtract these from the known elevations
    // found in OpenStreetMap data.
    if (includeEllipsoidToGeoidDifference) {
      elevations.forEach((vertex, elevation) -> {
        try {
          elevations.put(
            vertex,
            elevation - getApproximateEllipsoidToGeoidDifference(vertex.getY(), vertex.getX())
          );
        } catch (TransformException e) {
          LOG.error(
            "Error processing elevation for known elevation at vertex: {} due to error: {}",
            vertex,
            e
          );
        }
      });
    }

    // add all vertices which known elevation
    for (StreetEdge e : edgesWithElevation) {
      PackedCoordinateSequence profile = e.getElevationProfile();

      if (!elevations.containsKey(e.getFromVertex())) {
        double firstElevation = profile.getOrdinate(0, 1);
        elevations.put(e.getFromVertex(), firstElevation);
      }

      if (!elevations.containsKey(e.getToVertex())) {
        double lastElevation = profile.getOrdinate(profile.size() - 1, 1);
        elevations.put(e.getToVertex(), lastElevation);
      }
    }

    return elevations;
  }

  /**
   * Calculate the elevation for a single street edge. After the calculation is complete, update the
   * current progress.
   */
  private void processEdgeWithProgress(StreetEdge ee, ProgressTracker progress) {
    processEdge(ee);
    // Keep lambda to get correct line number in log
    //noinspection Convert2MethodRef
    progress.step(m -> LOG.info(m));
  }

  /**
   * Calculate the elevation for a single street edge, creating and assigning the elevation
   * profile.
   *
   * @param ee the street edge
   */
  private void processEdge(StreetEdge ee) {
    // First, check if the edge already has been calculated or if it exists in a pre-calculated cache. Checking
    // with this method avoids potentially waiting for a lock to be released for calculating the thread-specific
    // coverage.
    if (ee.hasElevationExtension()) {
      return;/* already set up */
    }

    // first try to find a cached value if possible
    Geometry edgeGeometry = ee.getGeometry();
    if (cachedElevations != null) {
      PackedCoordinateSequence coordinateSequence = cachedElevations.get(
        EncodedPolyline.encode(edgeGeometry).points()
      );
      if (coordinateSequence != null) {
        // found a cached value! Set the elevation profile with the pre-calculated data.
        setEdgeElevationProfile(ee, coordinateSequence);
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

      List<Coordinate> coordList = new LinkedList<>();

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
                new Coordinate(x1 + (pctAlongSeg * (x2 - x1)), y1 + (pctAlongSeg * (y2 - y1)))
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
      Coordinate[] coordArr = new Coordinate[coordList.size()];
      PackedCoordinateSequence elevPCS = new PackedCoordinateSequence.Double(
        coordList.toArray(coordArr)
      );

      setEdgeElevationProfile(ee, elevPCS);
    } catch (ElevationLookupException e) {
      issueStore.add(new ElevationProfileFailure(ee, e.getMessage()));
    }
  }

  /**
   * Gets a coverage interpolator instance specific to the current thread. If using multiple
   * threads, get the coverage interpolator instance associated with the ElevationWorkerThread.
   * Otherwise, use a class field.
   * <p>
   * For unknown reasons, the interpolation of heights at coordinates is a synchronized method in
   * the commonly used Interpolator2D class. Therefore, it is critical to use a dedicated Coverage
   * interpolator instance for each thread to avoid other threads waiting for a lock to be released
   * on the Coverage interpolator instance.
   * <p>
   * This method will get/lazy-create a thread-specific Coverage interpolator instance. Since these
   * interpolator instances take some time to create, they are lazy-created instead of created
   * upfront because it could lock all other threads even if other threads don't need an
   * interpolator right away if they happen to process a lot of cached data initially.
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
          try {
            getElevation(coverage, examplarCoordinate);
          } catch (ElevationLookupException e) {
            LOG.warn(
              "Error processing elevation for coordinate: {} due to error: {}",
              examplarCoordinate,
              e
            );
          }
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

  private void setEdgeElevationProfile(StreetEdge ee, PackedCoordinateSequence elevPCS) {
    try {
      StreetElevationExtensionBuilder.of(ee)
        .withElevationProfile(elevPCS)
        .withComputed(false)
        .build()
        .ifPresent(ee::setElevationExtension);
      if (ee.isElevationFlattened()) {
        issueStore.add(new ElevationFlattened(ee));
      }
    } catch (Exception e) {
      issueStore.add(new ElevationProfileFailure(ee, e.getMessage()));
    }
  }

  /**
   * Method for retrieving the elevation at a given Coordinate.
   *
   * @param coverage the specific Coverage instance to use in order to avoid competition between
   *                 threads
   * @param c        the coordinate (NAD83)
   * @return elevation in meters
   */
  private double getElevation(Coverage coverage, Coordinate c) throws ElevationLookupException {
    try {
      return getElevation(coverage, c.x, c.y);
    } catch (
      ArrayIndexOutOfBoundsException | PointOutsideCoverageException | TransformException e
    ) {
      // Each of the above exceptions can occur when finding the elevation at a coordinate.
      // - The ArrayIndexOutOfBoundsException seems to occur at the edges of some elevation tiles that
      //     might have areas with NoData. See https://github.com/opentripplanner/OpenTripPlanner/issues/2792
      // - The PointOutsideCoverageException can be thrown for points that are outside of the elevation tile area.
      // - The TransformException can occur when trying to compute the EllipsoidToGeoidDifference.
      throw new ElevationLookupException(e);
    }
  }

  /**
   * Method for retrieving the elevation at a given (x, y) pair.
   *
   * @param coverage the specific Coverage instance to use in order to avoid competition between
   *                 threads
   * @param x        the query longitude (NAD83)
   * @param y        the query latitude (NAD83)
   * @return elevation in meters
   */
  private double getElevation(Coverage coverage, double x, double y)
    throws PointOutsideCoverageException, TransformException {
    double[] values = new double[1];
    try {
      // We specify a CRS here because otherwise the coordinates are assumed to be in the coverage's native CRS.
      // That assumption is fine when the coverage happens to be in longitude-first WGS84 but we want to support
      // GeoTIFFs in various projections. Note that GeoTools defaults to strict EPSG axis ordering of (lat, long)
      // for DefaultGeographicCRS.WGS84, but OTP is using (long, lat) throughout and assumes unprojected DEM
      // rasters to also use (long, lat).
      coverage.evaluate(new Position2D(WGS84_XY, x, y), values);
    } catch (PointOutsideCoverageException e) {
      nPointsOutsideDEM.incrementAndGet();
      throw e;
    }

    var elevation =
      (values[0] * gridCoverageFactory.elevationUnitMultiplier()) -
      (includeEllipsoidToGeoidDifference ? getApproximateEllipsoidToGeoidDifference(y, x) : 0);

    minElevation = Math.min(minElevation, elevation);
    maxElevation = Math.max(maxElevation, elevation);

    nPointsEvaluated.incrementAndGet();

    return elevation;
  }

  /**
   * The Calculation of the EllipsoidToGeoidDifference is a very expensive operation, so the
   * resulting values are cached based on the coordinate values up to 2 significant digits. Two
   * significant digits are often more than enough for most parts of the world, but is useful for
   * certain areas that have dramatic changes. Since the values are computed once and cached, it has
   * almost no affect on performance to have this level of detail. See this image for an approximate
   * mapping of these difference values: https://earth-info.nga.mil/GandG/images/ww15mgh2.gif
   *
   * @param y latitude
   * @param x longitude
   */
  private double getApproximateEllipsoidToGeoidDifference(double y, double x)
    throws TransformException {
    int geoidDifferenceCoordinateValueMultiplier = 100;
    int xVal = IntUtils.round(x * geoidDifferenceCoordinateValueMultiplier);
    int yVal = IntUtils.round(y * geoidDifferenceCoordinateValueMultiplier);
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

  /**
   * A custom exception wrapper for all known elevation lookup exceptions
   */
  static class ElevationLookupException extends Exception {

    public ElevationLookupException(Exception e) {
      super(e);
    }
  }
}
