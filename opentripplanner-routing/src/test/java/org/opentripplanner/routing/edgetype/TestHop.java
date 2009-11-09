package org.opentripplanner.routing.edgetype;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.factory.GTFSHopFactory;

public class TestHop extends TestCase {

    public void testHopAfterMidnight() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));

        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        GTFSHopFactory hf = new GTFSHopFactory(context);
        ArrayList<Hop> hops = hf.run();

        Collections.sort(hops, new Hop.HopArrivalTimeComparator());
        Hop last = hops.get(hops.size() - 1);

        GregorianCalendar aSundayAtMidnight = new GregorianCalendar(2009, 7, 30, 0, 0, 0);
        TraverseResult traverseResult = last.traverse(new State(aSundayAtMidnight.getTimeInMillis()), options);
        assertEquals(480.0, traverseResult.weight);
    }

}
