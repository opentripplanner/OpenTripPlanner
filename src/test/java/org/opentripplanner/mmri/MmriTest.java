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

package org.opentripplanner.mmri;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.impl.GtfsGraphBuilderImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.LongDistancePathService;
import org.opentripplanner.updater.alerts.AlertsUpdateHandler;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;

/**
 * What is this package doing here?
 *
 * In 2013, significant improvements were made to OTP as part of a precommercial procurement project
 * in The Netherlands called MMRI ("MultiModale ReisInformatie" => "multimodal travel information").
 * This project is itself part of a larger project called "Better Benutten" => "better utilization".
 * Most effort concentrated on the implementation of GTFS-RT updates and related improvements to the
 * architecture of OTP. Additionally, a testing module was developed to verify that all the planners
 * that were involved in the project (not just OTP) met a minimum set of requirements. OTP was first
 * to pass all tests, ahead of two different solutions. Unfortunately, having two sets of tests does
 * not make it simpler to continuously verify that OTP still functions correctly, which is why these
 * MMRI tests have now been added to OTP's own test suite. These versions are intended to be a close
 * approximation of reality, but several minor shortcuts have been taken, like applying trip updates
 * directly to the graph instead of going through the thread-safe graph writer framework. Given that
 * thread-safety is a technical issue and not a functional one, this is considered to be acceptable.
 */
abstract class MmriTest extends TestCase {
    AlertsUpdateHandler alertsUpdateHandler;
    PlanGenerator planGenerator;
    LongDistancePathService longDistancePathService;
    GenericAStar genericAStar;
    Graph graph;
    ArrayList<ServiceDay> serviceDayList;
    TimetableSnapshotSource timetableSnapshotSource;
    AlertPatchServiceImpl alertPatchServiceImpl;

    abstract String getFeedName();

    protected void setUp() {
        File gtfs = new File("src/test/resources/mmri/" + getFeedName() + ".zip");
        File gtfsRealTime = new File("src/test/resources/mmri/" + getFeedName() + ".pb");
        GtfsBundle gtfsBundle = new GtfsBundle(gtfs);
        List<GtfsBundle> gtfsBundleList = Collections.singletonList(gtfsBundle);
        GtfsGraphBuilderImpl gtfsGraphBuilderImpl = new GtfsGraphBuilderImpl(gtfsBundleList);

        alertsUpdateHandler = new AlertsUpdateHandler();
        graph = new Graph();
        gtfsBundle.setTransfersTxtDefinesStationPaths(true);
        gtfsGraphBuilderImpl.buildGraph(graph, null);
        graph.index(new DefaultStreetVertexIndexFactory());
        timetableSnapshotSource = new TimetableSnapshotSource(graph);
        timetableSnapshotSource.setPurgeExpiredData(false);
        graph.setTimetableSnapshotSource(timetableSnapshotSource);
        alertPatchServiceImpl = new AlertPatchServiceImpl(graph);
        alertsUpdateHandler.setAlertPatchService(alertPatchServiceImpl);
        alertsUpdateHandler.setDefaultAgencyId("MMRI");

        try {
            InputStream inputStream = new FileInputStream(gtfsRealTime);
            FeedMessage feedMessage = FeedMessage.PARSER.parseFrom(inputStream);
            List<FeedEntity> feedEntityList = feedMessage.getEntityList();
            List<TripUpdate> updates = new ArrayList<TripUpdate>(feedEntityList.size());
            for (FeedEntity feedEntity : feedEntityList) {
                updates.add(feedEntity.getTripUpdate());
            }
            timetableSnapshotSource.applyTripUpdates(updates, "MMRI");
            alertsUpdateHandler.update(feedMessage);
        } catch (Exception exception) {}

        genericAStar = new GenericAStar();
        longDistancePathService = new LongDistancePathService(null, genericAStar);
        planGenerator = new PlanGenerator(null, longDistancePathService);
        serviceDayList = new ArrayList<ServiceDay>(3);

        serviceDayList.add(new ServiceDay(graph, 1388534400L, graph.getCalendarService(), "MMRI"));
        serviceDayList.add(new ServiceDay(graph, 1388620800L, graph.getCalendarService(), "MMRI"));
        serviceDayList.add(new ServiceDay(graph, 1388707200L, graph.getCalendarService(), "MMRI"));
    }

    Leg plan(long dateTime, String fromVertex, String toVertex, String onTripId,
            boolean wheelchairAccessible, boolean preferLeastTransfers, TraverseMode preferredMode,
            String excludedRoute, String excludedStop) {
        return plan(dateTime, fromVertex, toVertex, onTripId, wheelchairAccessible,
                preferLeastTransfers, preferredMode, excludedRoute, excludedStop, 1)[0];
    }

    Leg[] plan(long dateTime, String fromVertex, String toVertex, String onTripId,
            boolean wheelchairAccessible, boolean preferLeastTransfers, TraverseMode preferredMode,
            String excludedRoute, String excludedStop, int legCount) {
        final TraverseMode mode = preferredMode != null ? preferredMode : TraverseMode.TRANSIT;
        RoutingRequest routingRequest = new RoutingRequest();

        routingRequest.setArriveBy(dateTime < 0);
        routingRequest.dateTime = Math.abs(dateTime);
        if (fromVertex != null && !fromVertex.isEmpty()) {
            routingRequest.setFrom(new GenericLocation(null, "MMRI_" + fromVertex));
        }
        if (toVertex != null && !toVertex.isEmpty()) {
            routingRequest.setTo(new GenericLocation(null, "MMRI_" + toVertex));
        }
        if (onTripId != null && !onTripId.isEmpty()) {
            routingRequest.setStartingTransitTripId(new AgencyAndId("MMRI", onTripId));
        }
        routingRequest.setRoutingContext(graph);
        routingRequest.setWheelchairAccessible(wheelchairAccessible);
        routingRequest.setTransferPenalty(preferLeastTransfers ? 300 : 0);
        routingRequest.setModes(new TraverseModeSet(TraverseMode.WALK, mode));
        if (excludedRoute != null && !excludedRoute.isEmpty()) {
            routingRequest.setBannedRoutes("MMRI__" + excludedRoute);
        }
        if (excludedStop != null && !excludedStop.isEmpty()) {
            routingRequest.setBannedStopsHard("MMRI_" + excludedStop);
        }
        routingRequest.setOtherThanPreferredRoutesPenalty(0);
        routingRequest.setWalkBoardCost(0);
        routingRequest.rctx.serviceDays = serviceDayList;

        TripPlan tripPlan = planGenerator.generate(routingRequest);
        Itinerary itinerary = tripPlan.itinerary.get(0);

        assertEquals(legCount, itinerary.legs.size());

        return itinerary.legs.toArray(new Leg[legCount]);
    }

    void validateLeg(Leg leg, long startTime, long endTime, String toStopId, String fromStopId,
            String alert) {
        assertEquals(startTime, leg.startTime.getTimeInMillis());
        assertEquals(endTime, leg.endTime.getTimeInMillis());
        assertEquals(toStopId, leg.to.stopId.getId());
        assertEquals("MMRI", leg.to.stopId.getAgencyId());
        if (fromStopId != null) {
            assertEquals("MMRI", leg.from.stopId.getAgencyId());
            assertEquals(fromStopId, leg.from.stopId.getId());
        } else {
            assertNull(leg.from.stopId);
        }
        if (alert != null) {
            assertNotNull(leg.alerts);
            assertEquals(1, leg.alerts.size());
            assertEquals(alert, leg.alerts.get(0).alertHeaderText.getSomeTranslation());
        } else {
            assertNull(leg.alerts);
        }
    }
}
