package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingEntrance;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.test.support.VariableSource;

class StreetVehicleParkingLinkTest {

  public static final WgsCoordinate COORDINATE = new WgsCoordinate(1, 1);
  public static final SimpleVertex FROMV = new SimpleVertex(null, "1", 1, 1);
  public static final int COST = 1000;
  static Stream<Arguments> testCases = Stream.of(
    // no preferred tags so no extra cost
    of(Set.of("locker"), Set.of(), 1),
    of(Set.of(), Set.of(), 1),
    // parking lot doesn't have the preferred tag so add extra cost
    of(Set.of("roof"), Set.of("locker"), COST + 1),
    of(Set.of("locker"), Set.of("locker"), 1),
    of(Set.of("locker", "roof"), Set.of("locker"), 1),
    of(Set.of("locker", "roof"), Set.of("locker", "concierge"), 1),
    // parking doesn't have any tags so cannot be preferred
    of(Set.of(), Set.of("locker"), COST + 1)
  );

  @ParameterizedTest(
    name = "Bike parking with tags {0} and preferred tags {1} should lead to a traversal cost of {2}"
  )
  @VariableSource("testCases")
  void preferred(Set<String> parkingTags, Set<String> preferredTags, double expectedCost) {
    var parking = VehicleParking.builder().coordinate(COORDINATE).tags(parkingTags).build();
    var entrance = VehicleParkingEntrance
      .builder()
      .name(new NonLocalizedString("bike parking"))
      .vehicleParking(parking)
      .walkAccessible(true)
      .coordinate(COORDINATE)
      .build();
    var edge = new StreetVehicleParkingLink(
      FROMV,
      new VehicleParkingEntranceVertex(null, entrance)
    );

    var parkingReq = new VehicleParkingRequest();
    parkingReq.setPreferredTags(preferredTags);
    parkingReq.setUnpreferredTagCost(COST);

    var req = StreetSearchRequest.of();
    req.withMode(StreetMode.BIKE_TO_PARK);
    req.withParking(parkingReq);

    var result = traverse(edge, req.build());

    assertEquals(expectedCost, result.weight);
  }

  private State traverse(Edge edge, StreetSearchRequest request) {
    var state = new State(FROMV, request);

    assertEquals(0, state.weight);
    return edge.traverse(state);
  }
}
