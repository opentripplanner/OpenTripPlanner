package org.opentripplanner.ext.taxi;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.ext.taxi.model.TaxiRoute;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;

class TaxiRouteIndexTest {

  private static final Polygon SQUARE_1 = Polygons.square(
    new Coordinate(0, 0),
    new Coordinate(10, 10)
  );
  private static final Polygon SQUARE_2 = Polygons.square(
    new Coordinate(20, 20),
    new Coordinate(30, 30)
  );

  private static final Route ROUTE_1 = TransitRepositoryForTest.route("route-1").build();
  private static final Route ROUTE_2 = TransitRepositoryForTest.route("route-2").build();

  private static final TaxiRoute TAXI_ROUTE_1 = taxiRoute(ROUTE_1, SQUARE_1);
  private static final TaxiRoute TAXI_ROUTE_2 = taxiRoute(ROUTE_2, SQUARE_2);

  private static final WgsCoordinate INSIDE_SQUARE_1_A = new WgsCoordinate(2, 2);
  private static final WgsCoordinate INSIDE_SQUARE_1_B = new WgsCoordinate(8, 8);
  private static final WgsCoordinate INSIDE_SQUARE_2_A = new WgsCoordinate(25, 25);
  private static final WgsCoordinate INSIDE_SQUARE_2_B = new WgsCoordinate(28, 28);
  private static final WgsCoordinate OUTSIDE_ALL_ROUTES = new WgsCoordinate(50, 50);

  @Test
  void findsRouteCoveringBothPickupAndDropoff() {
    var index = TaxiRouteIndex.createAndIndex(List.of(TAXI_ROUTE_1));

    var result = index.findFirstRoute(INSIDE_SQUARE_1_A, INSIDE_SQUARE_1_B);

    assertThat(result).isPresent();
    assertThat(result.get().route()).isEqualTo(ROUTE_1);
  }

  @Test
  void returnsEmptyWhenDropoffOutsideRoute() {
    var index = TaxiRouteIndex.createAndIndex(List.of(TAXI_ROUTE_1));

    var result = index.findFirstRoute(INSIDE_SQUARE_1_A, INSIDE_SQUARE_2_A);

    assertThat(result).isEmpty();
  }

  @Test
  void returnsEmptyWhenNeitherPointInAnyRoute() {
    var index = TaxiRouteIndex.createAndIndex(List.of(TAXI_ROUTE_1, TAXI_ROUTE_2));

    var result = index.findFirstRoute(OUTSIDE_ALL_ROUTES, OUTSIDE_ALL_ROUTES);

    assertThat(result).isEmpty();
  }

  @Test
  void returnsEmptyForEmptyIndex() {
    var index = TaxiRouteIndex.createAndIndex(List.of());

    var result = index.findFirstRoute(INSIDE_SQUARE_1_A, INSIDE_SQUARE_1_B);

    assertThat(result).isEmpty();
  }

  @Test
  void findsCorrectRouteAmongMultipleCandidates() {
    var index = TaxiRouteIndex.createAndIndex(List.of(TAXI_ROUTE_1, TAXI_ROUTE_2));

    var resultInSquare1 = index.findFirstRoute(INSIDE_SQUARE_1_A, INSIDE_SQUARE_1_B);
    var resultInSquare2 = index.findFirstRoute(INSIDE_SQUARE_2_A, INSIDE_SQUARE_2_B);

    assertThat(resultInSquare1).isPresent();
    assertThat(resultInSquare1.get().route()).isEqualTo(ROUTE_1);
    assertThat(resultInSquare2).isPresent();
    assertThat(resultInSquare2.get().route()).isEqualTo(ROUTE_2);
  }

  @Test
  void findAllRoutesReturnsTheRouteCoveringTheCoordinate() {
    var index = TaxiRouteIndex.createAndIndex(List.of(TAXI_ROUTE_1, TAXI_ROUTE_2));

    var result = index.findAllRoutes(INSIDE_SQUARE_1_A);

    assertThat(result).containsExactly(TAXI_ROUTE_1);
  }

  @Test
  void findAllRoutesReturnsAllRoutesWhenTheyOverlap() {
    var overlappingRoute = taxiRoute(
      ROUTE_2,
      Polygons.square(new Coordinate(5, 5), new Coordinate(15, 15))
    );
    var index = TaxiRouteIndex.createAndIndex(List.of(TAXI_ROUTE_1, overlappingRoute));

    var result = index.findAllRoutes(INSIDE_SQUARE_1_B);

    assertThat(result).containsExactly(TAXI_ROUTE_1, overlappingRoute);
  }

  @Test
  void findAllRoutesReturnsEmptyListWhenNoRouteCoversTheCoordinate() {
    var index = TaxiRouteIndex.createAndIndex(List.of(TAXI_ROUTE_1, TAXI_ROUTE_2));

    var result = index.findAllRoutes(OUTSIDE_ALL_ROUTES);

    assertThat(result).isEmpty();
  }

  @Test
  void findAllRoutesReturnsEmptyListForEmptyIndex() {
    var index = TaxiRouteIndex.createAndIndex(List.of());

    var result = index.findAllRoutes(INSIDE_SQUARE_1_A);

    assertThat(result).isEmpty();
  }

  private static TaxiRoute taxiRoute(Route route, Polygon geometry) {
    return new TaxiRoute(route, geometry, null, null);
  }
}
