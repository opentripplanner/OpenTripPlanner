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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.AlertPatch;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.util.TestUtils;

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
            /*
             * mock TransitIndexService always returns preboard/prealight edges for stop A and a
             * subset of variants for route 1
             */
            @Override
            public Edge getPrealightEdge(AgencyAndId stop) {
                return graph.getVertex("agency_A_arrive").getOutgoing().iterator().next();
            }

            @Override
            public Edge getPreboardEdge(AgencyAndId stop) {
                return graph.getVertex("agency_A_depart").getIncoming().iterator().next();
            }

            @Override
            public RouteVariant getVariantForTrip(AgencyAndId trip) {
                return null;
            }

            @Override
            public List<RouteVariant> getVariantsForAgency(String agency) {
                return null;
            }

            @Override
            public List<RouteVariant> getVariantsForRoute(AgencyAndId routeId) {
                Route route = new Route();
                route.setId(routeId);
                route.setShortName(routeId.getId());

                PatternBoard somePatternBoard = (PatternBoard) graph.getVertex("agency_A_depart")
                        .getOutgoing().iterator().next();
                PatternHop somePatternHop = (PatternHop) somePatternBoard.getToVertex()
                        .getOutgoing().iterator().next();

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

            @Override
            public List<String> getDirectionsForRoute(AgencyAndId route) {
                return Collections.emptyList();
            }

            @Override
            public List<TraverseMode> getAllModes() {
                return Collections.emptyList();
            }

            @Override
            public List<AgencyAndId> getAllRouteIds() {
                return Collections.emptyList();
            }

            @Override
            public void addCalendars(Collection<ServiceCalendar> allCalendars) {
            }

            @Override
            public void addCalendarDates(Collection<ServiceCalendarDate> allDates) {
            }

            @Override
            public List<String> getAllAgencies() {
                return Arrays.asList("agency");
            }

            @Override
            public List<ServiceCalendarDate> getCalendarDatesByAgency(String agency) {
                return null;
            }

            @Override
            public List<ServiceCalendar> getCalendarsByAgency(String agency) {
                return null;
            }

            @Override
            public Agency getAgency(String id) {
                return null;
            }
        };
        graph.putService(TransitIndexService.class, index);
    }

    public void testStopAlertPatch() {
        AlertPatch snp1 = new AlertPatch();
        snp1.addTimePeriod(0, 1000L * 60 * 60 * 24 * 365 * 40); // until ~1/1/2011
        Alert note1 = Alert.createSimpleAlerts("The first note");
        snp1.setAlert(note1);
        snp1.setId("id1");
        snp1.setStop(new AgencyAndId("agency", "A"));
        snp1.apply(graph);

        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_e = graph.getVertex("agency_E_arrive");

        ShortestPathTree spt;
        GraphPath path;

        long startTime = TestUtils.dateInSeconds(2009, 8, 7, 0, 0, 0);
        spt = AStar.getShortestPathTree(graph, stop_a, stop_e, startTime, options);

        path = spt.getPath(stop_e, true);
        assertNotNull(path);
        HashSet<Alert> expectedNotes = new HashSet<Alert>();
        expectedNotes.add(note1);
        assertEquals(expectedNotes, path.states.get(1).getBackEdgeNarrative().getNotes());
    }

    public void testTimeRanges() {
        AlertPatch snp1 = new AlertPatch();
        long breakTime = TestUtils.dateInSeconds(2009, 8, 7, 0, 0, 0);
        snp1.addTimePeriod(0, breakTime); // until the beginning of the day
        long secondPeriodStartTime = TestUtils.dateInSeconds(2009, 8, 7, 7, 0, 0);
        long secondPeriodEndTime = TestUtils.dateInSeconds(2009, 8, 8, 0, 0, 0);
        snp1.addTimePeriod(secondPeriodStartTime, secondPeriodEndTime);
        Alert note1 = Alert.createSimpleAlerts("The first note");
        snp1.setAlert(note1);
        snp1.setId("id1");
        snp1.setStop(new AgencyAndId("agency", "A"));
        snp1.apply(graph);

        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_e = graph.getVertex("agency_E_arrive");

        ShortestPathTree spt;
        GraphPath path;

        long startTime = TestUtils.dateInSeconds(2009, 8, 7, 0, 0, 0);
        spt = AStar.getShortestPathTree(graph, stop_a, stop_e, startTime, options);

        path = spt.getPath(stop_e, true);
        assertNotNull(path);
        // expect no notes because we are during the break
        assertNull(path.states.get(1).getBackEdgeNarrative().getNotes());

        // now a trip during the second period
        startTime = TestUtils.dateInSeconds(2009, 8, 7, 8, 0, 0);
        spt = AStar.getShortestPathTree(graph, stop_a, stop_e, startTime, options);

        path = spt.getPath(stop_e, false); // do not optimize because we want the first trip
        assertNotNull(path);
        HashSet<Alert> expectedNotes = new HashSet<Alert>();
        expectedNotes.add(note1);
        assertEquals(expectedNotes, path.states.get(1).getBackEdgeNarrative().getNotes());
    }

    public void testRouteNotePatch() {
        AlertPatch rnp1 = new AlertPatch();

        rnp1.addTimePeriod(0, 1000L * 60 * 60 * 24 * 365 * 40); // until ~1/1/2011
        Alert note1 = Alert.createSimpleAlerts("The route note");
        rnp1.setAlert(note1);
        rnp1.setId("id1");
        rnp1.setRoute(new AgencyAndId("agency", "1"));
        rnp1.apply(graph);

        Vertex stop_a = graph.getVertex("agency_A");
        Vertex stop_e = graph.getVertex("agency_E_arrive");

        ShortestPathTree spt;
        GraphPath path;

        long startTime = TestUtils.dateInSeconds(2009, 8, 7, 7, 0, 0);
        spt = AStar.getShortestPathTree(graph, stop_a, stop_e, startTime, options);

        path = spt.getPath(stop_e, false);
        assertNotNull(path);
        HashSet<Alert> expectedNotes = new HashSet<Alert>();
        expectedNotes.add(note1);
        assertEquals(expectedNotes, path.states.get(2).getBackEdgeNarrative().getNotes());
    }
}
