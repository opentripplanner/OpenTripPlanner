package org.opentripplanner.ext.flex.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.test.support.PolylineAssert.assertThatPolylinesAreEqual;

import io.micrometer.core.instrument.Metrics;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.api.model.geometry.EncodedPolyline;
import org.opentripplanner.apis.transmodel.model.TripTimeOnDateHelper;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.FlexIntegrationTestData;
import org.opentripplanner.graph_builder.module.ValidateAndInterpolateStopTimesForEachTrip;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.algorithm.raptoradapter.router.TransitRouter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.mapping.LinkingContextRequestMapper;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriorityService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * This tests that the feed for the Cobb County Flex service is processed correctly. This service
 * contains both flex zones but also scheduled stops. Inside the zone, passengers can get on or off
 * anywhere, so there it works more like a taxi.
 * <p>
 * This service is not being offered anymore, but we keep the test because others of the same
 * type still exist.
 */
class ScheduledDeviatedTripIntegrationTest {

  static Graph graph;
  static TransitRepository transitRepository;
  static TransferRepository transferRepository;

  float delta = 0.01f;

  @Test
  void parseCobbCountyAsScheduledDeviatedTrip() {
    var flexTrips = transitRepository.getAllFlexTrips();
    assertFalse(flexTrips.isEmpty());
    assertEquals(72, flexTrips.size());

    assertEquals(
      Set.of(ScheduledDeviatedTrip.class),
      flexTrips.stream().map(FlexTrip::getClass).collect(Collectors.toSet())
    );

    var trip = getFlexTrip();
    var stop = trip
      .getStops()
      .stream()
      .filter(s -> s.getId().getId().equals("cujv"))
      .findFirst()
      .orElseThrow();
    assertEquals(33.85465, stop.getLat(), delta);
    assertEquals(-84.60039, stop.getLon(), delta);

    var flexZone = trip
      .getStops()
      .stream()
      .filter(s -> s.getId().getId().equals("zone_3"))
      .findFirst()
      .orElseThrow();
    assertEquals(33.825846635310214, flexZone.getLat(), delta);
    assertEquals(-84.63430143459385, flexZone.getLon(), delta);
  }

  /**
   * Trips which consist of flex and fixed-schedule stops should work in transit mode.
   * <p>
   * The flex stops will show up as intermediate stops (without a departure/arrival time) but you
   * cannot board or alight.
   */
  @Test
  void flexTripInTransitMode() {
    var feedId = transitRepository.getFeedIds().iterator().next();

    var transitService = TestServerContext.createTransitService(
      transitRepository,
      transferRepository
    );

    // from zone 3 to zone 2
    var from = GenericLocation.fromStopId(
      new FeedScopedId(feedId, "cujv"),
      "Transfer Point for Route 30"
    );
    var to = GenericLocation.fromStopId(
      new FeedScopedId(feedId, "yz85"),
      "Zone 1 - PUBLIX Super Market,Zone 1 Collection Point"
    );

    var itineraries = getItineraries(from, to, transitService);

    assertEquals(2, itineraries.size());

    var itin = itineraries.get(0);
    var leg = itin.legs().get(0);

    assertEquals("cujv", leg.from().stop.getId().getId());
    assertEquals("yz85", leg.to().stop.getId().getId());

    var intermediateStops = leg.listIntermediateStops();
    assertEquals(2, intermediateStops.size());
    assertEquals("zone_1", intermediateStops.get(0).place.stop.getId().getId());

    EncodedPolyline legGeometry = EncodedPolyline.of(leg.legGeometry());
    assertThatPolylinesAreEqual(
      legGeometry.points(),
      "kfsmEjojcOa@eBRKfBfHR|ALjBBhVArMG|OCrEGx@OhAKj@a@tAe@hA]l@MPgAnAgw@nr@cDxCm@t@c@t@c@x@_@~@]pAyAdIoAhG}@lE{AzHWhAtt@t~Aj@tAb@~AXdBHn@FlBC`CKnA_@nC{CjOa@dCOlAEz@E|BRtUCbCQ~CWjD??qBvXBl@kBvWOzAc@dDOx@sHv]aIG?q@@c@ZaB\\mA"
    );
  }

  /**
   * A flex service journey with fixed endpoints is routed as a regular scheduled leg, and its
   * flexible-area stop shows up as an intermediate stop. That stop has no scheduled
   * arrival/departure time (only a time window), so it must be excluded from the Transmodel
   * {@code intermediateEstimatedCalls}. Otherwise the {@link StopTime#MISSING_VALUE} placeholder
   * would be rendered as a bogus time (the day before the trip). See issue #7034.
   */
  @Test
  void intermediateEstimatedCallsSkipFlexWindowStops() {
    var feedId = transitRepository.getFeedIds().iterator().next();

    var transitService = TestServerContext.createTransitService(
      transitRepository,
      transferRepository
    );

    var from = GenericLocation.fromStopId(
      new FeedScopedId(feedId, "cujv"),
      "Transfer Point for Route 30"
    );
    var to = GenericLocation.fromStopId(
      new FeedScopedId(feedId, "yz85"),
      "Zone 1 - PUBLIX Super Market,Zone 1 Collection Point"
    );

    var leg = getItineraries(from, to, transitService).get(0).legs().get(0);

    // The flexible-area stop is exposed as an intermediate stop on the leg ...
    var intermediateStopIds = leg
      .listIntermediateStops()
      .stream()
      .map(s -> s.place.stop.getId().getId())
      .collect(Collectors.toList());
    assertTrue(intermediateStopIds.contains("zone_1"));

    // ... but it must not appear among the intermediate estimated calls, and every returned call
    // must carry a real scheduled time.
    var intermediateCalls = TripTimeOnDateHelper.getIntermediateTripTimeOnDatesForLeg(leg);
    assertTrue(intermediateCalls.stream().allMatch(TripTimeOnDate::hasScheduledTimes));
    assertFalse(
      intermediateCalls.stream().anyMatch(c -> c.getStop().getId().getId().equals("zone_1"))
    );
  }

  /**
   * We add flex trips, that can potentially not have a departure and arrival time, to the trip.
   * <p>
   * Normally these trip times are interpolated/repaired during the graph build but for flex this is
   * exactly what we don't want. Here we check that the interpolation process is skipped.
   *
   * @see ValidateAndInterpolateStopTimesForEachTrip#interpolateStopTimes(List)
   */
  @Test
  void shouldNotInterpolateFlexTimes() {
    var feedId = transitRepository.getFeedIds().iterator().next();
    var pattern = transitRepository.getTripPatternForId(new FeedScopedId(feedId, "090z:0:01"));

    assertEquals(4, pattern.numberOfStops());

    var tripTimes = pattern.getScheduledTimetable().getTripTimes().getFirst();
    var arrivalTime = tripTimes.getArrivalTime(1);

    assertEquals(StopTime.MISSING_VALUE, arrivalTime);
  }

  @BeforeAll
  static void setup() {
    TestOtpModel model = FlexIntegrationTestData.cobbFlexGtfs();
    graph = model.graph();
    transitRepository = model.transitRepository();
    transferRepository = TransferServiceTestFactory.defaultTransferRepository();
  }

  private static List<Itinerary> getItineraries(
    GenericLocation from,
    GenericLocation to,
    TransitService transitService
  ) {
    var zoneId = ZoneIds.NEW_YORK;
    var dateTime = LocalDateTime.of(2021, Month.DECEMBER, 16, 12, 0).atZone(zoneId);

    RouteRequest request = RouteRequest.of()
      .withDateTime(dateTime.toInstant())
      .withFrom(from)
      .withTo(to)
      .buildRequest();

    var transitStartOfTime = ServiceDateUtils.asStartOfService(request.dateTime(), zoneId);
    var additionalSearchDays = AdditionalSearchDays.defaults(dateTime);

    var vertexLinker = VertexLinkerTestFactory.of(graph);
    var linkingContextFactory = TestServerContext.createLinkingContextFactory(
      graph,
      vertexLinker,
      transitService
    );

    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var linkingRequest = LinkingContextRequestMapper.map(request);
      var linkingContext = linkingContextFactory.create(temporaryVerticesContainer, linkingRequest);
      var result = TransitRouter.route(
        request,
        transitService,
        graph,
        TestServerContext.createRaptorConfig(),
        Metrics.globalRegistry,
        TestServerContext.createStreetDetailsService(),
        TransferServiceTestFactory.transferService(transferRepository),
        VehicleRentalService.EMPTY,
        StreetLimitationParametersService.DEFAULT,
        RouterConfig.DEFAULT.flexParameters(),
        List.of(),
        null,
        null,
        TestServerContext.createViaTransferResolver(graph, transitService),
        TransitGroupPriorityService.empty(),
        transitStartOfTime,
        additionalSearchDays,
        new DebugTimingAggregator(),
        linkingContext,
        null
      );

      return result.getItineraries();
    }
  }

  private static FlexTrip<?, ?> getFlexTrip() {
    var feedId = transitRepository.getFeedIds().iterator().next();
    var tripId = new FeedScopedId(feedId, "a326c618-d42c-4bd1-9624-c314fbf8ecd8");
    return transitRepository.getFlexTrip(tripId);
  }
}
