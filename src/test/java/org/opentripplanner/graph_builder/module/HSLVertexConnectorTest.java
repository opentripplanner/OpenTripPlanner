package org.opentripplanner.graph_builder.module;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opentripplanner.routing.vertextype.TransitStop;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class HSLVertexConnectorTest extends AbstractVertexConnectorTest {
	
	protected VertexConnector connector = new HSLVertexConnector();
	
	@Test
	public void connectVertexSucceeds() {
		TransitStop transitStop = createTransitStop();
		boolean connected = connector.connectVertex(transitStop,false, vertices);
		assertTrue(connected);
	}
	
	@Test
	public void connectVertexSucceedsWithPrefixedTransitVertex() {
		TransitStop transitStop = createPrefixMatchedTransitStop();
		boolean connected = connector.connectVertex(transitStop,false, vertices);
		assertTrue(connected);
	}
	
	@Test
	public void connectVertexSucceedsWithPrefixedTransitStop() {
		TransitStop transitStop = createPrefixedTransitStop();
		boolean connected = connector.connectVertex(transitStop,false, vertices);
		assertTrue(connected);
	}
	
	@Test
	public void connectVertexCreatesCorrectLinks() {
		TransitStop transitStop = createTransitStop();
		connector.connectVertex(transitStop,false, vertices);
		assertBidirectionalEdges(transitStop, transitVertex);
	}
	
	@Test
	public void connectVertexCreatesCorrectLinksWithPrefixedTransitVertex() {
		TransitStop transitStop = createPrefixMatchedTransitStop();
		connector.connectVertex(transitStop,false, vertices);
		assertBidirectionalEdges(transitStop, prefixedTransitVertex);
	}
	
	@Test
	public void connectVertexCreatesCorrectLinksWithPrefixedTransitStop() {
		TransitStop transitStop = createPrefixedTransitStop();
		connector.connectVertex(transitStop,false, vertices);
		assertBidirectionalEdges(transitStop, transitVertex);
	}
	
	@Test
	public void connectVertexDoesNotCreateIncorrectLinks() {
		TransitStop transitStop = createTransitStop();
		connector.connectVertex(transitStop,false, vertices);
		assertNoEdges(prefixedTransitVertex);
		assertNoEdges(nonTransitVertex);
	}
	
	@Test
	public void connectVertexFailsWhenNoVertexMatches() {
		TransitStop transitStop = createUnmatchedTransitStop();
		boolean connected = connector.connectVertex(transitStop, false, vertices);
		assertFalse(connected);
	}
	
	@Test
	public void connectVertexDoesNotCreateLinksWhenNoVertexMatches() {
		TransitStop transitStop = createUnmatchedTransitStop();
		connector.connectVertex(transitStop,false, vertices);
		assertNoEdges(transitVertex);
		assertNoEdges(prefixedTransitVertex);
		assertNoEdges(nonTransitVertex);
	}
	
}