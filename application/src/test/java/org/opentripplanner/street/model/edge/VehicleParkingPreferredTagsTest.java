package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingEntrance;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

class VehicleParkingPreferredTagsTest {

  public static final WgsCoordinate COORDINATE = new WgsCoordinate(1, 1);
  public static final int EXTRA_COST = 1000;
  public static final int NO_COST = 0;

  static Stream<Arguments> testCases() {
    return Stream.of(
      // no preferred tags so no extra cost
      of(Set.of("locker"), Set.of(), NO_COST),
      of(Set.of(), Set.of(), NO_COST),
      // parking lot doesn't have the preferred tag so add extra cost
      of(Set.of("roof"), Set.of("locker"), EXTRA_COST),
      of(Set.of("locker"), Set.of("locker"), NO_COST),
      of(Set.of("locker", "roof"), Set.of("locker"), NO_COST),
      of(Set.of("locker", "roof"), Set.of("locker", "concierge"), NO_COST),
      // parking doesn't have any tags so cannot be preferred
      of(Set.of(), Set.of("locker"), EXTRA_COST)
    );
  }

  @ParameterizedTest(
    name = "Bike parking with tags {0} and preferred tags {1} should lead to a traversal cost of {2} (departAt)"
  )
  @MethodSource("testCases")
  void departAt(Set<String> parkingTags, Set<String> preferredTags, double expectedCost) {
    runTest(parkingTags, preferredTags, expectedCost, false);
  }

  @ParameterizedTest(
    name = "Bike parking with tags {0} and preferred tags {1} should lead to a traversal cost of {2} (arriveBy)"
  )
  @MethodSource("testCases")
  void arriveBy(Set<String> parkingTags, Set<String> preferredTags, double expectedCost) {
    runTest(parkingTags, preferredTags, expectedCost, true);
  }

  private void runTest(
    Set<String> parkingTags,
    Set<String> preferredTags,
    double expectedCost,
    boolean arriveBy
  ) {
    var parking = StreetModelForTest.vehicleParking()
      .tags(parkingTags)
      .availability(VehicleParkingSpaces.builder().bicycleSpaces(100).build())
      .bicyclePlaces(true)
      .build();
    var entrance = VehicleParkingEntrance.builder()
      .name(new NonLocalizedString("bike parking"))
      .vehicleParking(parking)
      .walkAccessible(true)
      .coordinate(COORDINATE)
      .build();

    var fromV = new VehicleParkingEntranceVertex(entrance);
    var edge = VehicleParkingEdge.createVehicleParkingEdge(fromV);

    var req = StreetSearchRequest.of();
    req.withMode(StreetMode.BIKE_TO_PARK);
    req.withArriveBy(arriveBy);
    req.withPreferences(p ->
      p.withBike(bike -> {
        bike.withParking(parkingPreferences -> {
          parkingPreferences.withUnpreferredVehicleParkingTagCost(EXTRA_COST);
          parkingPreferences.withPreferredVehicleParkingTags(preferredTags);
          parkingPreferences.withCost(0);
        });
      })
    );

    var result = traverse(fromV, edge, req.build());

    assertEquals(expectedCost, result.weight);
  }

  private State traverse(Vertex fromV, Edge edge, StreetSearchRequest request) {
    var state = new State(fromV, request);

    assertEquals(0, state.weight);
    return edge.traverse(state)[0];
  }
}
