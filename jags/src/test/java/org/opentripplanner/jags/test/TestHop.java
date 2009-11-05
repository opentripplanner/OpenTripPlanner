package org.opentripplanner.jags.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TraverseOptions;
import org.opentripplanner.jags.core.TraverseResult;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.factory.GTFSHopFactory;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.gtfs.GtfsLibrary;

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
