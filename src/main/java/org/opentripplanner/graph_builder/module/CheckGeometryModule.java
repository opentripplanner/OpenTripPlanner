package org.opentripplanner.graph_builder.module;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.BogusEdgeGeometry;
import org.opentripplanner.graph_builder.issues.BogusVertexGeometry;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

// TODO OTP2 This test might be obsolete now that there is no HopEdge anymore
/**
 * Check the geometry of every edge in the graph for any bogus geometry --
 * that is, geometry with coordinates of NaN.
 * This is mainly good for debugging, but probably worth keeping on for production
 * because the cost is small compared to the pain of debugging.
 */
public class CheckGeometryModule implements GraphBuilderModule {

    /** An set of ids which identifies what stages this graph builder provides (i.e. streets, elevation, transit) */
    public List<String> provides() {
        return Collections.emptyList();
    }

    /** A list of ids of stages which must be provided before this stage */
    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(CheckGeometryModule.class);
    private static final double MAX_VERTEX_SHAPE_ERROR = 150;

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {
        for (Vertex gv : graph.getVertices()) {
            if (Double.isNaN(gv.getCoordinate().x) || Double.isNaN(gv.getCoordinate().y)) {
                LOG.warn("Vertex " + gv + " has NaN location; this will cause doom.");
                issueStore.add(new BogusVertexGeometry(gv));
            }
            
            // TODO: This was filtered to EdgeNarratives before EdgeNarrative removal
            for (Edge e : gv.getOutgoing()) {
                Geometry g = e.getGeometry();
                if (g == null) {
                    continue;
                }
                for (Coordinate c : g.getCoordinates()) {
                    if (Double.isNaN(c.x) || Double.isNaN(c.y)) {
                        issueStore.add(new BogusEdgeGeometry(e));
                    }
                }
                /*
                if (e instanceof HopEdge) {
                    Coordinate edgeStartCoord = e.getFromVertex().getCoordinate();
                    Coordinate edgeEndCoord = e.getToVertex().getCoordinate();
                    Coordinate[] geometryCoordinates = g.getCoordinates();
                    if (geometryCoordinates.length < 2) {
                        issueStore.add(new BogusEdgeGeometry(e));
                        continue;
                    }
                    Coordinate geometryStartCoord = geometryCoordinates[0];
                    Coordinate geometryEndCoord = geometryCoordinates[geometryCoordinates.length - 1];
                    if (SphericalDistanceLibrary.distance(edgeStartCoord, geometryStartCoord) > MAX_VERTEX_SHAPE_ERROR) {
                        issueStore.add(new VertexShapeError(e));
                    } else if (SphericalDistanceLibrary.distance(edgeEndCoord, geometryEndCoord) > MAX_VERTEX_SHAPE_ERROR) {
                        issueStore.add(new VertexShapeError(e));
                    }
                }
                 */
            }
        }

    }

    @Override
    public void checkInputs() {
        //no inputs to check
    }

}
