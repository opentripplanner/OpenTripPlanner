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

package org.opentripplanner.graph_builder.impl.ned;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.media.jai.InterpolationBilinear;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.coverage.Coverage;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.ned.NEDGridCoverageFactory;
import org.opentripplanner.routing.core.GraphBuilderAnnotation;
import org.opentripplanner.routing.core.GraphBuilderAnnotation.Variety;
import org.opentripplanner.routing.edgetype.EdgeWithElevation;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * {@link GraphBuilder} plugin that takes a constructed (@link Graph} and overlays it onto National
 * Elevation Dataset (NED) raster data, creating elevation profiles for each Street encountered in
 * the Graph. The elevation profile is stored as a {@link PackedCoordinateSequence}, where each
 * (x,y) pair represents one sample, with the x-coord representing the distance along the edge as,
 * measured from the start, and the y-coord representing the sampled elevation at that point (both
 * in meters).
 * 
 * @author demory, novalis (missing elevation interp)
 * 
 */
public class NEDGraphBuilderImpl implements GraphBuilder {
    private static final Logger log = LoggerFactory.getLogger(NEDGraphBuilderImpl.class);

    private NEDGridCoverageFactory gridCoverageFactory;

    private Coverage coverage;

    /**
     * The distance between samples in meters. Defaults to 10m, the approximate resolution of 1/3
     * arc-second NED data.
     */
    private double distanceBetweenSamplesM = 10;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    public List<String> provides() {
        return Arrays.asList("elevation");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }
    
    public void setGridCoverageFactory(NEDGridCoverageFactory factory) {
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

        List<EdgeWithElevation> edgesWithElevation = new ArrayList<EdgeWithElevation>();
        int nProcessed = 0;
        int nTotal = graph.countEdges();
        for (Vertex gv : graph.getVertices()) {
            for (Edge ee : gv.getOutgoing()) {
                if (ee instanceof EdgeWithElevation) {
                    EdgeWithElevation edgeWithElevation = (EdgeWithElevation) ee;
                    // if (ee instanceof TurnEdge && ((TurnVertex)ee.getFromVertex()).is
                    processEdge(graph, edgeWithElevation);
                    if (edgeWithElevation.getElevationProfile() != null && !edgeWithElevation.isElevationFlattened()) {
                        edgesWithElevation.add(edgeWithElevation);
                    }
                    nProcessed += 1;
                    if (nProcessed % 50000 == 0)
                        log.info("set elevation on {}/{} edges", nProcessed, nTotal);
                }
            }
        }

        assignMissingElevations(graph, edgesWithElevation);
    }

    class ElevationRepairState {
        /* This uses an intuitionist approach to elevation inspection */
        public EdgeWithElevation backEdge;

        public ElevationRepairState backState;

        public Vertex vertex;

        public double distance;

        public double initialElevation;

        public ElevationRepairState(EdgeWithElevation backEdge, ElevationRepairState backState,
                Vertex vertex, double distance, double initialElevation) {
            this.backEdge = backEdge;
            this.backState = backState;
            this.vertex = vertex;
            this.distance = distance;
            this.initialElevation = initialElevation;
        }
    }

    /**
     * 
     */
    private void assignMissingElevations(Graph graph, List<EdgeWithElevation> edgesWithElevation) {

        log.debug("Assigning missing elevations");

        BinHeap<ElevationRepairState> pq = new BinHeap<ElevationRepairState>();
        BinHeap<ElevationRepairState> secondary_pq = new BinHeap<ElevationRepairState>();

        // elevation for each vertex (known or interpolated)
        HashMap<Vertex, Double> elevations = new HashMap<Vertex, Double>();

        // initialize queue with all vertices which already have known elevation
        for (EdgeWithElevation e : edgesWithElevation) {
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
            double key = pq.peek_min_key();
            ElevationRepairState state = pq.extract_min();

            if (pq.empty() && secondary_pq != null) {
                pq = secondary_pq;
                secondary_pq = null;
            }

            if (key != 0 && elevations.containsKey(state.vertex)) {
                // we have already explored this vertex; we might need to do something here
                // but for now let's not.
                continue;
            }

            ElevationRepairState curState = state;
            Vertex initialVertex = null;
            while (curState != null) {
                initialVertex = curState.vertex;
                curState = curState.backState;
            }

            double bestDistance = Double.MAX_VALUE;
            double bestElevation = 0;
            for (Edge e : state.vertex.getOutgoing()) {
                if (!(e instanceof EdgeWithElevation)) {
                    continue;
                }
                EdgeWithElevation edge = (EdgeWithElevation) e;
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
                if (!(e instanceof EdgeWithElevation)) {
                    continue;
                }
                EdgeWithElevation edge = (EdgeWithElevation) e;
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
                if (e instanceof EdgeWithElevation) {
                    EdgeWithElevation edge = ((EdgeWithElevation) e);

                    Double toElevation = elevations.get(edge.getToVertex());

                    if (fromElevation == null || toElevation == null) {
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

                    if(edge.setElevationProfile(profile, true)) {
                        log.trace(GraphBuilderAnnotation.register(graph, Variety.ELEVATION_FLATTENED, edge));
                    }
                }
            }
        }
    }

    /**
     * Processes a single {@link Street} edge, creating and assigning the elevation profile.
     * 
     * @param ee the street edge
     * @param graph the graph (used only for error handling)
     */
    private void processEdge(Graph graph, EdgeWithElevation ee) {
        if (ee.getElevationProfile() != null) {
            return; /* already set up */
        }
        Geometry g = ee.getGeometry();
        Coordinate[] coords = g.getCoordinates();

        List<Coordinate> coordList = new LinkedList<Coordinate>();

        // calculate the total edge length in meters
        double edgeLenM = 0;
        for (int i = 0; i < coords.length - 1; i++) {
            edgeLenM += distanceLibrary.distance(coords[i].y, coords[i].x, coords[i + 1].y,
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
            log.trace(GraphBuilderAnnotation.register(graph, Variety.ELEVATION_FLATTENED, ee));
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
            double pct = distanceLibrary .distance(y1, x1, y2, x2) / length;

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

        double pct = distanceLibrary.distance(y1, x1, y2, x2) / length;
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
            coverage.evaluate(new DirectPosition2D(x, y), values);
        } catch (org.opengis.coverage.PointOutsideCoverageException e) {
            // skip this for now
        }
        return values[0];
    }

    @Override
    public void checkInputs() {
        gridCoverageFactory.checkInputs();
    }

}