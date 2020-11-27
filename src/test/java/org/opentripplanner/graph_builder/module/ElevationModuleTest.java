package org.opentripplanner.graph_builder.module;

import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.module.ned.DegreeGridNEDTileSource;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.NEDGridCoverageFactoryImpl;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetWithElevationEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.OsmVertex;

import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ElevationModuleTest {

    /**
     * Tests whether elevation can be properly set using s3 bucket tiles. This test is ignored by default because it
     * would require checking in a file to this repository that is about 450mb large.
     *
     * To setup this test, copy everything in the /var/otp/cache/ned into
     * .../src/test/resources/org/opentripplanner/graph_builder/module/ned/
     */
    @Test
    @Ignore
    public void testSetElevationOnEdgesUsingS3BucketTiles() {
        // create a graph with a StreetWithElevationEdge
        Graph graph = new Graph();
        OsmVertex from = new OsmVertex(graph, "from", -122.6932051, 45.5122964, 40513757);
        OsmVertex to = new OsmVertex(graph, "to", -122.6903532, 45.5115309, 1677595882);
        LineString geometry = GeometryUtils.makeLineString(
            -122.6932051,
            45.5122964,
            -122.6931118,
            45.5122687,
            -122.692677,
            45.512204,
            -122.692443,
            45.512142,
            -122.6923995,
            45.5121229,
            -122.692359,
            45.512096,
            -122.692294,
            45.512024,
            -122.692219,
            45.511961,
            -122.69212,
            45.511908,
            -122.6920612,
            45.5118889,
            -122.6920047,
            45.5118749,
            -122.6919636,
            45.5118711,
            -122.6918977,
            45.5118684,
            -122.6918211,
            45.5118662,
            -122.6917612,
            45.5118599,
            -122.6916928,
            45.5118503,
            -122.691241,
            45.511774,
            -122.6903532,
            45.5115309
        );
        Coordinate[] coordinates = geometry.getCoordinates();
        double length = 0;
        for (int i = 1; i < coordinates.length; ++i) {
            length += SphericalDistanceLibrary.distance(coordinates[i - 1], coordinates[i]);
        }
        StreetWithElevationEdge edge = new StreetWithElevationEdge(
            from,
            to,
            geometry,
            "Southwest College St",
            length,
            StreetTraversalPermission.ALL,
            false
        );

        // create the elevation module
        File cacheDirectory = new File(ElevationModuleTest.class.getResource("ned").getFile());
        DegreeGridNEDTileSource awsTileSource = new DegreeGridNEDTileSource();
        NEDGridCoverageFactoryImpl gcf = new NEDGridCoverageFactoryImpl(cacheDirectory, awsTileSource);
        ElevationModule elevationModule = new ElevationModule(gcf);

        // build to graph to execute the elevation module
        elevationModule.checkInputs();
        elevationModule.buildGraph(graph, new HashMap<>());

        // verify that elevation data has been set on the StreetWithElevationEdge
        assertNotNull(edge.getElevationProfile());
        assertEquals(133.95, edge.getElevationProfile().getY(1), 0.1);
    }
}
