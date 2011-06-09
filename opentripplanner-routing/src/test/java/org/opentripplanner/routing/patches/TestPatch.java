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
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.patch.RouteNotePatch;
import org.opentripplanner.routing.patch.StopNotePatch;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;

public class TestPatch extends TestCase {
	private Graph graph;
	private TraverseOptions options;

	public void setUp() throws Exception {

		GtfsContext context = GtfsLibrary.readGtfs(new File(
				ConstantsForTests.FAKE_GTFS));

		options = new TraverseOptions();
		options.setGtfsContext(context);

		graph = new Graph();
		GTFSPatternHopFactory factory = new GTFSPatternHopFactory(context);
		factory.run(graph);
		TransitIndexService index = new TransitIndexService() {
			/*
			 * mock TransitIndexService always returns preboard/prealight edges
			 * for stop A and a subset of variants for route 1
			 */
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
			public List<RouteVariant> getVariantsForRoute(AgencyAndId routeId) {
				Route route = new Route();
				route.setId(routeId);
				route.setShortName(routeId.getId());
				
				PatternBoard somePatternBoard = (PatternBoard) graph.getOutgoing("agency_A_depart").iterator().next();
				PatternHop somePatternHop = (PatternHop) graph.getOutgoing(somePatternBoard.getToVertex()).iterator().next();
				
				Stop stopA = somePatternHop.getStartStop(); 
				ArrayList<Stop> stops = new ArrayList<Stop>();
				stops.add(stopA);
				
				RouteVariant variant = new RouteVariant(route, stops);
				RouteSegment segment = new RouteSegment(stopA.getId());
				
				segment.board = somePatternBoard;
				segment.hopOut = somePatternHop;
				variant.addSegment(segment);				
				
				ArrayList<RouteVariant> variants = new ArrayList<RouteVariant>();
				variants.add(variant);
				return variants;
			}
		};
		graph.putService(TransitIndexService.class, index);
	}

	public void testStopNotePatch() {
		StopNotePatch snp1 = new StopNotePatch();
		snp1.setStartTime(0);
		snp1.setEndTime(1000L * 60 * 60 * 24 * 365 * 40); // until ~1/1/2011
		snp1.setStartTimeOfDay(0); // midnight
		snp1.setEndTimeOfDay(600); // to ten past
		String note1 = "The first note";
		snp1.setNotes(note1);
		snp1.setId("id1");
		snp1.setStop(new AgencyAndId("agency", "A"));
		snp1.apply(graph);

		StopNotePatch snp2 = new StopNotePatch();
		snp2.setStartTime(0);
		snp2.setEndTime(1000L * 60 * 60 * 24 * 365 * 40); // until ~1/1/2010
		snp2.setStartTimeOfDay(540); // nine past midnight
		snp2.setEndTimeOfDay(21600); // to 6am
		String note2 = "The second note";
		snp2.setNotes(note2);
		snp2.setId("id2");
		snp2.setStop(new AgencyAndId("agency", "A"));
		snp2.apply(graph);

		Vertex stop_a = graph.getVertex("agency_A");
		Vertex stop_e = graph.getVertex("agency_E_arrive");

		ShortestPathTree spt;
		GraphPath path;

		long startTime = new GregorianCalendar(2009, 8, 7, 0, 0, 0).getTimeInMillis();
		spt = AStar.getShortestPathTree(graph, stop_a, stop_e, 
				new State(startTime), options);

		path = spt.getPath(stop_e, false); // do not optimize because we want
		// the first trip out of A
		assertNotNull(path);
		HashSet<String> expectedNotes = new HashSet<String>();
		expectedNotes.add(note1);
		assertEquals(expectedNotes, path.edges.get(0).narrative.getNotes());

		startTime = new GregorianCalendar(2009, 8, 7, 0, 9, 0).getTimeInMillis();
		spt = AStar.getShortestPathTree(graph, stop_a, stop_e, 
				new State(startTime), options);

		path = spt.getPath(stop_e, false);
		expectedNotes.add(note2);
		assertEquals(expectedNotes, path.edges.get(0).narrative.getNotes());
	}

	public void testRouteNotePatch() {
		RouteNotePatch rnp1 = new RouteNotePatch();
		rnp1.setStartTime(0);
		rnp1.setEndTime(1000L * 60 * 60 * 24 * 365 * 40); // until ~1/1/2011
		rnp1.setStartTimeOfDay(21600); // six am
		rnp1.setEndTimeOfDay(43200); // to noon
		String note1 = "The route note";
		rnp1.setNotes(note1);
		rnp1.setId("id1");
		rnp1.setRoute(new AgencyAndId("agency", "1"));
		rnp1.apply(graph);

		Vertex stop_a = graph.getVertex("agency_A");
		Vertex stop_e = graph.getVertex("agency_E_arrive");

		ShortestPathTree spt;
		GraphPath path;

		long startTime = new GregorianCalendar(2009, 8, 7, 7, 0, 0).getTimeInMillis();
		spt = AStar.getShortestPathTree(graph, stop_a, stop_e,
				new State(startTime), options);

		path = spt.getPath(stop_e, false); 
		assertNotNull(path);
		HashSet<String> expectedNotes = new HashSet<String>();
		expectedNotes.add(note1);
		assertEquals(expectedNotes, path.edges.get(1).narrative.getNotes());
	}
}