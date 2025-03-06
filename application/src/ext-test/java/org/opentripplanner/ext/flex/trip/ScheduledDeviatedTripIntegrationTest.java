package org.opentripplanner.ext.flex.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.test.support.PolylineAssert.assertThatPolylinesAreEqual;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.TestServerContext;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.DecorateWithFare;
import org.opentripplanner.ext.flex.FlexIntegrationTestData;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.ValidateAndInterpolateStopTimesForEachTrip;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.algorithm.raptoradapter.router.TransitRouter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.vertex.StreetLocation;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriorityService;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
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
  static TimetableRepository timetableRepository;

  float delta = 0.01f;

  @Test
  void parseCobbCountyAsScheduledDeviatedTrip() {
    var flexTrips = timetableRepository.getAllFlexTrips();
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

  @Test
  void calculateDirectFare() {
    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, true));
    var trip = getFlexTrip();

    var from = getNearbyStop(trip, "from-stop");
    var to = getNearbyStop(trip, "to-stop");

    var router = new FlexRouter(
      graph,
      new DefaultTransitService(timetableRepository),
      FlexParameters.defaultValues(),
      OffsetDateTime.parse("2021-11-12T10:15:24-05:00").toInstant(),
      null,
      1,
      1,
      List.of(from),
      List.of(to)
    );

    var filter = new DecorateWithFare(graph.getFareService());

    var itineraries = router
      .createFlexOnlyItineraries(false)
      .stream()
      .peek(filter::decorate)
      .toList();

    var itinerary = itineraries.getFirst();

    assertFalse(itinerary.getFares().getLegProducts().isEmpty());

    OTPFeature.enableFeatures(Map.of(OTPFeature.FlexRouting, false));
  }

  /**
   * Trips which consist of flex and fixed-schedule stops should work in transit mode.
   * <p>
   * The flex stops will show up as intermediate stops (without a departure/arrival time) but you
   * cannot board or alight.
   */
  @Test
  void flexTripInTransitMode() {
    var feedId = timetableRepository.getFeedIds().iterator().next();

    var serverContext = TestServerContext.createServerContext(graph, timetableRepository);

    // from zone 3 to zone 2
    var from = GenericLocation.fromStopId("Transfer Point for Route 30", feedId, "cujv");
    var to = GenericLocation.fromStopId(
      "Zone 1 - PUBLIX Super Market,Zone 1 Collection Point",
      feedId,
      "yz85"
    );

    var itineraries = getItineraries(from, to, serverContext);

    assertEquals(2, itineraries.size());

    var itin = itineraries.get(0);
    var leg = itin.getLegs().get(0);

    assertEquals("cujv", leg.getFrom().stop.getId().getId());
    assertEquals("yz85", leg.getTo().stop.getId().getId());

    var intermediateStops = leg.getIntermediateStops();
    assertEquals(1, intermediateStops.size());
    assertEquals("zone_1", intermediateStops.get(0).place.stop.getId().getId());

    EncodedPolyline legGeometry = EncodedPolyline.encode(leg.getLegGeometry());
    assertThatPolylinesAreEqual(
      legGeometry.points(),
      "kfsmEjojcOa@eBRKfBfHR|ALjBBhVArMG|OCrEGx@OhAKj@a@tAe@hA]l@MPgAnAgw@nr@cDxCm@t@c@t@c@x@_@~@]pAyAdIoAhG}@lE{AzHWhAtt@t~Aj@tAb@~AXdBHn@FlBC`CKnA_@nC{CjOa@dCOlAEz@E|BRtUCbCQ~CWjD??qBvXBl@kBvWOzAc@dDOx@sHv]aIG?q@@c@ZaB\\mA"
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
    var feedId = timetableRepository.getFeedIds().iterator().next();
    var pattern = timetableRepository.getTripPatternForId(new FeedScopedId(feedId, "090z:0:01"));

    assertEquals(3, pattern.numberOfStops());

    var tripTimes = pattern.getScheduledTimetable().getTripTimes().getFirst();
    var arrivalTime = tripTimes.getArrivalTime(1);

    assertEquals(StopTime.MISSING_VALUE, arrivalTime);
  }

  @BeforeAll
  static void setup() {
    TestOtpModel model = FlexIntegrationTestData.cobbFlexGtfs();
    graph = model.graph();
    timetableRepository = model.timetableRepository();
  }

  private static List<Itinerary> getItineraries(
    GenericLocation from,
    GenericLocation to,
    OtpServerRequestContext serverContext
  ) {
    var zoneId = ZoneIds.NEW_YORK;
    RouteRequest request = new RouteRequest();
    request.journey().transit().setFilters(List.of(AllowAllTransitFilter.of()));
    var dateTime = LocalDateTime.of(2021, Month.DECEMBER, 16, 12, 0).atZone(zoneId);
    request.setDateTime(dateTime.toInstant());
    request.setFrom(from);
    request.setTo(to);

    var transitStartOfTime = ServiceDateUtils.asStartOfService(request.dateTime(), zoneId);
    var additionalSearchDays = AdditionalSearchDays.defaults(dateTime);
    var result = TransitRouter.route(
      request,
      serverContext,
      TransitGroupPriorityService.empty(),
      transitStartOfTime,
      additionalSearchDays,
      new DebugTimingAggregator()
    );

    return result.getItineraries();
  }

  private static NearbyStop getNearbyStop(FlexTrip<?, ?> trip, String id) {
    // getStops() returns a set of stops and the order doesn't correspond to the stop times
    // of the trip
    var stopLocation = trip
      .getStops()
      .stream()
      .filter(s -> s instanceof AreaStop)
      .findFirst()
      .orElseThrow();

    return new NearbyStop(
      stopLocation,
      0,
      List.of(),
      new State(
        new StreetLocation(id, new Coordinate(0, 0), I18NString.of(id)),
        StreetSearchRequest.of().build()
      )
    );
  }

  private static FlexTrip<?, ?> getFlexTrip() {
    var feedId = timetableRepository.getFeedIds().iterator().next();
    var tripId = new FeedScopedId(feedId, "a326c618-d42c-4bd1-9624-c314fbf8ecd8");
    return timetableRepository.getFlexTrip(tripId);
  }
}
