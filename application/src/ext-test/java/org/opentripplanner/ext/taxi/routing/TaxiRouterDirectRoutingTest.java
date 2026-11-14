package org.opentripplanner.ext.taxi.routing;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.ext.taxi.TaxiRouteIndex;
import org.opentripplanner.ext.taxi.model.TaxiLeg;
import org.opentripplanner.ext.taxi.model.TaxiRoute;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.service.TransitRepository;

class TaxiRouterDirectRoutingTest implements PlanTestConstants {

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
  private static final WgsCoordinate PICKUP = new WgsCoordinate(5.0, 8.0);
  private static final WgsCoordinate DROPOFF = new WgsCoordinate(6.0, 8.5);

  private static final Polygon ROUTE_POLYGON = Polygons.square(
    new Coordinate(4, 4),
    new Coordinate(9, 9)
  );

  private static final Route ROUTE = TransitRepositoryForTest.route("taxi").build();

  private static final TaxiRouteIndex MATCHING_INDEX = TaxiRouteIndex.createAndIndex(
    List.of(new TaxiRoute(ROUTE, ROUTE_POLYGON, null, null))
  );
  private static final TaxiRouteIndex EMPTY_INDEX = TaxiRouteIndex.createAndIndex(List.of());

  // Covers the synthetic FROM/TO coordinates used by routeDirect(...).
  private static final TaxiRoute COVERING_ROUTE = new TaxiRoute(
    ROUTE,
    Polygons.square(new Coordinate(10.69, 59.89), new Coordinate(10.71, 59.91)),
    null,
    null
  );

  @Test
  void driveLegWithinRouteIsReplacedWithTaxiLeg() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    var subject = new TaxiRouter(MATCHING_INDEX);

    var result = subject.decorateItineraries(List.of(itinerary), PICKUP, DROPOFF).getFirst();

    var resultLeg = result.legs().getFirst();
    assertThat(resultLeg).isInstanceOf(TaxiLeg.class);
    var leg = (TaxiLeg) resultLeg;
    assertThat(leg.route()).isEqualTo(ROUTE);
  }

  @Test
  void driveLegWithNoMatchingRouteIsLeftUndecorated() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    var originalLeg = itinerary.legs().getFirst();
    var subject = new TaxiRouter(EMPTY_INDEX);

    var result = subject.decorateItineraries(List.of(itinerary), PICKUP, DROPOFF).getFirst();

    var resultLeg = result.legs().getFirst();
    assertThat(resultLeg).isInstanceOf(StreetLeg.class);
    assertThat(resultLeg).isNotInstanceOf(TaxiLeg.class);
    assertThat(resultLeg).isEqualTo(originalLeg);
  }

  @Test
  void nonDrivingLegIsLeftUntouched() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .bicycle(T11_00, T11_10, PLACE_B)
      .walk(60, PLACE_A)
      .bus(21, T11_10, T11_20, PLACE_B)
      .build();
    var originalLegs = List.copyOf(itinerary.legs());
    var subject = new TaxiRouter(EMPTY_INDEX);

    var result = subject.decorateItineraries(List.of(itinerary), PICKUP, DROPOFF).getFirst();

    assertThat(result.legs()).isEqualTo(originalLegs);
  }

  @Test
  void routeDecoratesItineraryWithMatchingRoute() {
    var itineraries = routeDirect(List.of(COVERING_ROUTE));

    assertThat(itineraries).isNotEmpty();
    var itinerary = itineraries.getFirst();
    var resultLeg = itinerary.legs().getFirst();
    assertThat(resultLeg).isInstanceOf(TaxiLeg.class);
    var leg = (TaxiLeg) resultLeg;
    assertThat(leg.route()).isEqualTo(ROUTE);
  }

  @Test
  void routeReturnsEmptyWithoutRoutingWhenOriginAndDestinationDoNotShareARoute() {
    var itineraries = routeDirect(List.of());

    assertThat(itineraries).isEmpty();
  }

  /**
   * A taxi is only used from the origin to the first via location, after that the rest of the
   * journey is walked.
   */
  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void routeWithViaUsesTaxiOnlyToTheFirstVia(boolean arriveBy) {
    // The segments are long enough (> 500 m) for driving to be clearly faster than walking
    var fromVertex = intersectionVertex("from", 59.900, 10.700);
    var viaVertex = intersectionVertex("via", 59.905, 10.700);
    var toVertex = intersectionVertex("to", 59.905, 10.720);
    streetEdge(fromVertex, viaVertex);
    streetEdge(viaVertex, fromVertex);
    streetEdge(viaVertex, toVertex);
    streetEdge(toVertex, viaVertex);

    var fromLocation = GenericLocation.fromCoordinate(59.900, 10.700);
    var toLocation = GenericLocation.fromCoordinate(59.905, 10.720);
    var via = new VisitViaLocation(
      "via",
      Duration.ZERO,
      List.of(),
      new WgsCoordinate(59.905, 10.700)
    );

    var request = RouteRequest.of()
      .withDateTime(OffsetDateTime.parse("2026-05-13T12:00Z").toInstant())
      .withArriveBy(arriveBy)
      .withFrom(fromLocation)
      .withTo(toLocation)
      .withViaLocations(List.of(via))
      .withJourney(jb -> jb.withDirect(new StreetRequest(StreetMode.TAXI)))
      .buildRequest();

    var linkingContext = new LinkingContext(
      Map.of(
        fromLocation,
        Set.of(fromVertex),
        via.coordinateLocation(),
        Set.of(viaVertex),
        toLocation,
        Set.of(toVertex)
      ),
      Set.of(),
      Set.of()
    );

    var itineraries = routeDirect(List.of(COVERING_ROUTE), request, linkingContext);

    assertThat(itineraries).hasSize(1);
    var legs = itineraries.getFirst().legs();
    var taxiLegs = legs.stream().filter(TaxiLeg.class::isInstance).toList();
    assertThat(taxiLegs).hasSize(1);
    assertThat(taxiLegs.getFirst().to().coordinate.latitude()).isWithin(1e-6).of(59.905);
    assertThat(taxiLegs.getFirst().to().coordinate.longitude()).isWithin(1e-6).of(10.700);
    var lastLeg = (StreetLeg) legs.getLast();
    assertThat(lastLeg.getMode()).isEqualTo(TraverseMode.WALK);
    assertThat(lastLeg.from().coordinate.longitude()).isWithin(1e-6).of(10.700);
    assertThat(lastLeg.to().coordinate.longitude()).isWithin(1e-6).of(10.720);
  }

  /**
   * Routes a direct {@link StreetMode#TAXI} request through {@link TaxiRouter#routeDirect} on a
   * minimal synthetic street graph, decorating the result with the given routes.
   */
  private static List<Itinerary> routeDirect(List<TaxiRoute> routes) {
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

    return routeDirect(routes, request, linkingContext);
  }

  private static List<Itinerary> routeDirect(
    List<TaxiRoute> routes,
    RouteRequest request,
    LinkingContext linkingContext
  ) {
    var transitService = TestServerContext.createTransitService(
      new TransitRepository(),
      new TimetableDataImport(),
      TransferServiceTestFactory.defaultTransferRepository()
    );

    var taxiRouter = new TaxiRouter(TaxiRouteIndex.createAndIndex(routes));

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
