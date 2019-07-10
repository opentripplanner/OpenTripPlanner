package org.opentripplanner.graph_builder.module;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opentripplanner.routing.vertextype.TransitStop;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultVertexConnectorTest extends AbstractVertexConnectorTest {
	
	protected VertexConnector connector = new DefaultVertexConnector();
	
	@Test
	public void connectVertexSucceedsWhenMatchingVertexExists() {
		TransitStop transitStop = createTransitStop();
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
	public void connectVertexDoesNotCreateIncorrectLinks() {
		TransitStop transitStop = createTransitStop();
		connector.connectVertex(transitStop,false, vertices);
		assertNoEdges(prefixedTransitVertex);
		assertNoEdges(nonTransitVertex);
	}
	
	@Test
	public void connectVertexFailsWhenNoMatchingVertexExists() {
		TransitStop transitStop = createUnmatchedTransitStop();
		boolean connected = connector.connectVertex(transitStop, false, vertices);
		assertFalse(connected);
	}
	
	@Test
	public void connectVertexDoesNotCreateLinksWhenNoMatchingVertexExists() {
		TransitStop transitStop = createUnmatchedTransitStop();
		connector.connectVertex(transitStop,false, vertices);
		assertNoEdges(transitVertex);
		assertNoEdges(prefixedTransitVertex);
		assertNoEdges(nonTransitVertex);
	}
	
}
