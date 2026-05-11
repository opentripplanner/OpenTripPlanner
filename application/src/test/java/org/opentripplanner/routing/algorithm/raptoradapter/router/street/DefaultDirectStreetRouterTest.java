package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetMode.WALK;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.linking.LinkingContext;

class DefaultDirectStreetRouterTest {

  private static final double FROM_LAT = 59.9000;
  private static final double FROM_LON = 10.7000;
  private static final double TO_LAT = 59.9010;
  private static final double TO_LON = 10.7010;

  @Test
  void directWalkRouteReturnsItinerary() {
    var fromVertex = intersectionVertex("from", FROM_LAT, FROM_LON);
    var toVertex = intersectionVertex("to", TO_LAT, TO_LON);

    streetEdge(fromVertex, toVertex);
    streetEdge(toVertex, fromVertex);

    var fromLocation = GenericLocation.fromCoordinate(FROM_LAT, FROM_LON);
    var toLocation = GenericLocation.fromCoordinate(TO_LAT, TO_LON);

    var request = RouteRequest.of()
      .withFrom(fromLocation)
      .withTo(toLocation)
      .withJourney(jb -> jb.withDirect(new StreetRequest(WALK)))
      .buildRequest();

    var linkingContext = new LinkingContext(
      Map.of(fromLocation, Set.of(fromVertex), toLocation, Set.of(toVertex)),
      Set.of(),
      Set.of()
    );

    var itineraries = new DefaultDirectStreetRouter().route(
      TestServerContext.of(),
      request,
      linkingContext
    );

    assertThat(itineraries).isNotEmpty();

    var first = itineraries.getFirst().legs().getFirst();
    assertEquals("from_to (59.9, 10.7)", first.from().toStringShort());
    assertEquals("to_from (59.901, 10.701)", first.to().toStringShort());
  }
}
