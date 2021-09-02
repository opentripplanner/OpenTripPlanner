package org.opentripplanner.routing.graphfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

class DirectGraphFinderTest extends GraphRoutingTest {

    private static GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    private Graph graph;

    private TransitStopVertex S1, S2, S3;

    @BeforeEach
    protected void setUp() throws Exception {
        graph = graphOf(new Builder() {
            @Override
            public void build() {
                S1 = stop("S1", 47.500, 19);
                S2 = stop("S2", 47.510, 19);
                S3 = stop("S3", 47.520, 19);
            }
        });
    }

    @Test
    void findClosestStops() {
        var ns1 = new NearbyStop(S1.getStop(), 0, null, null, null);
        var ns2 = new NearbyStop(S2.getStop(), 1112, null, null, null);

        var testee = new DirectGraphFinder(graph);
        assertEquals(
                List.of(ns1),
                testee.findClosestStops(47.500, 19.000, 100)
        );

        assertEquals(
                List.of(ns1, ns2),
                testee.findClosestStops(47.500, 19.000, 2000)
        );
    }
}