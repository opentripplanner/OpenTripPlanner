package org.opentripplanner.apis.transmodel.mapping;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.framework.time.TimeUtils.time;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import graphql.ExecutionInput;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.micrometer.core.instrument.Metrics;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.ext.emissions.DefaultEmissionsService;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.api.request.PassThroughPoint;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.street.service.DefaultStreetLimitationParametersService;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

public class TripRequestMapperTest implements PlanTestConstants {

  private static TransitModelForTest TEST_MODEL = TransitModelForTest.of();

  static final TransmodelRequestContext context;
  private static final Duration MAX_FLEXIBLE = Duration.ofMinutes(20);

  private static final Function<StopLocation, String> STOP_TO_ID = s -> s.getId().toString();

  private static final Route route1 = TransitModelForTest.route("route1").build();
  private static final Route route2 = TransitModelForTest.route("route2").build();

  private static final RegularStop stop1 = TEST_MODEL.stop("ST:stop1", 1, 1).build();
  private static final RegularStop stop2 = TEST_MODEL.stop("ST:stop2", 2, 1).build();
  private static final RegularStop stop3 = TEST_MODEL.stop("ST:stop3", 3, 1).build();

  static {
    var graph = new Graph();
    var itinerary = newItinerary(Place.forStop(stop1), time("11:00"))
      .bus(route1, 1, time("11:05"), time("11:20"), Place.forStop(stop2))
      .bus(route2, 2, time("11:20"), time("11:40"), Place.forStop(stop3))
      .build();
    var patterns = itineraryPatterns(itinerary);
    var stopModel = TEST_MODEL
      .stopModelBuilder()
      .withRegularStop(stop1)
      .withRegularStop(stop2)
      .withRegularStop(stop3)
      .build();

    var transitModel = new TransitModel(stopModel, new Deduplicator());
    transitModel.initTimeZone(ZoneIds.STOCKHOLM);
    var calendarServiceData = new CalendarServiceData();
    LocalDate serviceDate = itinerary.startTime().toLocalDate();
    patterns.forEach(pattern -> {
      transitModel.addTripPattern(pattern.getId(), pattern);
      final int serviceCode = pattern.getScheduledTimetable().getTripTimes(0).getServiceCode();
      transitModel.getServiceCodes().put(pattern.getId(), serviceCode);
      calendarServiceData.putServiceDatesForServiceId(pattern.getId(), List.of(serviceDate));
    });

    transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);
    transitModel.index();
    final var transitService = new DefaultTransitService(transitModel);
    var defaultRequest = new RouteRequest();

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

    context =
      new TransmodelRequestContext(
        DefaultServerRequestContext.create(
          RouterConfig.DEFAULT.transitTuningConfig(),
          defaultRequest,
          RaptorConfig.defaultConfigForTest(),
          graph,
          transitService,
          Metrics.globalRegistry,
          RouterConfig.DEFAULT.vectorTileConfig(),
          new DefaultWorldEnvelopeService(new DefaultWorldEnvelopeRepository()),
          new DefaultRealtimeVehicleService(transitService),
          new DefaultVehicleRentalService(),
          new DefaultEmissionsService(new EmissionsDataModel()),
          RouterConfig.DEFAULT.flexConfig(),
          List.of(),
          null,
          new DefaultStreetLimitationParametersService(new StreetLimitationParameters()),
          null
        ),
        null,
        transitService
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
    var defaultValue = StreetPreferences.DEFAULT
      .accessEgress()
      .maxDuration()
      .valueOf(StreetMode.WALK);
    var duration = List.of(
      Map.of("streetMode", StreetMode.WALK, "duration", defaultValue.plusSeconds(1))
    );

    Map<String, Object> arguments = Map.of("maxAccessEgressDurationForMode", duration);

    assertThrows(
      IllegalArgumentException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testMaxAccessEgressDurationForFlexWithTooLongDuration() {
    Map<String, Object> arguments = Map.of(
      "maxAccessEgressDurationForMode",
      List.of(Map.of("streetMode", StreetMode.FLEXIBLE, "duration", MAX_FLEXIBLE.plusSeconds(1)))
    );
    assertThrows(
      IllegalArgumentException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testMaxDirectDurationValidation() {
    var defaultValue = StreetPreferences.DEFAULT.maxDirectDuration().valueOf(StreetMode.WALK);
    var duration = List.of(
      Map.of("streetMode", StreetMode.WALK, "duration", defaultValue.plusSeconds(1))
    );

    Map<String, Object> arguments = Map.of("maxDirectDurationForMode", duration);

    assertThrows(
      IllegalArgumentException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
    );
  }

  @Test
  public void testMaxDirectDurationForFlexWithTooLongDuration() {
    Map<String, Object> arguments = Map.of(
      "maxDirectDurationForMode",
      List.of(Map.of("streetMode", StreetMode.FLEXIBLE, "duration", MAX_FLEXIBLE.plusSeconds(1)))
    );
    assertThrows(
      IllegalArgumentException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
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
  void testPassThroughPoints() {
    TransitIdMapper.clearFixedFeedId();

    final List<String> PTP1 = List.of(stop1, stop2, stop3).stream().map(STOP_TO_ID).toList();
    final List<String> PTP2 = List.of(stop2, stop3, stop1).stream().map(STOP_TO_ID).toList();
    final Map<String, Object> arguments = Map.of(
      "passThroughPoints",
      List.of(Map.of("name", "PTP1", "placeIds", PTP1), Map.of("placeIds", PTP2, "name", "PTP2"))
    );

    final List<PassThroughPoint> points = TripRequestMapper
      .createRequest(executionContext(arguments))
      .getPassThroughPoints();
    assertEquals(PTP1, points.get(0).stopLocations().stream().map(STOP_TO_ID).toList());
    assertEquals("PTP1", points.get(0).name());
    assertEquals(PTP2, points.get(1).stopLocations().stream().map(STOP_TO_ID).toList());
    assertEquals("PTP2", points.get(1).name());
  }

  @Test
  void testPassThroughPointsNoMatch() {
    TransitIdMapper.clearFixedFeedId();

    final Map<String, Object> arguments = Map.of(
      "passThroughPoints",
      List.of(Map.of("placeIds", List.of("F:XX:NonExisting")))
    );

    final RuntimeException ex = assertThrows(
      RuntimeException.class,
      () -> TripRequestMapper.createRequest(executionContext(arguments))
    );
    assertEquals("No match for F:XX:NonExisting.", ex.getMessage());
  }

  private DataFetchingEnvironment executionContext(Map<String, Object> arguments) {
    ExecutionInput executionInput = ExecutionInput
      .newExecutionInput()
      .query("")
      .operationName("trip")
      .context(context)
      .locale(Locale.ENGLISH)
      .build();

    var executionContext = newExecutionContextBuilder()
      .executionInput(executionInput)
      .executionId(ExecutionId.from(this.getClass().getName()))
      .build();

    var env = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .context(context)
      .arguments(arguments)
      .build();

    return env;
  }

  private static List<TripPattern> itineraryPatterns(final Itinerary itinerary) {
    return itinerary
      .getLegs()
      .stream()
      .filter(Leg::isScheduledTransitLeg)
      .map(Leg::asScheduledTransitLeg)
      .map(ScheduledTransitLeg::getTripPattern)
      .collect(toList());
  }
}
