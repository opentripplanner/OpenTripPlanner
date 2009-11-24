package org.opentripplanner;

import org.opentripplanner.narrative.TestNarrativeGenerator;
import org.opentripplanner.routing.TestHalfEdges;
import org.opentripplanner.routing.algorithm.TestDijkstra;
import org.opentripplanner.routing.core.TestGraph;
import org.opentripplanner.routing.edgetype.TestStreet;
import org.opentripplanner.routing.edgetype.loader.TestPatternHopLoader;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for core");
        // $JUnit-BEGIN$
        suite.addTestSuite(TestDijkstra.class);
        suite.addTestSuite(TestGraph.class);
        suite.addTestSuite(TestPatternHopLoader.class);
        suite.addTestSuite(TestHalfEdges.class);
        suite.addTestSuite(TestNarrativeGenerator.class);
        suite.addTestSuite(TestStreet.class);
        // $JUnit-END$
        return suite;
    }

}
