package org.opentripplanner.graph_builder.module;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopStreetVertex;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultVertexConnectorTest {
	private DefaultVertexConnector connector = new DefaultVertexConnector();
	private ArrayList<Vertex> vertices;
	TransitStopStreetVertex vertex;
	TransitStop ts;

	@Before
	public void setUp() throws Exception {
		vertices = new ArrayList<>();
		ts = Mockito.mock(TransitStop.class);
		Graph g = Mockito.mock(Graph.class);
		vertex = new TransitStopStreetVertex(g,"label",23.03,42.03,1,"TestWithoutprefix","123");
		vertices.add(vertex);
		vertex = new TransitStopStreetVertex(g,"label1",23.13,42.13,1,"TestWithprefix","H321");
		vertices.add(vertex);
		OsmVertex osmvertex = new OsmVertex(g,"Nonstreettransitvertex",23,24,100L);
		vertices.add(osmvertex);
	}

	@After
	public void tearDown() throws Exception {
		vertices.clear();
	}

	@Test
	public void connectVertexSuccess() {
		when(ts.getStopCode()).thenReturn("123");
		boolean connected = connector.connectVertex(ts,false, vertices);
		assertTrue(connected);
	}

	@Test
	public void connectVertexFail() {
		when(ts.getStopCode()).thenReturn("1243");
		boolean connected = connector.connectVertex(ts,false, vertices);
		assertFalse(connected);
	}
}