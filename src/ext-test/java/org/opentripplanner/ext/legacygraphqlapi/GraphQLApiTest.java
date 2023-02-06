package org.opentripplanner.ext.legacygraphqlapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.test.support.JsonAssertions.assertEqualJson;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.standalone.config.framework.json.JsonSupport;
import org.opentripplanner.standalone.server.TestServerRequestContext;
import org.opentripplanner.test.support.FilePatternSource;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.TransitModel;

@Execution(ExecutionMode.CONCURRENT)
class GraphQLApiTest implements PlanTestConstants {

  static final TestServerRequestContext context;
  static final Graph graph = new Graph();
  static final LegacyGraphQLAPI resource;

  private static final int T10_20 = TimeUtils.hm2time(10, 20);

  static {
    graph
      .getVehicleParkingService()
      .updateVehicleParking(
        List.of(
          VehicleParking
            .builder()
            .id(TransitModelForTest.id("parking-1"))
            .name(NonLocalizedString.ofNullable("parking"))
            .build()
        ),
        List.of()
      );
    var transitModel = new TransitModel();
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

    Itinerary i1 = newItinerary(A, T10_20).walk(20, E).bus(122, T10_20, T10_20, G).build();

    context = new TestServerRequestContext(graph, transitModel);
    context.setRoutingResult(List.of(i1));
    resource = new LegacyGraphQLAPI(context, "ignored");
  }

  @FilePatternSource(pattern = "src/ext-test/resources/legacygraphqlapi/queries/*.graphql")
  @ParameterizedTest(name = "Check GraphQL query in {0}")
  void graphQL(Path path) throws IOException {
    var query = Files.readString(path);
    var response = resource.getGraphQL(query, 2000, 10000, new TestHeaders());
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
