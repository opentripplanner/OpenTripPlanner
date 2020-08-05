package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;

import static org.junit.Assert.*;

public class LinkingGeoToolsTest {

    private final LinkingGeoTools linkingGeoTools = new LinkingGeoTools();

    private StreetVertex someVertex;
    private StreetVertex from;
    private StreetVertex to;
    private StreetEdge edge;

    @Before
    public void setUp() {
        someVertex = new StreetLocation("id1", new Coordinate(0.5, 2), "name");
        from = new StreetLocation("id1", new Coordinate(0, 1), "name");
        to = new StreetLocation("id2", new Coordinate(1, 1), "name");
        edge = new StreetEdge(from, to, GeometryUtils.makeLineString(0, 1, 0.5, 1, 1, 1),
                "S. Crystal Dr", 100, StreetTraversalPermission.PEDESTRIAN, false);
    }

    @Test
    public void shouldFindLocationClosestToVertex() {
        // when
        LinearLocation location = linkingGeoTools.findLocationClosestToVertex(someVertex, edge.getGeometry());

        // then
        assertEquals(new Coordinate(0.5, 1), location.getCoordinate(edge.getGeometry()));
    }

    @Test
    public void shouldProjectDistanceBetweenVertexes() {
        // when
        double distance = linkingGeoTools.distance(from, to);

        // then
        assertEquals(1, distance, 0.1);
    }

    @Test
    public void shouldProjectDistanceFromVertexToOwnEdge() {
        // when
        double distance = linkingGeoTools.distance(from, edge);

        // then
        assertEquals(0, distance, 0.1);
    }

    @Test
    public void shouldProjectDistanceFromVertexToOtherEdge() {
        // when
        double distance = linkingGeoTools.distance(someVertex, edge);

        // then
        assertEquals(1, distance, 0.1);
    }

    @Test
    public void shouldCreateLineString() {
        // given
        LineString lineString = linkingGeoTools.createLineString(from, to);

        // then
        assertEquals(from.getCoordinate(), lineString.getStartPoint().getCoordinate());
        assertEquals(to.getCoordinate(), lineString.getEndPoint().getCoordinate());
        assertEquals(2, lineString.getNumPoints());
    }

    @Test
    public void shouldCreateEnvelope() {
        // given
        Envelope envelope = linkingGeoTools.createEnvelope(someVertex);

        // then
        assertEquals(someVertex.getCoordinate().x, envelope.getMinX(), 0.1);
        assertEquals(someVertex.getCoordinate().x, envelope.getMaxX(), 0.1);
        assertTrue(envelope.getMinX() < envelope.getMaxX());
        assertTrue(envelope.getMinX() < someVertex.getCoordinate().x);
        assertTrue(someVertex.getCoordinate().x < envelope.getMaxX());

        assertEquals(someVertex.getCoordinate().y, envelope.getMinY(), 0.1);
        assertEquals(someVertex.getCoordinate().y, envelope.getMaxY(), 0.1);
        assertTrue(envelope.getMinY() < envelope.getMaxY());
        assertTrue(envelope.getMinY() < someVertex.getCoordinate().y);
        assertTrue(someVertex.getCoordinate().y < envelope.getMaxY());
    }

    @Test
    public void shouldReturnTrueForLocationExactlyAtTheBeginning() {
        // given
        LinearLocation ll = new LinearLocation(0, 0);

        // then
        assertTrue(linkingGeoTools.isLocationAtTheBeginning(ll));
    }

    @Test
    public void shouldReturnTrueForLocationAlmostAtTheBeginning() {
        // given
        LinearLocation ll = new LinearLocation(0, 1e-9);

        // then
        assertTrue(linkingGeoTools.isLocationAtTheBeginning(ll));
    }

    @Test
    public void shouldReturnFalseForLocationNotAtTheBeginning() {
        // given
        LinearLocation ll = new LinearLocation(0, 0.1);

        // then
        assertFalse(linkingGeoTools.isLocationAtTheBeginning(ll));
    }

    @Test
    public void shouldReturnFalseForLocationFarFromTheBeginning() {
        // given
        LinearLocation ll = new LinearLocation(1, 0);

        // then
        assertFalse(linkingGeoTools.isLocationAtTheBeginning(ll));
    }

    @Test
    public void shouldReturnTrueForLocationExactlyAtTheEnd() {
        // given
        LineString lineString = linkingGeoTools.createLineString(from, to);
        LinearLocation ll = new LinearLocation(1, 0);

        // then
        assertTrue(linkingGeoTools.isLocationExactlyAtTheEnd(ll, lineString));
    }

    @Test
    public void shouldReturnFalseForLocationNotExactlyAtTheBeginning() {
        // given
        LineString lineString = linkingGeoTools.createLineString(from, to);
        LinearLocation ll = new LinearLocation(0, 0);

        // then
        assertFalse(linkingGeoTools.isLocationExactlyAtTheEnd(ll, lineString));
    }

    @Test
    public void shouldReturnTrueForLocationAlmostAtTheEnd() {
        // given
        LineString lineString = linkingGeoTools.createLineString(from, to);
        LinearLocation ll = new LinearLocation(0, 1 - 1e-9);

        // then
        assertTrue(linkingGeoTools.isLocationAtTheEnd(ll, lineString));
    }

    @Test
    public void shouldReturnFalseForLocationNotAtTheEnd() {
        // given
        LineString lineString = linkingGeoTools.createLineString(from, to);
        LinearLocation ll = new LinearLocation(0, 0.5);

        // then
        assertFalse(linkingGeoTools.isLocationAtTheEnd(ll, lineString));
    }
}
