package org.opentripplanner;

import org.opentripplanner.routing.TestHalfEdges;
import org.opentripplanner.routing.algorithm.TestAStar;
import org.opentripplanner.routing.algorithm.TestDijkstra;
import org.opentripplanner.routing.algorithm.TestGraphPath;
import org.opentripplanner.routing.core.TestGraph;
import org.opentripplanner.routing.edgetype.TestStreet;
import org.opentripplanner.routing.edgetype.loader.TestPatternHopLoader;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for core");
        // $JUnit-BEGIN$
        suite.addTestSuite(TestAStar.class);
        suite.addTestSuite(TestDijkstra.class);
        suite.addTestSuite(TestGraph.class);
        suite.addTestSuite(TestGraphPath.class);
        suite.addTestSuite(TestPatternHopLoader.class);
        suite.addTestSuite(TestHalfEdges.class);
        suite.addTestSuite(TestStreet.class);
        // $JUnit-END$
        return suite;
    }

}
