package org.opentripplanner.graph_builder.module;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class VertexConnectorFactoryTest {
	
	@Before
	public void setUp() throws Exception {
	}
	
	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void getHSLVertexConnector() {
		boolean hsl = (VertexConnectorFactory.getVertexConnector("HSL") instanceof HSLVertexConnector);
		assertTrue(hsl);
	}
	
	@Test
	public void getDefaultVertexConnector() {
		boolean aDefault = (VertexConnectorFactory.getVertexConnector("") instanceof DefaultVertexConnector);
		assertTrue(aDefault);
	}
	
}