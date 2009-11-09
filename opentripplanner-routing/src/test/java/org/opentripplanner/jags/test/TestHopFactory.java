package org.opentripplanner.jags.test;

import junit.framework.TestCase;

import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.factory.GTFSHopFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class TestHopFactory extends TestCase {

    public void testBasic() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));

        GTFSHopFactory hf = new GTFSHopFactory(context);
        ArrayList<Hop> hops = hf.run();

        Collections.sort(hops, new Hop.HopArrivalTimeComparator());
        Hop last = hops.get(hops.size() - 1);
        assertEquals(91740, last.getStartStopTime().getDepartureTime());
    }
}
