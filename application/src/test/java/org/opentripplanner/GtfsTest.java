package org.opentripplanner;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.street.model.StreetMode.NOT_SET;
import static org.opentripplanner.street.model.StreetMode.WALK;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareService;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundleTestFactory;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorTransitDataMapper;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterService;
import org.opentripplanner.updater.alert.gtfs.AlertsUpdateHandler;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.gtfs.GtfsRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.gtfs.interpolation.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.interpolation.ForwardsDelayPropagationType;
import org.opentripplanner.utils.lang.RunnableUtils;

/** Common base class for many test classes which need to load a GTFS feed in preparation for tests. */
public abstract class GtfsTest {

  protected static final String FEED_ID = "FEED";

  public Graph graph;
  public TransitRepository transitRepository;

  AlertsUpdateHandler alertsUpdateHandler;
  GtfsRealTimeTripUpdateAdapter tripUpdateAdapter;
  TransitAlertServiceImpl alertPatchServiceImpl;
  public OtpServerRequestContext serverContext;

  public abstract String getFeedName();

  public Itinerary plan(
    long dateTime,
    String fromVertex,
    String toVertex,
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
    var builder = RouteRequest.of()
      .withNumItineraries(1)
      .withArriveBy(dateTime < 0)
      .withDateTime(Instant.ofEpochSecond(Math.abs(dateTime)));

    if (fromVertex != null && !fromVertex.isEmpty()) {
      builder.withFrom(GenericLocation.fromStopId(FeedScopedId.of(FEED_ID, fromVertex)));
    }
    if (toVertex != null && !toVertex.isEmpty()) {
      builder.withTo(GenericLocation.fromStopId(FeedScopedId.of(FEED_ID, toVertex)));
    }
    builder.withJourney(journeyBuilder -> {
      journeyBuilder.withWheelchair(wheelchairAccessible);

      var requestModesBuilder = RequestModes.of()
        .withDirectMode(NOT_SET)
        .withAccessMode(WALK)
        .withTransferMode(WALK)
        .withEgressMode(WALK);

      journeyBuilder.withModes(requestModesBuilder.build());

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
        List<FeedScopedId> routeIds = List.of(new FeedScopedId(FEED_ID, excludedRoute));
        filterRequestBuilder.addNot(SelectRequest.of().withRoutes(routeIds).build());
      }

      journeyBuilder.withTransit(b -> b.withFilters(List.of(filterRequestBuilder.build())));
    });

    // Init preferences
    builder.withPreferences(preferences -> {
      preferences.withTransfer(tx -> {
        tx.withSlack(Duration.ZERO);
        tx.withWaitReluctance(1);
        tx.withCost(preferLeastTransfers ? 300 : 0);
      });

      // The walk board cost is set low because it interferes with test 2c1.
      // As long as boarding has a very low cost, waiting should not be "better" than riding
      // since this makes interlining _worse_ than alighting and re-boarding the same line.
      // TODO rethink whether it makes sense to weight waiting to board _less_ than 1.
      preferences.withWalk(w -> w.withBoardCost(30));
      preferences.withTransit(tr -> tr.withOtherThanPreferredRoutesPenalty(0));
    });

    // Route
    RoutingResponse res = serverContext.routingService().route(builder.buildRequest());

    // Assert itineraries
    List<Itinerary> itineraries = res.getTripPlan().itineraries;
    // Stored in instance field for use in individual tests
    Itinerary itinerary = itineraries.get(0);

    assertEquals(legCount, itinerary.legs().size());

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
    assertEquals(startTime, leg.startTime().toInstant().toEpochMilli());
    assertEquals(endTime, leg.endTime().toInstant().toEpochMilli());
    assertEquals(toStopId, leg.to().stop.getId().getId());
    assertEquals(FEED_ID, leg.to().stop.getId().getFeedId());
    if (fromStopId != null) {
      assertEquals(FEED_ID, leg.from().stop.getId().getFeedId());
      assertEquals(fromStopId, leg.from().stop.getId().getId());
    } else {
      assertNull(leg.from().stop.getId());
    }
    if (alert != null) {
      assertNotNull(leg.listStreetNotes());
      assertEquals(1, leg.listStreetNotes().size());
      assertEquals(alert, leg.listStreetNotes().iterator().next().note.toString());
    } else {
      assertThat(leg.listStreetNotes()).isEmpty();
    }
  }

  @BeforeEach
  protected void setUp() throws Exception {
    File gtfs = new File("src/test/resources/" + getFeedName());
    File gtfsRealTime = new File("src/test/resources/" + getFeedName() + ".pb");

    GtfsBundle gtfsBundle = GtfsBundleTestFactory.forTest(gtfs, FEED_ID);
    List<GtfsBundle> gtfsBundleList = List.of(gtfsBundle);

    alertsUpdateHandler = new AlertsUpdateHandler(false);
    graph = new Graph();
    transitRepository = new TransitRepository(new SiteRepository());
    transitRepository.initUpdaterManager(
      new GraphUpdaterManager(GraphWriterService.NOOP, RunnableUtils.NOOP, List.of())
    );
    TransferRepository transferRepository = TransferServiceTestFactory.defaultTransferRepository();

    GtfsModule gtfsGraphBuilderImpl = GtfsModule.forTest(
      gtfsBundleList,
      transitRepository,
      graph,
      LocalDateRange.ofUnbounded()
    );

    gtfsGraphBuilderImpl.buildGraph();
    transitRepository.index();
    graph.index();

    TransitTuningParameters tuningParameters = RouterConfig.DEFAULT.transitTuningConfig();
    var scheduledRaptorData = RaptorTransitDataMapper.map(
      tuningParameters,
      transitRepository,
      transferRepository
    );
    transitRepository.initRaptorTransitData(scheduledRaptorData);
    var registry =
      org.opentripplanner.framework.transaction.internal.TransactionFactory.createRepositoryRegistry();
    var timetableSnapshot = new org.opentripplanner.transit.repository.DefaultTimetableRepository(
      new RaptorTransitData(scheduledRaptorData),
      transitRepository.copyTripCalendarForRealTimeUpdates()
    );
    var timetableHandle = registry.registerRepositorySnapshot(
      timetableSnapshot,
      new org.opentripplanner.transit.repository.TimetableRepositoryLifecycle(
        timetableSnapshot,
        false,
        LocalDate::now
      )
    );
    var updateManager =
      org.opentripplanner.framework.transaction.internal.TransactionFactory.createUpdateManagerWithAtomicCommits(
        "test",
        registry,
        java.util.concurrent.Executors.defaultThreadFactory()
      );

    tripUpdateAdapter = new GtfsRealTimeTripUpdateAdapter(
      transitRepository,
      new Deduplicator(),
      LocalDate::now
    );
    alertPatchServiceImpl = new TransitAlertServiceImpl(transitRepository);
    alertsUpdateHandler.setTransitAlertService(alertPatchServiceImpl);
    alertsUpdateHandler.setFeedId(FEED_ID);

    try {
      InputStream inputStream = new FileInputStream(gtfsRealTime);
      FeedMessage feedMessage = FeedMessage.parseFrom(inputStream);
      List<FeedEntity> feedEntityList = feedMessage.getEntityList();
      List<TripUpdate> updates = new ArrayList<>(feedEntityList.size());
      for (FeedEntity feedEntity : feedEntityList) {
        updates.add(feedEntity.getTripUpdate());
      }
      updateManager
        .submit(ctx -> {
          var buffer = ctx.repository(timetableHandle);
          tripUpdateAdapter
            .forUpdate(buffer)
            .applyTripUpdates(
              null,
              ForwardsDelayPropagationType.DEFAULT,
              BackwardsDelayPropagationType.REQUIRED_NO_DATA,
              UpdateIncrementality.DIFFERENTIAL,
              updates,
              FEED_ID
            );
        })
        .get();
      alertsUpdateHandler.update(feedMessage, null);
    } catch (FileNotFoundException _) {} catch (Exception e) {
      throw new RuntimeException(e);
    }
    serverContext = TestServerContext.createServerContext(
      graph,
      transitRepository,
      transferRepository,
      new DefaultFareService(),
      timetableHandle,
      registry,
      null,
      null
    );
  }
}
