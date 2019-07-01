package org.opentripplanner.graph_builder.module;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class VertexConnectorFactoryTest {
	private VertexConnectorFactory factory;

	@Before
	public void setUp() throws Exception {
		factory = new VertexConnectorFactory();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void getHSLVertexConnector() {
		boolean hsl = (factory.getVertexConnector("HSL") instanceof  HSLVertexConnector);
		assertTrue(hsl);
	}
	@Test
	public void getDefaultVertexConnector() {
		boolean def = (factory.getVertexConnector("") instanceof  DefaultVertexConnector);
		assertTrue(def);
	}
}