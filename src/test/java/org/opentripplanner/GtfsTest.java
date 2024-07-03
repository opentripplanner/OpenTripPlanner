package org.opentripplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.routing.api.request.StreetMode.NOT_SET;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;
import static org.opentripplanner.updater.trip.BackwardsDelayPropagationType.REQUIRED_NO_DATA;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RequestModesBuilder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.alert.AlertsUpdateHandler;
import org.opentripplanner.updater.trip.TimetableSnapshotSource;
import org.opentripplanner.updater.trip.UpdateIncrementality;

/** Common base class for many test classes which need to load a GTFS feed in preparation for tests. */
public abstract class GtfsTest {

  public Graph graph;
  public TransitModel transitModel;

  AlertsUpdateHandler alertsUpdateHandler;
  TimetableSnapshotSource timetableSnapshotSource;
  TransitAlertServiceImpl alertPatchServiceImpl;
  public OtpServerRequestContext serverContext;
  public GtfsFeedId feedId;

  public abstract String getFeedName();

  public Itinerary plan(
    long dateTime,
    String fromVertex,
    String toVertex,
    String onTripId,
    boolean wheelchairAccessible,
    boolean preferLeastTransfers,
    TransitMode preferredMode,
    String excludedRoute,
    String excludedStop,
    int legCount
  ) {
    // Preconditions
    if (excludedStop != null && !excludedStop.isEmpty()) {
      throw new UnsupportedOperationException("Stop banning is not yet implemented in OTP2");
    }

    // Init request
    RouteRequest routingRequest = new RouteRequest();

    routingRequest.setNumItineraries(1);

    routingRequest.setArriveBy(dateTime < 0);
    routingRequest.setDateTime(Instant.ofEpochSecond(Math.abs(dateTime)));
    if (fromVertex != null && !fromVertex.isEmpty()) {
      routingRequest.setFrom(
        LocationStringParser.getGenericLocation(null, feedId.getId() + ":" + fromVertex)
      );
    }
    if (toVertex != null && !toVertex.isEmpty()) {
      routingRequest.setTo(
        LocationStringParser.getGenericLocation(null, feedId.getId() + ":" + toVertex)
      );
    }
    if (onTripId != null && !onTripId.isEmpty()) {
      // TODO VIA - set different on-board request
      //routingRequest.startingTransitTripId = (new FeedScopedId(feedId.getId(), onTripId));
    }
    routingRequest.setWheelchair(wheelchairAccessible);

    RequestModesBuilder requestModesBuilder = RequestModes
      .of()
      .withDirectMode(NOT_SET)
      .withAccessMode(WALK)
      .withTransferMode(WALK)
      .withEgressMode(WALK);
    routingRequest.journey().setModes(requestModesBuilder.build());

    var filterRequestBuilder = TransitFilterRequest.of();
    if (preferredMode != null) {
      filterRequestBuilder.addSelect(
        SelectRequest.of().addTransportMode(new MainAndSubMode(preferredMode, null)).build()
      );
    } else {
      filterRequestBuilder.addSelect(
        SelectRequest.of().withTransportModes(MainAndSubMode.all()).build()
      );
    }

    if (excludedRoute != null && !excludedRoute.isEmpty()) {
      List<FeedScopedId> routeIds = List.of(new FeedScopedId(feedId.getId(), excludedRoute));
      filterRequestBuilder.addNot(SelectRequest.of().withRoutes(routeIds).build());
    }

    routingRequest.journey().transit().setFilters(List.of(filterRequestBuilder.build()));

    // Init preferences
    routingRequest.withPreferences(preferences -> {
      preferences.withTransfer(tx -> {
        tx.withSlack(0);
        tx.withWaitReluctance(1);
        tx.withCost(preferLeastTransfers ? 300 : 0);
      });

      // The walk board cost is set low because it interferes with test 2c1.
      // As long as boarding has a very low cost, waiting should not be "better" than riding
      // since this makes interlining _worse_ than alighting and re-boarding the same line.
      // TODO rethink whether it makes sense to weight waiting to board _less_ than 1.
      preferences.withWalk(w -> w.withBoardCost(30));
      preferences.withTransit(tr -> tr.setOtherThanPreferredRoutesPenalty(0));
    });

    // Route
    RoutingResponse res = serverContext.routingService().route(routingRequest);

    // Assert itineraries
    List<Itinerary> itineraries = res.getTripPlan().itineraries;
    // Stored in instance field for use in individual tests
    Itinerary itinerary = itineraries.get(0);

    assertEquals(legCount, itinerary.getLegs().size());

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
    assertEquals(startTime, leg.getStartTime().toInstant().toEpochMilli());
    assertEquals(endTime, leg.getEndTime().toInstant().toEpochMilli());
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

  @BeforeEach
  protected void setUp() throws Exception {
    File gtfs = new File("src/test/resources/" + getFeedName());
    File gtfsRealTime = new File("src/test/resources/" + getFeedName() + ".pb");
    GtfsBundle gtfsBundle = new GtfsBundle(gtfs);
    feedId = new GtfsFeedId.Builder().id("FEED").build();
    gtfsBundle.setFeedId(feedId);
    List<GtfsBundle> gtfsBundleList = Collections.singletonList(gtfsBundle);

    alertsUpdateHandler = new AlertsUpdateHandler();
    var deduplicator = new Deduplicator();
    graph = new Graph(deduplicator);
    transitModel = new TransitModel(new StopModel(), deduplicator);

    GtfsModule gtfsGraphBuilderImpl = new GtfsModule(
      gtfsBundleList,
      transitModel,
      graph,
      ServiceDateInterval.unbounded()
    );

    gtfsGraphBuilderImpl.buildGraph();
    transitModel.index();
    graph.index(transitModel.getStopModel());
    serverContext = TestServerContext.createServerContext(graph, transitModel);
    timetableSnapshotSource =
      new TimetableSnapshotSource(
        TimetableSnapshotSourceParameters.DEFAULT
          .withPurgeExpiredData(true)
          .withMaxSnapshotFrequency(Duration.ZERO),
        transitModel
      );
    alertPatchServiceImpl = new TransitAlertServiceImpl(transitModel);
    alertsUpdateHandler.setTransitAlertService(alertPatchServiceImpl);
    alertsUpdateHandler.setFeedId(feedId.getId());

    try {
      InputStream inputStream = new FileInputStream(gtfsRealTime);
      FeedMessage feedMessage = FeedMessage.PARSER.parseFrom(inputStream);
      List<FeedEntity> feedEntityList = feedMessage.getEntityList();
      List<TripUpdate> updates = new ArrayList<>(feedEntityList.size());
      for (FeedEntity feedEntity : feedEntityList) {
        updates.add(feedEntity.getTripUpdate());
      }
      timetableSnapshotSource.applyTripUpdates(
        null,
        REQUIRED_NO_DATA,
        UpdateIncrementality.DIFFERENTIAL,
        updates,
        feedId.getId()
      );
      alertsUpdateHandler.update(feedMessage);
    } catch (Exception exception) {}
  }
}
