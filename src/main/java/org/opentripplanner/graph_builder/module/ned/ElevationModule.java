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

package org.opentripplanner.graph_builder.module.ned;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.coverage.Coverage;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.graph_builder.annotation.ElevationFlattened;
import org.opentripplanner.graph_builder.module.extra_elevation_data.ElevationPoint;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetWithElevationEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.InterpolationBilinear;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} plugin that applies elevation data to street data that has already
 * been loaded into a (@link Graph}, creating elevation profiles for each Street encountered
 * in the Graph. Depending on the {@link ElevationGridCoverageFactory} specified
 * this could be auto-downloaded and cached National Elevation Dataset (NED) raster data or
 * a GeoTIFF file. The elevation profiles are stored as {@link PackedCoordinateSequence} objects,
 * where each (x,y) pair represents one sample, with the x-coord representing the distance along
 * the edge measured from the start, and the y-coord representing the sampled elevation at that
 * point (both in meters).
 */
public class ElevationModule implements GraphBuilderModule {

    private static final Logger log = LoggerFactory.getLogger(ElevationModule.class);

    private ElevationGridCoverageFactory gridCoverageFactory;

    private Coverage coverage;

    // Keep track of the proportion of elevation fetch operations that fail so we can issue warnings.
    private int nPointsEvaluated = 0;
    private int nPointsOutsideDEM = 0;

    /**
     * The distance between samples in meters. Defaults to 10m, the approximate resolution of 1/3
     * arc-second NED data.
     */
    private double distanceBetweenSamplesM = 10;

    public ElevationModule() { /* This makes me a "bean" */ };
    
    public ElevationModule(ElevationGridCoverageFactory factory) {
        this.setGridCoverageFactory(factory);
    }

    public List<String> provides() {
        return Arrays.asList("elevation");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }
    
    public void setGridCoverageFactory(ElevationGridCoverageFactory factory) {
        gridCoverageFactory = factory;
    }

    public void setDistanceBetweenSamplesM(double distance) {
        distanceBetweenSamplesM = distance;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        gridCoverageFactory.setGraph(graph);
        Coverage gridCov = gridCoverageFactory.getGridCoverage();

        // If gridCov is a GridCoverage2D, apply a bilinear interpolator. Otherwise, just use the
        // coverage as is (note: UnifiedGridCoverages created by NEDGridCoverageFactoryImpl handle
        // interpolation internally)
        coverage = (gridCov instanceof GridCoverage2D) ? Interpolator2D.create(
                (GridCoverage2D) gridCov, new InterpolationBilinear()) : gridCov;
        log.info("Setting street elevation profiles from digital elevation model...");
        List<StreetEdge> edgesWithElevation = new ArrayList<StreetEdge>();
        int nProcessed = 0;
        int nTotal = graph.countEdges();
        for (Vertex gv : graph.getVertices()) {
            for (Edge ee : gv.getOutgoing()) {
                if (ee instanceof StreetWithElevationEdge) {
                    StreetWithElevationEdge edgeWithElevation = (StreetWithElevationEdge) ee;
                    processEdge(graph, edgeWithElevation);
                    if (edgeWithElevation.getElevationProfile() != null && !edgeWithElevation.isElevationFlattened()) {
                        edgesWithElevation.add(edgeWithElevation);
                    }
                    nProcessed += 1;
                    if (nProcessed % 50000 == 0) {
                        log.info("set elevation on {}/{} edges", nProcessed, nTotal);
                        double failurePercentage = nPointsOutsideDEM / nPointsEvaluated * 100;
                        if (failurePercentage > 50) {
                            log.warn("Fetching elevation failed at {}/{} points ({}%)",
                                    nPointsOutsideDEM, nPointsEvaluated, failurePercentage);
                            log.warn("Elevation is missing at a large number of points. DEM may be for the wrong region. " +
                                    "If it is unprojected, perhaps the axes are not in (longitude, latitude) order.");
                        }
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        HashMap<Vertex, Double> extraElevation = (HashMap<Vertex, Double>) extra.get(ElevationPoint.class);
        assignMissingElevations(graph, edgesWithElevation, extraElevation);
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
     * Processes a single street edge, creating and assigning the elevation profile.
     * 
     * @param ee the street edge
     * @param graph the graph (used only for error handling)
     */
    private void processEdge(Graph graph, StreetWithElevationEdge ee) {
        if (ee.getElevationProfile() != null) {
            return; /* already set up */
        }
        Geometry g = ee.getGeometry();
        Coordinate[] coords = g.getCoordinates();

        List<Coordinate> coordList = new LinkedList<Coordinate>();

        // calculate the total edge length in meters
        double edgeLenM = 0;
        for (int i = 0; i < coords.length - 1; i++) {
            edgeLenM += SphericalDistanceLibrary.distance(coords[i].y, coords[i].x, coords[i + 1].y,
                    coords[i + 1].x);
        }

        // initial sample (x = 0)
        coordList.add(new Coordinate(0, getElevation(coords[0])));

        // loop for edge-internal samples
        for (double x = distanceBetweenSamplesM; x < edgeLenM; x += distanceBetweenSamplesM) {
            // avoid final-segment samples less than half the distance between samples:
            if (edgeLenM - x < distanceBetweenSamplesM / 2) {
                break;
            }

            Coordinate internal = getPointAlongEdge(coords, edgeLenM, x / edgeLenM);
            coordList.add(new Coordinate(x, getElevation(internal)));
        }

        // final sample (x = edge length)
        coordList.add(new Coordinate(edgeLenM, getElevation(coords[coords.length - 1])));

        // construct the PCS
        Coordinate coordArr[] = new Coordinate[coordList.size()];
        PackedCoordinateSequence elevPCS = new PackedCoordinateSequence.Double(
                coordList.toArray(coordArr));

        if(ee.setElevationProfile(elevPCS, false)) {
            log.trace(graph.addBuilderAnnotation(new ElevationFlattened(ee)));
        }
    }

    /**
     * Returns a coordinate along a path located at a specific point indicated by the percentage of
     * distance covered from start to end.
     * 
     * @param coords the list of (x,y) coordinates that form the path
     * @param length the total length of the path
     * @param t the percentage (ranges from 0 to 1)
     * @return the (x,y) coordinate at t
     */
    public Coordinate getPointAlongEdge(Coordinate[] coords, double length, double t) {

        double pctThrough = 0; // current percentage of the edge length traversed

        // endpoints of current segment within edge:
        double x1 = coords[0].x, y1 = coords[0].y, x2, y2;

        for (int i = 1; i < coords.length - 1; i++) { // loop through inner points
            Coordinate innerPt = coords[i];
            x2 = innerPt.x;
            y2 = innerPt.y;

            // percentage of total edge length represented by current segment:
            double pct = SphericalDistanceLibrary.distance(y1, x1, y2, x2) / length;

            if (pctThrough + pct > t) { // if current segment contains 't,' we're done
                double pctAlongSeg = (t - pctThrough) / pct;
                return new Coordinate(x1 + (pctAlongSeg * (x2 - x1)), y1
                        + (pctAlongSeg * (y2 - y1)));
            }

            pctThrough += pct;
            x1 = x2;
            y1 = y2;
        }

        // handle the final segment separately
        x2 = coords[coords.length - 1].x;
        y2 = coords[coords.length - 1].y;

        double pct = SphericalDistanceLibrary.distance(y1, x1, y2, x2) / length;
        double pctAlongSeg = (t - pctThrough) / pct;

        return new Coordinate(x1 + (pctAlongSeg * (x2 - x1)), y1 + (pctAlongSeg * (y2 - y1)));
    }

    /**
     * Method for retrieving the elevation at a given Coordinate.
     * 
     * @param c the coordinate (NAD83)
     * @return elevation in meters
     */
    private double getElevation(Coordinate c) {
        return getElevation(c.x, c.y);
    }

    /**
     * Method for retrieving the elevation at a given (x, y) pair.
     * 
     * @param x the query longitude (NAD83)
     * @param y the query latitude (NAD83)
     * @return elevation in meters
     */
    private double getElevation(double x, double y) {
        double values[] = new double[1];
        try {
            // We specify a CRS here because otherwise the coordinates are assumed to be in the coverage's native CRS.
            // That assumption is fine when the coverage happens to be in longitude-first WGS84 but we want to support
            // GeoTIFFs in various projections. Note that GeoTools defaults to strict EPSG axis ordering of (lat, long)
            // for DefaultGeographicCRS.WGS84, but OTP is using (long, lat) throughout and assumes unprojected DEM
            // rasters to also use (long, lat).
            coverage.evaluate(new DirectPosition2D(GeometryUtils.WGS84_XY, x, y), values);
        } catch (org.opengis.coverage.PointOutsideCoverageException e) {
            nPointsOutsideDEM += 1;
        }
        nPointsEvaluated += 1;
        return values[0];
    }

    @Override
    public void checkInputs() {
        gridCoverageFactory.checkInputs();
    }

}