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
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.loader.GTFSPatternHopLoader;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class TestDijkstra extends TestCase {
    public void testBasic() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));

        TraverseOptions options = new TraverseOptions();
        options.setGtfsContext(context);

        Graph gg = new Graph();
        GTFSPatternHopLoader hl = new GTFSPatternHopLoader(gg, context);
        hl.load();

        long startTime = new GregorianCalendar(2009, 8, 7, 12, 0, 0).getTimeInMillis();
        ShortestPathTree spt = Dijkstra.getShortestPathTree(gg, "Caltrain_Millbrae Caltrain",
                "Caltrain_Mountain View Caltrain", new State(startTime), options);

        GraphPath path = spt.getPath(gg.getVertex("Caltrain_Mountain View Caltrain"));

        GregorianCalendar endTime = new GregorianCalendar(2009, 8, 7, 13, 29);
        
        assertEquals(path.vertices.lastElement().state.getTime(), endTime.getTimeInMillis());
    }
}
