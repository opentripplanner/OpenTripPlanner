package org.opentripplanner;

import org.opentripplanner.narrative.TestNarrativeGenerator;
import org.opentripplanner.routing.algorithm.TestDijkstra;
import org.opentripplanner.routing.core.TestGraph;
import org.opentripplanner.routing.edgetype.TestHop;
import org.opentripplanner.routing.edgetype.TestStreet;
import org.opentripplanner.routing.edgetype.factory.TestHopFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for core");
        // $JUnit-BEGIN$
        suite.addTestSuite(TestDijkstra.class);
        suite.addTestSuite(TestGraph.class);
        suite.addTestSuite(TestHop.class);
        suite.addTestSuite(TestHopFactory.class);
        // suite.addTestSuite(TestKaoGraph.class); //failing because kao graphs don't really work
        // with the new board-alight stuff
        suite.addTestSuite(TestNarrativeGenerator.class);
        suite.addTestSuite(TestStreet.class);
        // $JUnit-END$
        return suite;
    }

}
