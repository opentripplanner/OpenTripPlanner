package org.opentripplanner.jags.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for core");
		//$JUnit-BEGIN$
		suite.addTestSuite(TestFeed.class);
		suite.addTestSuite(TestGraph.class);
		suite.addTestSuite(TestHop.class);
		suite.addTestSuite(TestHopFactory.class);
		suite.addTestSuite(TestKaoGraph.class);
		suite.addTestSuite(TestDijkstra.class);
		//$JUnit-END$
		return suite;
	}

}
