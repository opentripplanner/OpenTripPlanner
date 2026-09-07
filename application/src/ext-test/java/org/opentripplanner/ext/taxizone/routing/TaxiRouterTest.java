package org.opentripplanner.ext.taxizone.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.ext.taxizone.TaxiZoneIndex;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.ext.taxizone.model.TaxiZoneLeg;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.service.TransitRepository;

class TaxiRouterTest implements PlanTestConstants {

  private static final double FROM_LAT = 59.9000;
  private static final double FROM_LON = 10.7000;
  private static final double TO_LAT = 59.9010;
  private static final double TO_LON = 10.7010;

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final Place PLACE_A = Place.forStop(
    TEST_MODEL.stop("A").withCoordinate(5.0, 8.0).build()
  );
  private static final Place PLACE_B = Place.forStop(
    TEST_MODEL.stop("B").withCoordinate(6.0, 8.5).build()
  );

  private static final Polygon ZONE_POLYGON = Polygons.square(
    new Coordinate(4, 4),
    new Coordinate(9, 9)
  );

  private static final Route ZONE_ROUTE = TransitRepositoryForTest.route("taxi").build();

  private static final TaxiZoneIndex MATCHING_INDEX = new TaxiZoneIndex(
    List.of(
      new TaxiZone(
        ZONE_POLYGON,
        ZONE_ROUTE,
        null,
        null,
        LocalDateRange.ofInclusiveEnd(
          TestItineraryBuilder.SERVICE_DAY,
          TestItineraryBuilder.SERVICE_DAY
        )
      )
    )
  );
  private static final TaxiZoneIndex EMPTY_INDEX = new TaxiZoneIndex(List.of());
  private static final TaxiZoneIndex WRONG_DATE_INDEX = new TaxiZoneIndex(
    List.of(
      new TaxiZone(
        ZONE_POLYGON,
        ZONE_ROUTE,
        null,
        null,
        LocalDateRange.ofInclusiveEnd(
          TestItineraryBuilder.SERVICE_DAY.plusDays(1),
          TestItineraryBuilder.SERVICE_DAY.plusDays(1)
        )
      )
    )
  );

  // Covers the synthetic FROM/TO coordinates used by routeDirect(...).
  private static final TaxiZone COVERING_ZONE = new TaxiZone(
    Polygons.square(new Coordinate(10.69, 59.89), new Coordinate(10.71, 59.91)),
    ZONE_ROUTE,
    null,
    null,
    LocalDateRange.ofInclusiveEnd(null, null)
  );

  @Test
  void driveLegWithinZoneIsReplacedWithTaxiZoneLeg() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    var subject = new TaxiRouter(MATCHING_INDEX);

    var result = subject.decorateAndFilter(List.of(itinerary)).getFirst();

    var leg = assertInstanceOf(TaxiZoneLeg.class, result.legs().getFirst());
    assertEquals(ZONE_ROUTE, leg.route());
  }

  @Test
  void driveLegWithNoMatchingZoneIsRemovedFromResult() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    var subject = new TaxiRouter(EMPTY_INDEX);

    var result = subject.decorateAndFilter(List.of(itinerary));

    assertTrue(result.isEmpty());
  }

  @Test
  void driveLegOutsideZoneServiceDatesIsRemovedFromResult() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    var subject = new TaxiRouter(WRONG_DATE_INDEX);

    var result = subject.decorateAndFilter(List.of(itinerary));

    assertTrue(result.isEmpty());
  }

  @Test
  void nonDrivingLegIsLeftUntouched() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .bus(21, T11_00, T11_10, PLACE_B)
      .build();
    var originalLeg = itinerary.legs().getFirst();
    var subject = new TaxiRouter(EMPTY_INDEX);

    var result = subject.decorateAndFilter(List.of(itinerary)).getFirst();

    assertEquals(originalLeg, result.legs().getFirst());
  }

  @Test
  void routeDirectDecoratesItineraryWithMatchingZone() {
    var itineraries = routeDirect(List.of(COVERING_ZONE));

    assertFalse(itineraries.isEmpty());
    var itinerary = itineraries.getFirst();
    var leg = assertInstanceOf(TaxiZoneLeg.class, itinerary.legs().getFirst());
    assertEquals(ZONE_ROUTE, leg.route());
  }

  @Test
  void routeDirectFiltersOutItineraryWhenNoZoneMatches() {
    var itineraries = routeDirect(List.of());

    assertTrue(itineraries.isEmpty());
  }

  /**
   * Routes a direct {@link StreetMode#TAXI} request through {@link TaxiRouter#routeDirect} on a
   * minimal synthetic street graph, decorating the result with the given zones.
   */
  private static List<Itinerary> routeDirect(List<TaxiZone> zones) {
    var fromVertex = intersectionVertex("from", FROM_LAT, FROM_LON);
    var toVertex = intersectionVertex("to", TO_LAT, TO_LON);

    streetEdge(fromVertex, toVertex);
    streetEdge(toVertex, fromVertex);

    var fromLocation = GenericLocation.fromCoordinate(FROM_LAT, FROM_LON);
    var toLocation = GenericLocation.fromCoordinate(TO_LAT, TO_LON);

    RouteRequest request = RouteRequest.of()
      .withDateTime(OffsetDateTime.parse("2026-05-13T12:00Z").toInstant())
      .withFrom(fromLocation)
      .withTo(toLocation)
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.TAXI)))
      .buildRequest();

    var linkingContext = new LinkingContext(
      Map.of(fromLocation, Set.of(fromVertex), toLocation, Set.of(toVertex)),
      Set.of(),
      Set.of()
    );

    var transitService = TestServerContext.createTransitService(
      new TransitRepository(),
      TransferServiceTestFactory.defaultTransferRepository()
    );

    var taxiRouter = new TaxiRouter(new TaxiZoneIndex(zones));

    return taxiRouter.routeDirect(
      new Graph(),
      transitService,
      TestServerContext.createStreetLimitationParametersService(),
      TestServerContext.createVehicleRentalService(),
      TestServerContext.createStreetDetailsService(),
      null,
      request,
      linkingContext
    );
  }
}
