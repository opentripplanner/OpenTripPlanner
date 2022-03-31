package org.opentripplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import org.junit.jupiter.api.BeforeEach;
import io.micrometer.core.instrument.Metrics;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.updater.alerts.AlertsUpdateHandler;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Common base class for many test classes which need to load a GTFS feed in preparation for tests. */
public abstract class GtfsTest {

    public Graph graph;
    AlertsUpdateHandler alertsUpdateHandler;
    TimetableSnapshotSource timetableSnapshotSource;
    TransitAlertServiceImpl alertPatchServiceImpl;
    public Router router;
    private GtfsFeedId feedId;

    public abstract String getFeedName();

    @BeforeEach
    protected void setUp() {
        File gtfs = new File("src/test/resources/" + getFeedName());
        File gtfsRealTime = new File("src/test/resources/" + getFeedName() + ".pb");
        GtfsBundle gtfsBundle = new GtfsBundle(gtfs);
        feedId = new GtfsFeedId.Builder().id("FEED").build();
        gtfsBundle.setFeedId(feedId);
        List<GtfsBundle> gtfsBundleList = Collections.singletonList(gtfsBundle);
        GtfsModule gtfsGraphBuilderImpl = new GtfsModule(
                gtfsBundleList,
                ServiceDateInterval.unbounded()
        );

        alertsUpdateHandler = new AlertsUpdateHandler();
        graph = new Graph();

        gtfsGraphBuilderImpl.buildGraph(graph, null);
        // Set the agency ID to be used for tests to the first one in the feed.
        String agencyId = graph.getAgencies().iterator().next().getId().getId();
        System.out.printf("Set the agency ID for this test to %s\n", agencyId);
        graph.index();
        router = new Router(graph, RouterConfig.DEFAULT, Metrics.globalRegistry);
        router.startup();
        timetableSnapshotSource = new TimetableSnapshotSource(graph);
        timetableSnapshotSource.purgeExpiredData = false;
        graph.getOrSetupTimetableSnapshotProvider(g -> timetableSnapshotSource);
        alertPatchServiceImpl = new TransitAlertServiceImpl(graph);
        alertsUpdateHandler.setTransitAlertService(alertPatchServiceImpl);
        alertsUpdateHandler.setFeedId(feedId.getId());

        try {
            final boolean fullDataset = false;
            InputStream inputStream = new FileInputStream(gtfsRealTime);
            FeedMessage feedMessage = FeedMessage.PARSER.parseFrom(inputStream);
            List<FeedEntity> feedEntityList = feedMessage.getEntityList();
            List<TripUpdate> updates = new ArrayList<>(feedEntityList.size());
            for (FeedEntity feedEntity : feedEntityList) {
                updates.add(feedEntity.getTripUpdate());
            }
            CalendarService calendarService = graph.getCalendarService();
            Deduplicator deduplicator = graph.deduplicator;
            GraphIndex graphIndex = graph.index;
            Map<FeedScopedId, Integer> serviceCodes = graph.getServiceCodes();
            timetableSnapshotSource.applyTripUpdates(
                    calendarService,
                    deduplicator,
                    graphIndex,
                    serviceCodes,
                    fullDataset,
                    updates,
                    feedId.getId()
            );
            alertsUpdateHandler.update(feedMessage);
        } catch (Exception exception) {}
    }

    public Itinerary plan(long dateTime, String fromVertex, String toVertex, String onTripId,
            boolean wheelchairAccessible, boolean preferLeastTransfers, TraverseMode preferredMode,
            String excludedRoute, String excludedStop, int legCount
    ) {
        final TraverseMode mode = preferredMode != null ? preferredMode : TraverseMode.TRANSIT;
        RoutingRequest routingRequest = new RoutingRequest();
        routingRequest.setNumItineraries(1);
        
        routingRequest.setArriveBy(dateTime < 0);
        routingRequest.setDateTime(Instant.ofEpochSecond(Math.abs(dateTime)));
        if (fromVertex != null && !fromVertex.isEmpty()) {
            routingRequest.from = LocationStringParser.getGenericLocation(null, feedId.getId() + ":" + fromVertex);
        }
        if (toVertex != null && !toVertex.isEmpty()) {
            routingRequest.to = LocationStringParser.getGenericLocation(null, feedId.getId() + ":" + toVertex);
        }
        if (onTripId != null && !onTripId.isEmpty()) {
            routingRequest.startingTransitTripId = (new FeedScopedId(feedId.getId(), onTripId));
        }
        routingRequest.setWheelchairAccessible(wheelchairAccessible);
        routingRequest.transferCost = (preferLeastTransfers ? 300 : 0);
        routingRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.WALK, mode));
        if (excludedRoute != null && !excludedRoute.isEmpty()) {
            routingRequest.setBannedRoutes(List.of(new FeedScopedId(feedId.getId(), excludedRoute)));
        }
        if (excludedStop != null && !excludedStop.isEmpty()) {
            throw new UnsupportedOperationException("Stop banning is not yet implemented in OTP2");
        }
        routingRequest.setOtherThanPreferredRoutesPenalty(0);
        // The walk board cost is set low because it interferes with test 2c1.
        // As long as boarding has a very low cost, waiting should not be "better" than riding
        // since this makes interlining _worse_ than alighting and re-boarding the same line.
        // TODO rethink whether it makes sense to weight waiting to board _less_ than 1.
        routingRequest.setWaitReluctance(1);
        routingRequest.setWalkBoardCost(30);
        routingRequest.transferSlack = 0;

        RoutingResponse res = new RoutingWorker(router, routingRequest, graph.getTimeZone().toZoneId()).route();
        List<Itinerary> itineraries = res.getTripPlan().itineraries;
        // Stored in instance field for use in individual tests
        Itinerary itinerary = itineraries.get(0);

        assertEquals(legCount, itinerary.legs.size());

        return itinerary;
    }

    public void validateLeg(
            Leg leg,
            long startTime,
            long endTime,
            String toStopId,
            String fromStopId,
            String alert
    ) {
        assertEquals(startTime, leg.getStartTime().getTimeInMillis());
        assertEquals(endTime, leg.getEndTime().getTimeInMillis());
        assertEquals(toStopId, leg.getTo().stop.getId().getId());
        assertEquals(feedId.getId(), leg.getTo().stop.getId().getFeedId());
        if (fromStopId != null) {
            assertEquals(feedId.getId(), leg.getFrom().stop.getId().getFeedId());
            assertEquals(fromStopId, leg.getFrom().stop.getId().getId());
        } else {
            assertNull(leg.getFrom().stop.getId());
        }
        if (alert != null) {
            assertNotNull(leg.getStreetNotes());
            assertEquals(1, leg.getStreetNotes().size());
            assertEquals(alert, leg.getStreetNotes().iterator().next().note.toString());
        } else {
            assertNull(leg.getStreetNotes());
        }
    }
}
