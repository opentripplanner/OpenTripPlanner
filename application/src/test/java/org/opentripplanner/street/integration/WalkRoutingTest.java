package org.opentripplanner.street.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.DirectStreetRouter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.linking.mapping.LinkingContextRequestMapper;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.test.support.ResourceLoader;

class WalkRoutingTest {

  static final Instant DATE_TIME = Instant.now();
  private final Graph roundabout;

  {
    TestOtpModel model = ConstantsForTests.buildOsmGraph(
      ResourceLoader.of(WalkRoutingTest.class).file("roundabout.osm.pbf")
    );
    roundabout = model.graph();

    model.timetableRepository().index();
    roundabout.index();
  }

  /**
   * Both https://www.openstreetmap.org/way/146988098 and
   * https://www.openstreetmap.org/way/146988099 are routable for pedestrians, the routing engine
   * should return a path from any point of the first way to any point of the second.
   * <br>
   * See also <a href="https://github.com/opentripplanner/OpenTripPlanner/issues/5706">issue
   * #5706</a>
   */
  @Test
  void shouldRouteAroundRoundabout() {
    var start = GenericLocation.fromCoordinate(59.94646, 10.77511);
    var end = GenericLocation.fromCoordinate(59.94641, 10.77522);
    assertFalse(route(roundabout, start, end, DATE_TIME, false).isEmpty());
  }

  private static List<Itinerary> route(
    Graph graph,
    GenericLocation from,
    GenericLocation to,
    Instant instant,
    boolean arriveBy
  ) {
    RouteRequest request = RouteRequest.of()
      .withDateTime(instant)
      .withFrom(from)
      .withTo(to)
      .withArriveBy(arriveBy)
      .buildRequest();
    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var vertexLinker = VertexLinkerTestFactory.of(graph);
      var vertexCreationService = new VertexCreationService(vertexLinker);
      var linkingContextFactory = new LinkingContextFactory(graph, vertexCreationService);
      var linkingRequest = LinkingContextRequestMapper.map(request);
      var linkingContext = linkingContextFactory.create(temporaryVerticesContainer, linkingRequest);
      var ctx = TestServerContext.ofGraph(graph);
      return DirectStreetRouter.route(ctx, request, linkingContext);
    }
  }
}
