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
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.standalone.config.framework.json.JsonSupport;
import org.opentripplanner.test.support.FilePatternSource;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
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
    var transitModel = new TransitModel(stopModel.build(), new Deduplicator());
    transitModel.initTimeZone(ZoneIds.BERLIN);
    transitModel.index();
    Arrays
      .stream(TransitMode.values())
      .sorted(Comparator.comparing(Enum::name))
      .forEach(m ->
        transitModel
          .getTransitModelIndex()
          .addRoutes(
            TransitModelForTest
              .route(m.name())
              .withMode(m)
              .withLongName(new NonLocalizedString("Long name for %s".formatted(m)))
              .build()
          )
      );

    Itinerary i1 = newItinerary(A, T11_00)
      .walk(20, B)
      .bus(122, T11_01, T11_15, C)
      .rail(439, T11_30, T11_50, D)
      .carHail(D10m, E)
      .build();

    var railLeg = i1.getTransitLeg(2);
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

    var transitService = new DefaultTransitService(transitModel);
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
