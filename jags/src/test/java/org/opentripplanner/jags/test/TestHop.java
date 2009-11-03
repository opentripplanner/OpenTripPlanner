package org.opentripplanner.jags.test;

import junit.framework.TestCase;

import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.factory.GTFSHopFactory;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.gtfs.GtfsLibrary;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;

public class TestHop extends TestCase {

  public void testHopAfterMidnight() throws Exception {

    GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));

    WalkOptions options = new WalkOptions();
    options.setGtfsContext(context);

    GTFSHopFactory hf = new GTFSHopFactory(context);
    ArrayList<Hop> hops = hf.run();

    Collections.sort(hops, new Hop.HopArrivalTimeComparator());
    Hop last = hops.get(hops.size() - 1);

    GregorianCalendar aSundayAtMidnight = new GregorianCalendar(2009, 7, 30, 0,
        0, 0);
    WalkResult walkResult = last.walk(new State(aSundayAtMidnight.getTimeInMillis()), options);
    assertEquals(480.0,walkResult.weight);
  }
}
