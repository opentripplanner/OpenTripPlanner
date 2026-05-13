package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.model.StreetMode.WALK;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestServerContext;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.ItinerarySummarizer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;

class ViaDirectStreetRouterTest {

  private static final double FROM_LAT = 59.9000;
  private static final double FROM_LON = 10.7000;
  private static final double VIA_1_LAT = 59.9005;
  private static final double VIA_1_LON = 10.7005;
  private static final double VIA_2_LAT = 59.9008;
  private static final double VIA_2_LON = 10.7008;
  private static final double TO_LAT = 59.9010;
  private static final double TO_LON = 10.7010;

  @Test
  void directWalkRouteWithViaReturnsItinerary() {
    var fromVertex = intersectionVertex("from", FROM_LAT, FROM_LON);
    var firstViaVertex = intersectionVertex("via1", VIA_1_LAT, VIA_1_LON);
    var secondViaVertex = intersectionVertex("via2", VIA_2_LAT, VIA_2_LON);
    var toVertex = intersectionVertex("to", TO_LAT, TO_LON);

    streetEdge(fromVertex, firstViaVertex);
    streetEdge(firstViaVertex, fromVertex);
    streetEdge(firstViaVertex, secondViaVertex);
    streetEdge(secondViaVertex, firstViaVertex);
    streetEdge(secondViaVertex, toVertex);
    streetEdge(toVertex, secondViaVertex);

    var fromLocation = GenericLocation.fromCoordinate(FROM_LAT, FROM_LON);
    var toLocation = GenericLocation.fromCoordinate(TO_LAT, TO_LON);

    var firstViaPoint = new VisitViaLocation(
      "via1",
      Duration.ZERO,
      List.of(),
      new WgsCoordinate(VIA_1_LAT, VIA_1_LON)
    );
    var firstViaLocation = firstViaPoint.coordinateLocation();

    var secondViaWait = Duration.ofMinutes(30);
    var secondViaPoint = new VisitViaLocation(
      "via2",
      secondViaWait,
      List.of(),
      new WgsCoordinate(VIA_2_LAT, VIA_2_LON)
    );
    var secondViaLocation = secondViaPoint.coordinateLocation();

    var startTime = ZonedDateTime.of(
      LocalDate.of(2026, Month.MAY, 12),
      LocalTime.of(20, 30),
      ZoneIds.GMT
    );
    var request = RouteRequest.of()
      .withFrom(fromLocation)
      .withTo(toLocation)
      .withViaLocations(List.of(firstViaPoint, secondViaPoint))
      .withJourney(jb -> jb.withDirect(new StreetRequest(WALK)))
      .withDateTime(startTime.toInstant())
      .buildRequest();

    var linkingContext = new LinkingContext(
      Map.ofEntries(
        entry(fromLocation, Set.of(fromVertex)),
        entry(firstViaLocation, Set.of(firstViaVertex)),
        entry(secondViaLocation, Set.of(secondViaVertex)),
        entry(toLocation, Set.of(toVertex))
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

    var itinerary = new ItinerarySummarizer(itineraries.getFirst());

    assertThat(itinerary.summarizeLegs()).containsExactly(
      "[2026-05-12T20:30Z from_via1 (59.9, 10.7)] → [2026-05-12T20:30:47Z corner of via1_via2 and via1_from (59.9005, 10.7005)]",
      "[2026-05-12T20:30:47Z corner of via1_via2 and via1_from (59.9005, 10.7005)] → [2026-05-12T20:31:15Z corner of via2_via1 and via2_to (59.9008, 10.7008)]",
      "[2026-05-12T21:01:15Z corner of via2_via1 and via2_to (59.9008, 10.7008)] → [2026-05-12T21:01:34Z to_via2 (59.901, 10.701)]"
    );
  }

  @Test
  void isRequestInvalidForRoutingWithPassThrough() {
    var fromLocation = GenericLocation.fromCoordinate(FROM_LAT, FROM_LON);
    var toLocation = GenericLocation.fromCoordinate(TO_LAT, TO_LON);

    var firstViaPoint = new VisitViaLocation(
      "via1",
      Duration.ZERO,
      List.of(),
      new WgsCoordinate(VIA_1_LAT, VIA_1_LON)
    );

    var secondViaPoint = new PassThroughViaLocation("via2", List.of(id("A")));

    var request = RouteRequest.of()
      .withFrom(fromLocation)
      .withTo(toLocation)
      .withViaLocations(List.of(firstViaPoint, secondViaPoint))
      .withJourney(jb -> jb.withDirect(new StreetRequest(WALK)))
      .buildRequest();

    var router = new ViaDirectStreetRouter();
    assertTrue(router.isRequestInvalidForRouting(request));
  }

  @Test
  void isStraightLineDistanceWithinLimit() {
    var fromVertex = intersectionVertex("from", FROM_LAT, FROM_LON);
    var firstViaVertex = intersectionVertex("via1", VIA_1_LAT, VIA_1_LON);
    var secondViaVertex = intersectionVertex("via2", VIA_2_LAT, VIA_2_LON);
    var toVertex = intersectionVertex("to", TO_LAT, TO_LON);

    var fromLocation = GenericLocation.fromCoordinate(FROM_LAT, FROM_LON);
    var toLocation = GenericLocation.fromCoordinate(TO_LAT, TO_LON);

    var firstViaPoint = new VisitViaLocation(
      "via1",
      Duration.ZERO,
      List.of(),
      new WgsCoordinate(VIA_1_LAT, VIA_1_LON)
    );
    var firstViaLocation = firstViaPoint.coordinateLocation();

    var secondViaPoint = new VisitViaLocation(
      "via2",
      Duration.ZERO,
      List.of(),
      new WgsCoordinate(VIA_2_LAT, VIA_2_LON)
    );
    var secondViaLocation = secondViaPoint.coordinateLocation();

    var request = RouteRequest.of()
      .withFrom(fromLocation)
      .withTo(toLocation)
      .withViaLocations(List.of(firstViaPoint, secondViaPoint))
      .withJourney(jb -> jb.withDirect(new StreetRequest(WALK)))
      .buildRequest();

    var linkingContext = new LinkingContext(
      Map.ofEntries(
        entry(fromLocation, Set.of(fromVertex)),
        entry(firstViaLocation, Set.of(firstViaVertex)),
        entry(secondViaLocation, Set.of(secondViaVertex)),
        entry(toLocation, Set.of(toVertex))
      ),
      Set.of(),
      Set.of()
    );
    var router = new ViaDirectStreetRouter();
    assertTrue(router.isStraightLineDistanceWithinLimit(linkingContext, request, 500));
    var withoutViaDistance = SphericalDistanceLibrary.distance(
      fromLocation.getCoordinate(),
      toLocation.getCoordinate()
    );
    assertFalse(
      router.isStraightLineDistanceWithinLimit(linkingContext, request, withoutViaDistance)
    );
  }
}
