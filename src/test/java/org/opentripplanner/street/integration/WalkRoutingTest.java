package org.opentripplanner.street.integration;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.test.support.ResourceLoader;

class WalkRoutingTest {

  static final Instant dateTime = Instant.now();
  private final Graph roundabout;

  {
    TestOtpModel model = ConstantsForTests.buildOsmGraph(
      ResourceLoader.of(WalkRoutingTest.class).file("roundabout.osm.pbf")
    );
    roundabout = model.graph();

    model.transitModel().index();
    roundabout.index(model.transitModel().getStopModel());
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
    var start = new GenericLocation(59.94646, 10.77511);
    var end = new GenericLocation(59.94641, 10.77522);
    Assertions.assertDoesNotThrow(() -> route(roundabout, start, end));
  }

  private static List<GraphPath<State, Edge, Vertex>> route(
    Graph graph,
    GenericLocation from,
    GenericLocation to
  ) {
    RouteRequest request = new RouteRequest();
    request.setDateTime(dateTime);
    request.setFrom(from);
    request.setTo(to);
    request.journey().direct().setMode(StreetMode.WALK);

    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        graph,
        request,
        request.journey().direct().mode(),
        request.journey().direct().mode()
      )
    ) {
      var gpf = new GraphPathFinder(null);
      return gpf.graphPathFinderEntryPoint(request, temporaryVertices);
    }
  }
}
