package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.ItinerarySummarizer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

class ViaDirectStreetRouterTest {

  private static final double FROM_LAT = 59.9000;
  private static final double FROM_LON = 10.7000;
  private static final double VIA_1_LAT = 59.9005;
  private static final double VIA_1_LON = 10.7005;
  private static final double VIA_2_LAT = 59.9008;
  private static final double VIA_2_LON = 10.7008;
  private static final double TO_LAT = 59.9010;
  private static final double TO_LON = 10.7010;
  private static final IntersectionVertex FROM = intersectionVertex("from", FROM_LAT, FROM_LON);
  private static final IntersectionVertex VIA_1 = intersectionVertex("via1", VIA_1_LAT, VIA_1_LON);
  private static final IntersectionVertex VIA_2 = intersectionVertex("via2", VIA_2_LAT, VIA_2_LON);
  private static final IntersectionVertex TO = intersectionVertex("to", TO_LAT, TO_LON);
  private static final GenericLocation FROM_LOCATION = GenericLocation.fromCoordinate(
    FROM_LAT,
    FROM_LON
  );
  private static final GenericLocation TO_LOCATION = GenericLocation.fromCoordinate(TO_LAT, TO_LON);
  private static final VisitViaLocation FIRST_VIA_LOCATION = new VisitViaLocation(
    "via1",
    Duration.ZERO,
    List.of(),
    new WgsCoordinate(VIA_1_LAT, VIA_1_LON)
  );
  private static final GenericLocation FIRST_VIA_COORDINATE_LOCATION =
    FIRST_VIA_LOCATION.coordinateLocation();

  private static final Duration SECOND_VIA_WAIT = Duration.ofMinutes(30);
  private static final VisitViaLocation SECOND_VIA_LOCATION = new VisitViaLocation(
    "via2",
    SECOND_VIA_WAIT,
    List.of(),
    new WgsCoordinate(VIA_2_LAT, VIA_2_LON)
  );
  private static final GenericLocation SECOND_VIA_COORDINATE_LOCATION =
    SECOND_VIA_LOCATION.coordinateLocation();

  static {
    streetEdge(FROM, VIA_1);
    streetEdge(VIA_1, FROM);
    streetEdge(VIA_1, VIA_2);
    streetEdge(VIA_2, VIA_1);
    streetEdge(VIA_2, TO);
    streetEdge(TO, VIA_2);
  }

  static List<Arguments> arriveByTestCases() {
    return List.of(
      of(
        false,
        List.of(
          "[2026-05-12T20:30Z from_via1 (59.9, 10.7)] → [2026-05-12T20:30:47Z corner of via1_via2 and via1_from (59.9005, 10.7005)]",
          "[2026-05-12T20:30:47Z corner of via1_via2 and via1_from (59.9005, 10.7005)] → [2026-05-12T20:31:15Z corner of via2_via1 and via2_to (59.9008, 10.7008)]",
          "[2026-05-12T21:01:15Z corner of via2_via1 and via2_to (59.9008, 10.7008)] → [2026-05-12T21:01:34Z to_via2 (59.901, 10.701)]"
        )
      ),
      of(
        true,
        List.of(
          "[2026-05-12T19:58:26Z from_via1 (59.9, 10.7)] → [2026-05-12T19:59:13Z corner of via1_via2 and via1_from (59.9005, 10.7005)]",
          "[2026-05-12T19:59:13Z corner of via1_via2 and via1_from (59.9005, 10.7005)] → [2026-05-12T19:59:41Z corner of via2_via1 and via2_to (59.9008, 10.7008)]",
          "[2026-05-12T20:29:41Z corner of via2_via1 and via2_to (59.9008, 10.7008)] → [2026-05-12T20:30Z to_via2 (59.901, 10.701)]"
        )
      )
    );
  }

  @ParameterizedTest
  @MethodSource("arriveByTestCases")
  void directWalkRouteWithViaReturnsItinerary(boolean arriveBy, List<String> legSummary) {
    var startTime = ZonedDateTime.of(
      LocalDate.of(2026, Month.MAY, 12),
      LocalTime.of(20, 30),
      ZoneIds.GMT
    );
    var request = RouteRequest.of()
      .withFrom(FROM_LOCATION)
      .withTo(TO_LOCATION)
      .withViaLocations(List.of(FIRST_VIA_LOCATION, SECOND_VIA_LOCATION))
      .withJourney(jb -> jb.withDirect(new StreetRequest(WALK)))
      .withDateTime(startTime.toInstant())
      .withArriveBy(arriveBy)
      .buildRequest();

    var linkingContext = new LinkingContext(
      Map.ofEntries(
        entry(FROM_LOCATION, Set.of(FROM)),
        entry(FIRST_VIA_COORDINATE_LOCATION, Set.of(VIA_1)),
        entry(SECOND_VIA_COORDINATE_LOCATION, Set.of(VIA_2)),
        entry(TO_LOCATION, Set.of(TO))
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

    assertThat(itinerary.summarizeLegs()).containsExactlyElementsIn(legSummary);
  }

  @Test
  void isRequestInvalidForRoutingWithPassThrough() {
    var fromLocation = GenericLocation.fromCoordinate(FROM_LAT, FROM_LON);
    var toLocation = GenericLocation.fromCoordinate(TO_LAT, TO_LON);

    var secondViaPoint = new PassThroughViaLocation("via2", List.of(id("A")));

    var request = RouteRequest.of()
      .withFrom(fromLocation)
      .withTo(toLocation)
      .withViaLocations(List.of(FIRST_VIA_LOCATION, secondViaPoint))
      .withJourney(jb -> jb.withDirect(new StreetRequest(WALK)))
      .buildRequest();

    var router = new ViaDirectStreetRouter();
    assertTrue(router.isRequestInvalidForRouting(request));
  }

  @Test
  void isStraightLineDistanceWithinLimit() {
    var request = RouteRequest.of()
      .withFrom(FROM_LOCATION)
      .withTo(TO_LOCATION)
      .withViaLocations(List.of(FIRST_VIA_LOCATION, SECOND_VIA_LOCATION))
      .withJourney(jb -> jb.withDirect(new StreetRequest(WALK)))
      .buildRequest();

    var linkingContext = new LinkingContext(
      Map.ofEntries(
        entry(FROM_LOCATION, Set.of(FROM)),
        entry(FIRST_VIA_COORDINATE_LOCATION, Set.of(VIA_1)),
        entry(SECOND_VIA_COORDINATE_LOCATION, Set.of(VIA_2)),
        entry(TO_LOCATION, Set.of(TO))
      ),
      Set.of(),
      Set.of()
    );
    var router = new ViaDirectStreetRouter();
    assertTrue(router.isStraightLineDistanceWithinLimit(linkingContext, request, 500));
    var withoutViaDistance = SphericalDistanceLibrary.distance(
      FROM_LOCATION.getCoordinate(),
      TO_LOCATION.getCoordinate()
    );
    assertFalse(
      router.isStraightLineDistanceWithinLimit(linkingContext, request, withoutViaDistance)
    );
  }
}
