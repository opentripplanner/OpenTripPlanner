package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetMode.WALK;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.geometry.WgsCoordinate;

class ViaDirectStreetRouterTest {

  private static final double FROM_LAT = 59.9000;
  private static final double FROM_LON = 10.7000;
  private static final double VIA_LAT = 59.9005;
  private static final double VIA_LON = 10.7005;
  private static final double TO_LAT = 59.9010;
  private static final double TO_LON = 10.7010;

  @Test
  void directWalkRouteWithViaReturnsItinerary() {
    var fromVertex = intersectionVertex("from", FROM_LAT, FROM_LON);
    var viaVertex = intersectionVertex("via", VIA_LAT, VIA_LON);
    var toVertex = intersectionVertex("to", TO_LAT, TO_LON);

    streetEdge(fromVertex, viaVertex);
    streetEdge(viaVertex, fromVertex);
    streetEdge(viaVertex, toVertex);
    streetEdge(toVertex, viaVertex);

    var fromLocation = GenericLocation.fromCoordinate(FROM_LAT, FROM_LON);
    var toLocation = GenericLocation.fromCoordinate(TO_LAT, TO_LON);

    var viaPoint = new VisitViaLocation(
      "via",
      Duration.ZERO,
      List.of(),
      new WgsCoordinate(VIA_LAT, VIA_LON)
    );
    var viaLocation = viaPoint.coordinateLocation();

    var request = RouteRequest.of()
      .withFrom(fromLocation)
      .withTo(toLocation)
      .withViaLocations(List.of(viaPoint))
      .withJourney(jb -> jb.withDirect(new StreetRequest(WALK)))
      .buildRequest();

    var linkingContext = new LinkingContext(
      Map.of(
        fromLocation,
        Set.of(fromVertex),
        viaLocation,
        Set.of(viaVertex),
        toLocation,
        Set.of(toVertex)
      ),
      Set.of(),
      Set.of()
    );

    var itineraries = new ViaDirectStreetRouter().route(
      TestServerContext.of(),
      request,
      linkingContext
    );

    assertThat(itineraries).isNotEmpty();

    var legs = itineraries.getFirst().legs();
    assertEquals(2, legs.size());
    assertEquals("from_via (59.9, 10.7)", legs.get(0).from().toStringShort());
    assertEquals(
      "corner of via_from and via_to (59.9005, 10.7005)",
      legs.get(0).to().toStringShort()
    );
    assertEquals(
      "corner of via_from and via_to (59.9005, 10.7005)",
      legs.get(1).from().toStringShort()
    );
    assertEquals("to_via (59.901, 10.701)", legs.get(1).to().toStringShort());
  }
}
