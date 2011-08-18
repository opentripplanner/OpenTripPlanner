/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.algorithm;

import java.io.File;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.TestUtils;

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
        long startTime = TestUtils.dateInSeconds(2009, 8, 7, 12, 0, 0);
        spt = AStar.getShortestPathTree(gg, "Caltrain_Millbrae Caltrain",
                "Caltrain_Mountain View Caltrain", startTime, options);

        path = spt.getPath(gg.getVertex("Caltrain_Mountain View Caltrain"), true);

        FareService fareService = gg.getService(FareService.class);
        
        Fare cost = fareService.getCost(path);
        assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 425));
    }

    public void testPortland() throws Exception {

        Graph gg = ConstantsForTests.getInstance().getPortlandGraph();
        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(ConstantsForTests.getInstance().getPortlandContext());
        StreetVertexIndexServiceImpl index = new StreetVertexIndexServiceImpl(gg);
        index.setup();
        ShortestPathTree spt;
        GraphPath path = null;
        long startTime = TestUtils.dateInSeconds(2009, 11, 1, 12, 0, 0);

        // from zone 3 to zone 2
        spt = AStar.getShortestPathTree(gg, "TriMet_10579", "TriMet_8371", startTime,
                options);

        path = spt.getPath(gg.getVertex("TriMet_8371"), true);
        assertNotNull(path);

        FareService fareService = gg.getService(FareService.class);
        Fare cost = fareService.getCost(path);
        assertEquals(new Money(new WrappedCurrency("USD"), 200), cost.getFare(FareType.regular));

        // long trip

        startTime = TestUtils.dateInSeconds(2009, 11, 1, 14, 0, 0);
        spt = AStar.getShortestPathTree(gg, "TriMet_8389", "TriMet_1252", startTime,
                options);

        path = spt.getPath(gg.getVertex("TriMet_1252"), true);
        assertNotNull(path);
        cost = fareService.getCost(path);
        
        //assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 460));
        
        // complex trip
        options.maxTransfers = 5;
        startTime = TestUtils.dateInSeconds(2009, 11, 1, 14, 0, 0);
        spt = AStar.getShortestPathTree(gg, "TriMet_10428", "TriMet_4231", startTime,
                options);

        path = spt.getPath(gg.getVertex("TriMet_4231"), true);
        assertNotNull(path);
        cost = fareService.getCost(path);
        //
        // this is commented out because portland's fares are, I think, broken in the gtfs. see
        // thread on gtfs-changes.
        // assertEquals(cost.getFare(FareType.regular), new Money(new WrappedCurrency("USD"), 430));
    }
}
