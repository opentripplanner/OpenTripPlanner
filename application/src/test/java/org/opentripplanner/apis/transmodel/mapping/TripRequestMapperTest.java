package org.opentripplanner.apis.transmodel.mapping;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.utils.time.TimeUtils.time;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.TestServerContext;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TimetableRepository;

public class TripRequestMapperTest implements PlanTestConstants {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final Duration MAX_FLEXIBLE = Duration.ofMinutes(20);
  private static final Function<StopLocation, String> STOP_TO_ID = s -> s.getId().toString();

  private static final Route route1 = TimetableRepositoryForTest.route("route1").build();
  private static final Route route2 = TimetableRepositoryForTest.route("route2").build();

  private static final RegularStop stop1 = TEST_MODEL.stop("ST:stop1", 1, 1).build();
  private static final RegularStop stop2 = TEST_MODEL.stop("ST:stop2", 2, 1).build();
  private static final RegularStop stop3 = TEST_MODEL.stop("ST:stop3", 3, 1).build();

  private static final Graph graph = new Graph();
  private static final TimetableRepository timetableRepository;

  private TransmodelRequestContext context;

  static {
    var itinerary = newItinerary(Place.forStop(stop1), time("11:00"))
      .bus(route1, 1, time("11:05"), time("11:20"), Place.forStop(stop2))
      .bus(route2, 2, time("11:20"), time("11:40"), Place.forStop(stop3))
      .build();
    var patterns = itineraryPatterns(itinerary);
    var siteRepository = TEST_MODEL.siteRepositoryBuilder()
      .withRegularStop(stop1)
      .withRegularStop(stop2)
      .withRegularStop(stop3)
      .build();

    timetableRepository = new TimetableRepository(siteRepository, new Deduplicator());
    timetableRepository.initTimeZone(ZoneIds.STOCKHOLM);
    var calendarServiceData = new CalendarServiceData();
    LocalDate serviceDate = itinerary.startTime().toLocalDate();
    patterns.forEach(pattern -> {
      timetableRepository.addTripPattern(pattern.getId(), pattern);
      final int serviceCode = pattern
        .getScheduledTimetable()
        .getTripTimes()
        .getFirst()
        .getServiceCode();
      timetableRepository.getServiceCodes().put(pattern.getId(), serviceCode);
      calendarServiceData.putServiceDatesForServiceId(pattern.getId(), List.of(serviceDate));
    });

    timetableRepository.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );
    timetableRepository.index();
  }

  @BeforeEach
  void setup() {
    final RouteRequest defaultRequest = new RouteRequest();

    // Change defaults for FLEXIBLE to a lower value than the default 45m. This should restrict the
    // input to be less than 20m, not 45m.
    defaultRequest.withPreferences(pb ->
      pb.withStreet(sp ->
        sp
          .withAccessEgress(ae -> ae.withMaxDuration(b -> b.with(StreetMode.FLEXIBLE, MAX_FLEXIBLE))
          )
          .withMaxDirectDuration(b -> b.with(StreetMode.FLEXIBLE, MAX_FLEXIBLE))
      )
    );

    var otpServerRequestContext = TestServerContext.createServerContext(
      graph,
      timetableRepository,
      null,
      defaultRequest
    );

    context = new TransmodelRequestContext(
      otpServerRequestContext,
      otpServerRequestContext.routingService(),
      otpServerRequestContext.transitService()
    );
  }

  private static final List<Map<String, Object>> DURATIONS = List.of(
    Map.of("streetMode", StreetMode.WALK, "duration", Duration.ofMinutes(30)),
    Map.of("streetMode", StreetMode.BIKE, "duration", Duration.ofMinutes(31)),
    Map.of("streetMode", StreetMode.BIKE_TO_PARK, "duration", Duration.ofMinutes(32)),
    Map.of("streetMode", StreetMode.BIKE_RENTAL, "duration", Duration.ofMinutes(33)),
    Map.of("streetMode", StreetMode.SCOOTER_RENTAL, "duration", Duration.ofMinutes(34)),
    Map.of("streetMode", StreetMode.CAR, "duration", Duration.ofMinutes(35)),
    Map.of("streetMode", StreetMode.CAR_TO_PARK, "duration", Duration.ofMinutes(36)),
    Map.of("streetMode", StreetMode.CAR_PICKUP, "duration", Duration.ofMinutes(30)),
    Map.of("streetMode", StreetMode.CAR_RENTAL, "duration", Duration.ofMinutes(38)),
    // Same as max value for FLEXIBLE
    Map.of("streetMode", StreetMode.FLEXIBLE, "duration", MAX_FLEXIBLE)
  );

  @Test
  public void testMaxAccessEgressDurationForMode() {
    Map<String, Object> arguments = Map.of("maxAccessEgressDurationForMode", DURATIONS);

    var routeRequest = TripRequestMapper.createRequest(executionContext(arguments));
    assertNotNull(routeRequest);
    var preferences = routeRequest.preferences();
    assertNotNull(preferences);
    var streetPreferences = preferences.street();
    assertNotNull(streetPreferences);
    var maxAccessEgressDuration = streetPreferences.accessEgress().maxDuration();
    assertNotNull(maxAccessEgressDuration);

    for (var entry : DURATIONS) {
      var mode = (StreetMode) entry.get("streetMode");
      var expected = (Duration) entry.get("duration");
      assertEquals(expected, maxAccessEgressDuration.valueOf(mode), mode.name());
    }
  }

  @Test
  public void testMaxDirectDurationForMode() {
    Map<String, Object> arguments = Map.of("maxDirectDurationForMode", DURATIONS);

    var routeRequest = TripRequestMapper.createRequest(executionContext(arguments));
    assertNotNull(routeRequest);
    var preferences = routeRequest.preferences();
    assertNotNull(preferences);
    var streetPreferences = preferences.street();
    assertNotNull(streetPreferences);
    var maxDirectDuration = streetPreferences.maxDirectDuration();
    assertNotNull(maxDirectDuration);

    for (var entry : DURATIONS) {
      var streetMode = (StreetMode) entry.get("streetMode");
      var duration = (Duration) entry.get("duration");

      assertEquals(maxDirectDuration.valueOf(streetMode), duration);
    }
  }

  @Test
  public void testMaxAccessEgressDurationValidation() {
    var defaultValue = StreetPreferences.DEFAULT.accessEgress()
      .maxDuration()
      .valueOf(StreetMode.WALK);
    var duration = List.of(
      Map.of("streetMode", StreetMode.WALK, "duration", defaultValue.plusSeconds(1))
    );

    Map<String, Object> arguments = Map.of("maxAccessEgressDurationForMode", duration);

    assertThrows(IllegalArgumentException.class, () ->
      TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testMaxAccessEgressDurationForFlexWithTooLongDuration() {
    Map<String, Object> arguments = Map.of(
      "maxAccessEgressDurationForMode",
      List.of(Map.of("streetMode", StreetMode.FLEXIBLE, "duration", MAX_FLEXIBLE.plusSeconds(1)))
    );
    assertThrows(IllegalArgumentException.class, () ->
      TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testMaxDirectDurationValidation() {
    var defaultValue = StreetPreferences.DEFAULT.maxDirectDuration().valueOf(StreetMode.WALK);
    var duration = List.of(
      Map.of("streetMode", StreetMode.WALK, "duration", defaultValue.plusSeconds(1))
    );

    Map<String, Object> arguments = Map.of("maxDirectDurationForMode", duration);

    assertThrows(IllegalArgumentException.class, () ->
      TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testMaxDirectDurationForFlexWithTooLongDuration() {
    Map<String, Object> arguments = Map.of(
      "maxDirectDurationForMode",
      List.of(Map.of("streetMode", StreetMode.FLEXIBLE, "duration", MAX_FLEXIBLE.plusSeconds(1)))
    );
    assertThrows(IllegalArgumentException.class, () ->
      TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testBikeTriangleFactors() {
    Map<String, Object> arguments = Map.of(
      "bicycleOptimisationMethod",
      VehicleRoutingOptimizeType.TRIANGLE,
      "triangleFactors",
      Map.of("safety", 0.1, "slope", 0.1, "time", 0.8)
    );

    var req1 = TripRequestMapper.createRequest(executionContext(arguments));

    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, req1.preferences().bike().optimizeType());
    assertEquals(
      new TimeSlopeSafetyTriangle(0.8, 0.1, 0.1),
      req1.preferences().bike().optimizeTriangle()
    );
  }

  @Test
  void testDefaultTriangleFactors() {
    var req2 = TripRequestMapper.createRequest(executionContext(Map.of()));
    assertEquals(VehicleRoutingOptimizeType.SAFE_STREETS, req2.preferences().bike().optimizeType());
    assertEquals(TimeSlopeSafetyTriangle.DEFAULT, req2.preferences().bike().optimizeTriangle());
  }

  static Stream<Arguments> noTriangleCases() {
    return VehicleRoutingOptimizeType.nonTriangleValues().stream().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("noTriangleCases")
  public void testBikeTriangleFactorsHasNoEffect(VehicleRoutingOptimizeType bot) {
    Map<String, Object> arguments = Map.of(
      "bicycleOptimisationMethod",
      bot,
      "triangleFactors",
      Map.of("safety", 0.1, "slope", 0.1, "time", 0.8)
    );

    var req1 = TripRequestMapper.createRequest(executionContext(arguments));

    assertEquals(bot, req1.preferences().bike().optimizeType());
    assertEquals(TimeSlopeSafetyTriangle.DEFAULT, req1.preferences().bike().optimizeTriangle());
  }

  @Test
  void testViaLocations() {
    TransitIdMapper.clearFixedFeedId();

    final List<String> PTP1 = Stream.of(stop1, stop2, stop3).map(STOP_TO_ID).toList();
    final List<String> PTP2 = Stream.of(stop3, stop2).map(STOP_TO_ID).toList();
    final Map<String, Object> arguments = Map.of(
      "passThroughPoints",
      List.of(Map.of("name", "PTP1", "placeIds", PTP1), Map.of("placeIds", PTP2, "name", "PTP2"))
    );

    final List<ViaLocation> viaLocations = TripRequestMapper.createRequest(
      executionContext(arguments)
    ).getViaLocations();
    assertEquals(
      "PassThroughViaLocation{label: PTP1, stopLocationIds: [F:ST:stop1, F:ST:stop2, F:ST:stop3]}",
      viaLocations.get(0).toString()
    );
    assertEquals("PTP1", viaLocations.get(0).label());
    assertEquals(
      "PassThroughViaLocation{label: PTP2, stopLocationIds: [F:ST:stop3, F:ST:stop2]}",
      viaLocations.get(1).toString()
    );
    assertEquals("PTP2", viaLocations.get(1).label());
  }

  @Test
  public void testNoModes() {
    var req = TripRequestMapper.createRequest(executionContext(Map.of()));

    assertEquals(StreetMode.WALK, req.journey().access().mode());
    assertEquals(StreetMode.WALK, req.journey().egress().mode());
    assertEquals(StreetMode.WALK, req.journey().direct().mode());
    assertEquals(StreetMode.WALK, req.journey().transfer().mode());
  }

  @Test
  public void testEmptyModes() {
    Map<String, Object> arguments = Map.of("modes", Map.of());
    var req = TripRequestMapper.createRequest(executionContext(arguments));

    assertEquals(StreetMode.NOT_SET, req.journey().access().mode());
    assertEquals(StreetMode.NOT_SET, req.journey().egress().mode());
    assertEquals(StreetMode.NOT_SET, req.journey().direct().mode());
    assertEquals(StreetMode.WALK, req.journey().transfer().mode());
  }

  @Test
  public void testNullModes() {
    HashMap<Object, Object> modes = new HashMap<>();
    modes.put("accessMode", null);
    modes.put("egressMode", null);
    modes.put("directMode", null);
    Map<String, Object> arguments = Map.of("modes", modes);
    var req = TripRequestMapper.createRequest(executionContext(arguments));

    assertEquals(StreetMode.NOT_SET, req.journey().access().mode());
    assertEquals(StreetMode.NOT_SET, req.journey().egress().mode());
    assertEquals(StreetMode.NOT_SET, req.journey().direct().mode());
    assertEquals(StreetMode.WALK, req.journey().transfer().mode());
  }

  @Test
  public void testExplicitModes() {
    Map<String, Object> arguments = Map.of(
      "modes",
      Map.of(
        "accessMode",
        StreetMode.SCOOTER_RENTAL,
        "egressMode",
        StreetMode.BIKE_RENTAL,
        "directMode",
        StreetMode.BIKE_TO_PARK
      )
    );
    var req = TripRequestMapper.createRequest(executionContext(arguments));

    assertEquals(StreetMode.SCOOTER_RENTAL, req.journey().access().mode());
    assertEquals(StreetMode.BIKE_RENTAL, req.journey().egress().mode());
    assertEquals(StreetMode.BIKE_TO_PARK, req.journey().direct().mode());
    assertEquals(StreetMode.WALK, req.journey().transfer().mode());
  }

  /**
   * This tests that both the new parameter name 'transferSlack` and the deprecated one
   * 'minimumTransferTime' (for backwards compatibility) are correctly mapped to the internal
   * transfer slack as a duration.
   */
  @ParameterizedTest
  @ValueSource(strings = { "transferSlack", "minimumTransferTime" })
  public void testBackwardsCompatibleTransferSlack(String name) {
    Map<String, Object> arguments = Map.of(name, 101);
    var req = TripRequestMapper.createRequest(executionContext(arguments));
    assertEquals(Duration.ofSeconds(101), req.preferences().transfer().slack());
  }

  @Test
  public void testExplicitModesBikeAccess() {
    Map<String, Object> arguments = Map.of("modes", Map.of("accessMode", StreetMode.BIKE));
    var req = TripRequestMapper.createRequest(executionContext(arguments));

    assertEquals(StreetMode.BIKE, req.journey().access().mode());
    assertEquals(StreetMode.NOT_SET, req.journey().egress().mode());
    assertEquals(StreetMode.NOT_SET, req.journey().direct().mode());
    assertEquals(StreetMode.BIKE, req.journey().transfer().mode());
  }

  private DataFetchingEnvironment executionContext(Map<String, Object> arguments) {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
      .query("")
      .operationName("trip")
      .context(context)
      .locale(Locale.ENGLISH)
      .build();

    var executionContext = newExecutionContextBuilder()
      .executionInput(executionInput)
      .executionId(ExecutionId.from(this.getClass().getName()))
      .build();

    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .context(context)
      .arguments(arguments)
      .build();

    return env;
  }

  private static List<TripPattern> itineraryPatterns(final Itinerary itinerary) {
    return itinerary
      .legs()
      .stream()
      .filter(Leg::isScheduledTransitLeg)
      .map(Leg::asScheduledTransitLeg)
      .map(ScheduledTransitLeg::getTripPattern)
      .collect(toList());
  }
}
