package org.opentripplanner.graph_builder.linking;


import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.Stop;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class LinkStopToPlatformTest {

    private static GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    private Graph graph;

    @Before
    public void before() {

        // Set up transit platform

        graph = new Graph();

        ArrayList<IntersectionVertex> vertices = new ArrayList<IntersectionVertex>();

        vertices.add(new IntersectionVertex(graph, "1", 10.22054, 59.13568, "Platform vertex 1"));
        vertices.add(new IntersectionVertex(graph, "2", 10.22432, 59.13519, "Platform vertex 2"));
        vertices.add(new IntersectionVertex(graph, "3", 10.22492, 59.13514, "Platform vertex 3"));
        vertices.add(new IntersectionVertex(graph, "4", 10.22493, 59.13518, "Platform vertex 4"));
        vertices.add(new IntersectionVertex(graph, "5", 10.22056, 59.13575, "Platform vertex 5"));

        AreaEdgeList areaEdgeList = new AreaEdgeList();

        ArrayList<AreaEdge> edges = new ArrayList<AreaEdge>();

        edges.add(createAreaEdge(vertices.get(0), vertices.get(1), areaEdgeList, "edge 1"));
        edges.add(createAreaEdge(vertices.get(1), vertices.get(2), areaEdgeList, "edge 2"));
        edges.add(createAreaEdge(vertices.get(2), vertices.get(3), areaEdgeList, "edge 3"));
        edges.add(createAreaEdge(vertices.get(3), vertices.get(4), areaEdgeList, "edge 4"));
        edges.add(createAreaEdge(vertices.get(4), vertices.get(0), areaEdgeList, "edge 5"));

        edges.add(createAreaEdge(vertices.get(1), vertices.get(0), areaEdgeList, "edge 6"));
        edges.add(createAreaEdge(vertices.get(2), vertices.get(1), areaEdgeList, "edge 7"));
        edges.add(createAreaEdge(vertices.get(3), vertices.get(2), areaEdgeList, "edge 8"));
        edges.add(createAreaEdge(vertices.get(4), vertices.get(3), areaEdgeList, "edge 9"));
        edges.add(createAreaEdge(vertices.get(0), vertices.get(4), areaEdgeList, "edge 10"));

        Stop stop = Stop.stopForTest("TestStop",59.13545, 10.22213);

        TransitStopVertex stopVertex = new TransitStopVertex(graph, stop, null);
    }

    /**
     * Tests that extra edges are added when linking stops to platform areas to prevent detours around the platform.
     */
    @Test
    public void testLinkStopWithoutExtraEdges() {
        SimpleStreetSplitter splitter = new SimpleStreetSplitter(graph);
        splitter.link();

        assertEquals(16, graph.getEdges().size());
    }

    @Test
    public void testLinkStopWithExtraEdges() {
        SimpleStreetSplitter splitter = new SimpleStreetSplitter(graph);
        splitter.setAddExtraEdgesToAreas(true);
        splitter.link();

        assertEquals(38, graph.getEdges().size());
    }

    private AreaEdge createAreaEdge(IntersectionVertex v1, IntersectionVertex v2, AreaEdgeList area, String nameString) {
        LineString line = geometryFactory.createLineString(new Coordinate[] { v1.getCoordinate(), v2.getCoordinate()});
        double length = SphericalDistanceLibrary.distance(v1.getCoordinate(),
                v2.getCoordinate());
        I18NString name = new LocalizedString(nameString, new OSMWithTags());

        return new AreaEdge(v1, v2, line, name, line.getLength(), StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, false, area );
    }
}
