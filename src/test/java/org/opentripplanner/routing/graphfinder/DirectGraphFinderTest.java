package org.opentripplanner.routing.graphfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
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
        var ns1 = new NearbyStop(S1, 0, null, linestring(47.500, 19.000, 47.500, 19.000), null);
        var ns2 = new NearbyStop(S2, 1112, null, linestring(47.500, 19.000, 47.510, 19.000), null);

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

    static LineString linestring(double ... latlon) {
        if (latlon.length % 2 != 0) {
            throw new IllegalArgumentException("Uneven number of parameters: " + Arrays.toString(latlon));
        }

        return geometryFactory.createLineString(
                IntStream.range(0, latlon.length / 2)
                .mapToObj(i -> new Coordinate(latlon[i * 2 + 1], latlon[i * 2]))
                .toArray(Coordinate[]::new)
        );
    }
}