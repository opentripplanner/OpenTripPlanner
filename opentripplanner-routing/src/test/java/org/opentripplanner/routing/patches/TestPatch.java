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

package org.opentripplanner.routing.patches;


import java.io.File;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.patch.StopNotePatch;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.transit_index.RouteVariant;

public class TestPatch extends TestCase {
	private Graph graph;
	private TraverseOptions options;

	public void setUp() throws Exception {

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));

        options = new TraverseOptions();
        options.setGtfsContext(context);

        graph = new Graph();
        GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
        factory.run(graph);
        TransitIndexService index = new TransitIndexService() {
        	/* mock TransitIndexService always returns preboard/prealight edges for stop A */
			@Override
			public Edge getPrealightEdge(AgencyAndId stop) {
				return graph.getOutgoing("agency_A_arrive").iterator().next();
			}

			@Override
			public Edge getPreboardEdge(AgencyAndId stop) {
				return graph.getIncoming("agency_A_depart").iterator().next();
			}

			@Override
			public RouteVariant getVariantForTrip(AgencyAndId trip) {
				return null;
			}

			@Override
			public List<RouteVariant> getVariantsForRoute(AgencyAndId route) {
				return null;
			}        	
        };
        graph.putService(TransitIndexService.class, index);
        
	}
	
	public void testStopNotePatch() {
		StopNotePatch snp1 = new StopNotePatch();
		snp1.setStartTime(0);
		snp1.setEndTime(1000L*60*60*24*365*40); //until ~1/1/2011
		snp1.setStartTimeOfDay(0); //midnight 
		snp1.setEndTimeOfDay(600); //to ten past
		String note1 = "The first note";
		snp1.setNotes(note1);
		snp1.setId("id1");
		snp1.setStop(new AgencyAndId("agency", "A"));
		snp1.apply(graph);
		
		StopNotePatch snp2 = new StopNotePatch();
		snp2.setStartTime(0);
		snp2.setEndTime(1000L*60*60*24*365*40); //until ~1/1/2010
		snp2.setStartTimeOfDay(540); //nine past midnight
		snp2.setEndTimeOfDay(43200); //to noon
		String note2 = "The second note";
		snp2.setNotes(note2);
		snp2.setId("id2");
		snp2.setStop(new AgencyAndId("agency", "A"));
		snp2.apply(graph);
		
        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_e = graph.getVertex("agency_E_arrive");

        ShortestPathTree spt;
        GraphPath path;

        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_e.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_e, false); //do not optimize because we want the first trip out of A 
        assertNotNull(path);
        HashSet<String> expectedNotes = new HashSet<String>();
        expectedNotes.add(note1);
        assertEquals(expectedNotes, path.edges.get(0).narrative.getNotes());
        
        
        spt = AStar.getShortestPathTree(graph, stop_a.getLabel(), stop_e.getLabel(), new State(
                new GregorianCalendar(2009, 8, 7, 0, 9, 0).getTimeInMillis()), options);

        path = spt.getPath(stop_e, false);         
        expectedNotes.add(note2);
        assertEquals(expectedNotes, path.edges.get(0).narrative.getNotes());
	}
}