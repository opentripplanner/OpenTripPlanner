package org.opentripplanner.jags.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for core");
		//$JUnit-BEGIN$
		suite.addTestSuite(TestDijkstra.class);
		suite.addTestSuite(TestGraph.class);
		suite.addTestSuite(TestHop.class);
		suite.addTestSuite(TestHopFactory.class);
		suite.addTestSuite(TestKaoGraph.class);
		suite.addTestSuite(TestNarrativeGenerator.class);
		suite.addTestSuite(TestStreetDirection.class);
		//$JUnit-END$
		return suite;
	}

}
