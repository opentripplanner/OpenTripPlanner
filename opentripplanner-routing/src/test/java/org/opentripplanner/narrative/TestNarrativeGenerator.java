package org.opentripplanner.narrative;

import java.util.GregorianCalendar;
import java.util.Vector;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.narrative.model.Narrative;
import org.opentripplanner.narrative.model.NarrativeSection;
import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class TestNarrativeGenerator extends TestCase {

    Graph graph;

    GtfsContext context;

    public void setUp() {
        graph = ConstantsForTests.getInstance().getPortlandGraph();
        context = ConstantsForTests.getInstance().getPortlandContext();
    }

    public void testNarrativeGenerator() {

        Vertex airport = graph.getVertex("TriMet_10579");
        TraverseOptions wo = new TraverseOptions();
        wo.setGtfsContext(context);
        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);
        ShortestPathTree spt = Dijkstra.getShortestPathTree(graph, "TriMet_6876", airport
                .getLabel(), new State(startTime.getTimeInMillis()), wo);

        GraphPath path = spt.getPath(airport);

        assertNotNull(path);

        Narrative narrative = new Narrative(path);
        Vector<NarrativeSection> sections = narrative.getSections();
        /*
         * This trip, from a bus stop near TriMet HQ to the Airport, should take the #70 bus, then
         * transfer, then take the MAX Red Line, thus three sections. If the test fails, that's
         * because a more complex (and wrong) route is being chosen.
         */

        NarrativeSection busSection = sections.elementAt(0);
        NarrativeSection redLineSection = sections.elementAt(2);

        assertTrue(busSection.getEndTime() < redLineSection.getStartTime());
        assertEquals(startTime.getTimeInMillis(), busSection.getStartTime());

        assertEquals(TransportationMode.BUS, busSection.getMode());
        assertEquals(TransportationMode.TRAM, redLineSection.getMode());

        assertEquals(3, sections.size());
    }

    /**
     * TODO: bdferris - I killed this test temporarily because of a shapefile loader dependency. I
     * promise to replace it with a better test once the narrative generation code is refactored.
     */
    public void testWalkNarrative() {

    }
}
