package org.opentripplanner.graph_builder.linking;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import static org.junit.Assert.assertEquals;

public class EdgesMakerTest {

    private final EdgesMaker edgesMaker = new EdgesMaker();

    private TemporaryStreetLocation origin;
    private TemporaryStreetLocation destination;
    private TemporaryRentVehicleVertex rentVehicleVertex;
    private StreetVertex to;
    private TransitStop transitStop;

    @Before
    public void setUp() {
        Coordinate coordinate = new Coordinate(0, 1);
        origin = new TemporaryStreetLocation("id1", coordinate, null, false);
        destination = new TemporaryStreetLocation("id2", coordinate, null, true);
        to = new StreetLocation("id4", coordinate, "name");
        rentVehicleVertex = new TemporaryRentVehicleVertex("id3", coordinate, "name");

        Graph graph = new Graph();
        Stop stop = new Stop();
        stop.setId(new FeedScopedId("TEST", "TEST"));
        stop.setName("TEST");
        stop.setLon(1);
        stop.setLat(1);
        transitStop = new TransitStop(graph, stop);
    }

    @Test
    public void shouldCreateTemporaryEdgeFromOrigin() {
        // when
        edgesMaker.makeTemporaryEdges(origin, to);

        // then
        assertEquals(1, origin.getOutgoing().size());
        assertEquals(1, to.getIncoming().size());
        assertEquals(origin.getOutgoing(), to.getIncoming());
    }

    @Test
    public void shouldCreateTemporaryEdgeToDestination() {
        // when
        edgesMaker.makeTemporaryEdges(destination, to);

        // then
        assertEquals(1, destination.getIncoming().size());
        assertEquals(1, to.getOutgoing().size());
        assertEquals(destination.getIncoming(), to.getOutgoing());
    }

    @Test
    public void shouldCreatePermanentEdgesToAndFromTransitStop() {
        // when
        edgesMaker.makePermanentEdges(transitStop, to);

        // then
        assertEquals(1, transitStop.getOutgoing().size());
        assertEquals(1, to.getIncoming().size());
        assertEquals(transitStop.getOutgoing(), to.getIncoming());

        assertEquals(1, transitStop.getIncoming().size());
        assertEquals(1, to.getOutgoing().size());
        assertEquals(transitStop.getIncoming(), to.getOutgoing());
    }

    @Test
    public void shouldNotCreatePermanentEdgesForUnknownVertexType() {
        // when
        edgesMaker.makePermanentEdges(origin, to);

        // then
        assertEquals(0, origin.getOutgoing().size());
        assertEquals(0, to.getIncoming().size());

        assertEquals(0, origin.getIncoming().size());
        assertEquals(0, to.getOutgoing().size());
    }

    @Test
    public void shouldNotCreatePermanentEdgesToAndFromTransitStopWhenTheyAlreadyExist() {
        // given
        edgesMaker.makePermanentEdges(transitStop, to);

        // when
        edgesMaker.makePermanentEdges(transitStop, to);

        // then
        assertEquals(1, transitStop.getOutgoing().size());
        assertEquals(1, to.getIncoming().size());
        assertEquals(transitStop.getOutgoing(), to.getIncoming());

        assertEquals(1, transitStop.getIncoming().size());
        assertEquals(1, to.getOutgoing().size());
        assertEquals(transitStop.getIncoming(), to.getOutgoing());
    }

    @Test
    public void shouldCreateTemporaryEdgesBothWays() {
        // given

        // when
        edgesMaker.makeTemporaryEdgesBothWays(rentVehicleVertex, to);

        // then
        assertEquals(1, rentVehicleVertex.getOutgoing().size());
        assertEquals(1, to.getIncoming().size());
        assertEquals(rentVehicleVertex.getOutgoing(), to.getIncoming());

        assertEquals(1, rentVehicleVertex.getIncoming().size());
        assertEquals(1, to.getIncoming().size());
        assertEquals(rentVehicleVertex.getIncoming(), to.getOutgoing());
    }
}
