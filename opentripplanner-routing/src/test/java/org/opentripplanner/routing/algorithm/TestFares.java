package org.opentripplanner.routing.algorithm;

import java.io.File;
import java.util.Currency;
import java.util.GregorianCalendar;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.edgetype.loader.GTFSPatternHopLoader;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import junit.framework.TestCase;

public class TestFares extends TestCase {
    public void testBasic() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));
        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        Graph gg = new Graph();
        GTFSPatternHopLoader hl = new GTFSPatternHopLoader(gg, context);
        hl.load();
        ShortestPathTree spt;
        GraphPath path = null;
        long startTime = new GregorianCalendar(2009, 8, 7, 12, 0, 0).getTimeInMillis();
        spt = AStar.getShortestPathTree(gg, "Caltrain_Millbrae Caltrain",
                "Caltrain_Mountain View Caltrain", new State(startTime), options);

        path = spt.getPath(gg.getVertex("Caltrain_Mountain View Caltrain"));

        Fare cost = path.vertices.lastElement().state.getCost(context);
        assertEquals(cost.getFare(FareType.regular), new Money(Currency.getInstance("USD"), 425));
    }
}
