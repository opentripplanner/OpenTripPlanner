package org.opentripplanner.routing.vertextype;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.NonLocalizedString;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class StreetVertexTest {

    private static final LineString GEOMETRY = GeometryUtils.makeLineString(0, 1, 1, 2);

    private StreetVertex streetVertex, otherVertex;

    @Before
    public void setUp() {
        Graph graph = new Graph();
        streetVertex = new SimpleStreetVertex(graph, "label", new Coordinate(0, 1), new NonLocalizedString("name"));
        otherVertex = new SimpleStreetVertex(graph, "label2", new Coordinate(1, 2), new NonLocalizedString("name2"));
    }

    @Test
    public void shouldReturnUnnamedStreetIfNoEdgesArePresent() {
        // then
        assertEquals(new LocalizedString("unnamedStreet", (String[]) null), streetVertex.getIntersectionName(Locale.ENGLISH));
    }

    @Test
    public void shouldReturnProperStreetName() {
        // given
        StreetEdge someStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "Some Street", 100, StreetTraversalPermission.CAR, false);
        someStreet.setHasBogusName(false);

        // then
        assertEquals(new NonLocalizedString("Some Street"), streetVertex.getIntersectionName(Locale.ENGLISH));
    }

    @Test
    public void shouldReturnProperCornerName() {
        // given
        StreetEdge someStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "Some Street", 100, StreetTraversalPermission.CAR, false);
        someStreet.setHasBogusName(false);
        StreetEdge otherStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "Other Street", 100, StreetTraversalPermission.CAR, false);
        otherStreet.setHasBogusName(false);

        // then
        assertEquals(new LocalizedString("corner", new String[]{"Some Street", "Other Street"}), streetVertex.getIntersectionName(Locale.ENGLISH));
    }

    @Test
    public void shouldIgnoreBogusNameIfHasAProperOne() {
        // given
        StreetEdge someStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "Some Street", 100, StreetTraversalPermission.CAR, false);
        someStreet.setHasBogusName(false);
        StreetEdge otherStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "placeholder name", 100, StreetTraversalPermission.CAR, false);
        otherStreet.setHasBogusName(true);

        // then
        assertEquals(new NonLocalizedString("Some Street"), streetVertex.getIntersectionName(Locale.ENGLISH));
    }

    @Test
    public void shouldIgnoreAdditionalStreetNames() {
        // given
        StreetEdge someStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "Some Street", 100, StreetTraversalPermission.CAR, false);
        someStreet.setHasBogusName(false);
        StreetEdge otherStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "Other Street", 100, StreetTraversalPermission.CAR, false);
        otherStreet.setHasBogusName(false);
        StreetEdge thirdStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "Third Street", 100, StreetTraversalPermission.CAR, false);
        thirdStreet.setHasBogusName(false);

        // then
        assertEquals(new LocalizedString("corner", new String[]{"Some Street", "Other Street"}), streetVertex.getIntersectionName(Locale.ENGLISH));
    }

    @Test
    public void shouldIgnoreDuplicateStreetNames() {
        // given
        StreetEdge someStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "Some Street", 100, StreetTraversalPermission.CAR, false);
        someStreet.setHasBogusName(false);
        StreetEdge someStreet2 = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "Some Street", 100, StreetTraversalPermission.CAR, false);
        someStreet2.setHasBogusName(false);

        // then
        assertEquals(new NonLocalizedString("Some Street"), streetVertex.getIntersectionName(Locale.ENGLISH));
    }

    @Test
    public void shouldUseBogusNameWhenNoProperArePresent() {
        // given
        StreetEdge someStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "placeholder", 100, StreetTraversalPermission.CAR, false);
        someStreet.setHasBogusName(true);

        // then
        assertEquals(new NonLocalizedString("placeholder"), streetVertex.getIntersectionName(Locale.ENGLISH));
    }

    @Test
    public void shouldIgnoreAdditionalBogusNames() {
        // given
        StreetEdge someStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "placeholder 1", 100, StreetTraversalPermission.CAR, false);
        someStreet.setHasBogusName(true);
        StreetEdge otherStreet = new StreetEdge(streetVertex, otherVertex, GEOMETRY, "placeholder 2", 100, StreetTraversalPermission.CAR, false);
        otherStreet.setHasBogusName(true);

        // then
        assertEquals(new NonLocalizedString("placeholder 1"), streetVertex.getIntersectionName(Locale.ENGLISH));
    }

    private static class SimpleStreetVertex extends StreetVertex {

        public SimpleStreetVertex(Graph g, String label, Coordinate coord, I18NString streetName) {
            super(g, label, coord, streetName);
        }
    }
}
