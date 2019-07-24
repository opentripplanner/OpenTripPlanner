package org.opentripplanner.graph_builder.module;

import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopStreetVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public abstract class AbstractVertexConnectorTest {
	
	protected static final String DEFAULT_STOP_CODE          = "123";
	protected static final String PREFIXLESS_STOP_CODE       = "321";
	protected static final String PREFIXED_STOP_CODE         = "H" + PREFIXLESS_STOP_CODE;
	protected static final String PREFIXED_DEFAULT_STOP_CODE = "H" + DEFAULT_STOP_CODE;
	
	protected TransitStopStreetVertex transitVertex;
	protected TransitStopStreetVertex prefixedTransitVertex;
	protected OsmVertex               nonTransitVertex;
	protected List<Vertex>            vertices;
	protected Graph                   g;
	
	@Before
	public void setUp() throws Exception {
		g = Mockito.mock(Graph.class);
		transitVertex = new TransitStopStreetVertex(g, "label1", 23.03, 42.03, 1, "WithoutPrefix", DEFAULT_STOP_CODE);
		prefixedTransitVertex = new TransitStopStreetVertex(g, "label2", 23.13, 42.13, 1, "WithPrefix", PREFIXED_STOP_CODE);
		nonTransitVertex = new OsmVertex(g, "Nonstreettransitvertex", 23, 24, 100L);
		
		vertices = new ArrayList<>();
		vertices.add(transitVertex);
		vertices.add(prefixedTransitVertex);
		vertices.add(nonTransitVertex);
	}
	
	@After
	public void tearDown() throws Exception {
	}
	
	protected static void assertBidirectionalEdges(Vertex from, Vertex to) {
		assertOutgoingEdge(from, to);
		assertOutgoingEdge(to, from);
		assertIncomingEdge(from, to);
		assertIncomingEdge(to, from);
	}
	
	protected static void assertOutgoingEdge(Vertex from, Vertex to) {
		Optional<Edge> optionalEdge = from.getOutgoing().stream().findFirst();
		assertTrue(optionalEdge.isPresent());
		assertSame(to, optionalEdge.get().getToVertex());
	}
	
	protected static void assertIncomingEdge(Vertex to, Vertex from) {
		Optional<Edge> optionalEdge = to.getIncoming().stream().findFirst();
		assertTrue(optionalEdge.isPresent());
		assertSame(from, optionalEdge.get().getFromVertex());
	}
	
	protected static void assertNoEdges(Vertex vertex) {
		assertTrue(vertex.getOutgoing().isEmpty());
		assertTrue(vertex.getIncoming().isEmpty());
	}
	
	protected TransitStop createTransitStop(String code) {
		Stop stop = new Stop();
		stop.setId(new FeedScopedId());
		stop.setCode(code);
		return new TransitStop(g, stop);
	}
	
	protected TransitStop createTransitStop() {
		return createTransitStop(DEFAULT_STOP_CODE);
	}
	
	protected TransitStop createPrefixedTransitStop() {
		return createTransitStop(PREFIXED_DEFAULT_STOP_CODE);
	}
	
	protected TransitStop createPrefixMatchedTransitStop() {
		return createTransitStop(PREFIXLESS_STOP_CODE);
	}
	
	protected TransitStop createUnmatchedTransitStop() {
		return createTransitStop("1234");
	}
	
}
