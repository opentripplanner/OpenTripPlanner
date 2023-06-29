package org.opentripplanner.ext.legacygraphqlapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.model.plan.PlanTestConstants.A;
import static org.opentripplanner.model.plan.PlanTestConstants.B;
import static org.opentripplanner.model.plan.PlanTestConstants.C;
import static org.opentripplanner.model.plan.PlanTestConstants.D;
import static org.opentripplanner.model.plan.PlanTestConstants.D10m;
import static org.opentripplanner.model.plan.PlanTestConstants.E;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_01;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_15;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_30;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_50;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.test.support.JsonAssertions.assertEqualJson;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.FERRY;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nonnull;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.FaresToItineraryMapper;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.standalone.config.framework.json.JsonSupport;
import org.opentripplanner.test.support.FilePatternSource;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

@Execution(ExecutionMode.CONCURRENT)
class GraphQLIntegrationTest {

  static final Graph graph = new Graph();

  static final Instant ALERT_START_TIME = OffsetDateTime
    .parse("2023-02-15T12:03:28+01:00")
    .toInstant();
  static final Instant ALERT_END_TIME = ALERT_START_TIME.plus(1, ChronoUnit.DAYS);

  private static final LegacyGraphQLRequestContext context;

  private static final Deduplicator DEDUPLICATOR = new Deduplicator();

  static {
    graph
      .getVehicleParkingService()
      .updateVehicleParking(
        List.of(
          VehicleParking
            .builder()
            .id(id("parking-1"))
            .name(NonLocalizedString.ofNullable("parking"))
            .build()
        ),
        List.of()
      );

    var stopModel = StopModel.of();
    PlanTestConstants.listStops().forEach(sl -> stopModel.withRegularStop((RegularStop) sl));
    var transitModel = new TransitModel(stopModel.build(), DEDUPLICATOR);

    final TripPattern pattern = TransitModelForTest.pattern(BUS).build();
    var trip = TransitModelForTest.trip("123").build();
    var stopTimes = TransitModelForTest.stopTimesEvery5Minutes(3, trip, T11_00);
    var tripTimes = new TripTimes(trip, stopTimes, DEDUPLICATOR);
    pattern.add(tripTimes);

    transitModel.addTripPattern(id("pattern-1"), pattern);

    transitModel.initTimeZone(ZoneIds.BERLIN);
    transitModel.index();
    var routes = Arrays
      .stream(TransitMode.values())
      .sorted(Comparator.comparing(Enum::name))
      .map(m ->
        TransitModelForTest
          .route(m.name())
          .withMode(m)
          .withLongName(new NonLocalizedString("Long name for %s".formatted(m)))
          .build()
      )
      .toList();

    var busRoute = routes.stream().filter(r -> r.getMode().equals(BUS)).findFirst().get();

    routes.forEach(route -> transitModel.getTransitModelIndex().addRoutes(route));

    var step1 = walkStep("street");
    step1.setRelativeDirection(RelativeDirection.DEPART);
    step1.setAbsoluteDirection(20);
    var step2 = walkStep("elevator");
    step2.setRelativeDirection(RelativeDirection.ELEVATOR);

    Itinerary i1 = newItinerary(A, T11_00)
      .walk(20, B, List.of(step1, step2))
      .bus(busRoute, 122, T11_01, T11_15, C)
      .rail(439, T11_30, T11_50, D)
      .carHail(D10m, E)
      .build();

    var busLeg = i1.getTransitLeg(1);
    ScheduledTransitLeg railLeg = (ScheduledTransitLeg) i1.getTransitLeg(2);

    var fares = new ItineraryFares();
    fares.addFare(FareType.regular, Money.euros(3.1f));
    fares.addFareComponent(
      FareType.regular,
      List.of(new FareComponent(id("AB"), Money.euros(3.1f), List.of(busLeg)))
    );

    var dayPass = fareProduct("day-pass");
    fares.addItineraryProducts(List.of(dayPass));

    var singleTicket = fareProduct("single-ticket");
    fares.addFareProduct(railLeg, singleTicket);
    fares.addFareProduct(busLeg, singleTicket);
    i1.setFare(fares);

    i1.setFare(fares);
    FaresToItineraryMapper.addFaresToLegs(fares, i1);

    i1.setAccessibilityScore(0.5f);

    railLeg.withAccessibilityScore(.3f);

    var alert = TransitAlert
      .of(id("an-alert"))
      .withHeaderText(new NonLocalizedString("A header"))
      .withDescriptionText(new NonLocalizedString("A description"))
      .withUrl(new NonLocalizedString("https://example.com"))
      .withCause(AlertCause.MAINTENANCE)
      .withEffect(AlertEffect.REDUCED_SERVICE)
      .withSeverity(AlertSeverity.VERY_SEVERE)
      .addEntity(new EntitySelector.Stop(A.stop.getId()))
      .addTimePeriod(
        new TimePeriod(ALERT_START_TIME.getEpochSecond(), ALERT_END_TIME.getEpochSecond())
      )
      .build();
    railLeg.addAlert(alert);

    var transitService = new DefaultTransitService(transitModel) {
      @Override
      public List<TransitMode> getModesOfStopLocation(StopLocation stop) {
        return List.of(BUS, FERRY);
      }
    };
    context =
      new LegacyGraphQLRequestContext(
        new TestRoutingService(List.of(i1)),
        transitService,
        new DefaultFareService(),
        graph.getVehicleParkingService(),
        new DefaultVehicleRentalService(),
        new DefaultVehiclePositionService(),
        GraphFinder.getInstance(graph, transitService::findRegularStop),
        new RouteRequest()
      );
  }

  @FilePatternSource(pattern = "src/ext-test/resources/legacygraphqlapi/queries/*.graphql")
  @ParameterizedTest(name = "Check GraphQL query in {0}")
  void graphQL(Path path) throws IOException {
    var query = Files.readString(path);
    var response = LegacyGraphQLIndex.getGraphQLResponse(
      query,
      null,
      null,
      2000,
      2000,
      Locale.ENGLISH,
      context
    );
    var actualJson = extracted(response);
    assertEquals(200, response.getStatus());

    Path expectationFile = getPath(path);

    if (!expectationFile.toFile().exists()) {
      Files.writeString(
        expectationFile,
        JsonSupport.prettyPrint(actualJson),
        StandardOpenOption.CREATE_NEW
      );
      fail(
        "No expectations file for %s so generated it. Please check the content.".formatted(path)
      );
    }

    var expectedJson = Files.readString(expectationFile);
    assertEqualJson(expectedJson, actualJson);
  }

  @Nonnull
  private static WalkStep walkStep(String name) {
    return new WalkStep(
      new NonLocalizedString(name),
      WgsCoordinate.GREENWICH,
      false,
      10,
      false,
      false
    );
  }

  @Nonnull
  private static FareProduct fareProduct(String name) {
    return new FareProduct(
      id(name),
      name,
      Money.euros(10),
      null,
      new RiderCategory(id("senior-citizens"), "Senior citizens", null),
      new FareMedium(id("oyster"), "TfL Oyster Card")
    );
  }

  /**
   * Locate 'expectations' relative to the given query input file. The 'expectations' and 'queries'
   * subdirectories are expected to be in the same directory.
   */
  @Nonnull
  private static Path getPath(Path path) {
    return path
      .getParent()
      .getParent()
      .resolve("expectations")
      .resolve(path.getFileName().toString().replace(".graphql", ".json"));
  }

  private static String extracted(Response response) {
    if (response instanceof OutboundJaxrsResponse outbound) {
      return (String) outbound.getContext().getEntity();
    }
    fail("expected an outbound response but got %s".formatted(response.getClass().getSimpleName()));
    return null;
  }
}
