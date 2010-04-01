package org.opentripplanner.routing.algorithm;

import java.io.File;
import java.util.GregorianCalendar;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import junit.framework.TestCase;

public class TestFares extends TestCase {
    public void testBasic() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));
        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        Graph gg = new Graph();
        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(gg);
        ShortestPathTree spt;
        GraphPath path = null;
        long startTime = new GregorianCalendar(2009, 8, 7, 12, 0, 0).getTimeInMillis();
        spt = AStar.getShortestPathTree(gg, "Caltrain_Millbrae Caltrain",
                "Caltrain_Mountain View Caltrain", new State(startTime), options);

        path = spt.getPath(gg.getVertex("Caltrain_Mountain View Caltrain"));

        Fare cost = path.getCost();
        assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 425));
    }

    public void testPortland() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.PORTLAND_GTFS));
        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        Graph gg = new Graph();
        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(gg);
        ShortestPathTree spt;
        GraphPath path = null;
        long startTime = new GregorianCalendar(2009, 11, 1, 12, 0, 0).getTimeInMillis();

        // from zone 3 to zone 2
        spt = AStar.getShortestPathTree(gg, "TriMet_10579", "TriMet_8346", new State(startTime),
                options);

        path = spt.getPath(gg.getVertex("TriMet_8346"));
        assertNotNull(path);
        Fare cost = path.getCost();
        assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 200));

        System.out.println("long trip");
        // long trip

        startTime = new GregorianCalendar(2009, 11, 1, 14, 0, 0).getTimeInMillis();
        spt = AStar.getShortestPathTree(gg, "TriMet_8389", "TriMet_1252", new State(startTime),
                options);

        path = spt.getPath(gg.getVertex("TriMet_1252"));
        assertNotNull(path);
        cost = path.getCost();
        
        //assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 460));
        
        // complex trip

        startTime = new GregorianCalendar(2009, 11, 1, 14, 0, 0).getTimeInMillis();
        spt = AStar.getShortestPathTree(gg, "TriMet_10428", "TriMet_4231", new State(startTime),
                options);

        path = spt.getPath(gg.getVertex("TriMet_4231"));
        assertNotNull(path);
        cost = path.getCost();
        /*
         * this is commented out because portland's fares are, I think, broken in the gtfs. see
         * thread on gtfs-changes.
         */
        // assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 430));

    }
}
