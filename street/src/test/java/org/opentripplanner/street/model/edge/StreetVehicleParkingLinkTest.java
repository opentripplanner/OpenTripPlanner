package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.core.model.id.FeedScopedIdFactory.id;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingEntrance;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.filter.ParkingFilter;
import org.opentripplanner.street.search.request.filter.ParkingSelect.TagsSelect;
import org.opentripplanner.street.search.state.State;

class StreetVehicleParkingLinkTest {

  static Stream<Arguments> testCases() {
    return Stream.of(
      of(Set.of(), Set.of(), Set.of(), true),
      of(Set.of("a-tag"), Set.of(), Set.of(), true),
      of(Set.of("a"), Set.of("a"), Set.of(), false),
      of(Set.of("a"), Set.of("a"), Set.of("a"), false),
      of(Set.of("a", "b"), Set.of("b"), Set.of("a"), false),
      of(Set.of("a", "b"), Set.of(), Set.of("a"), true),
      of(Set.of("a", "b"), Set.of(), Set.of("c"), false)
    );
  }

  @ParameterizedTest(name = "Parking[tags={0}], Request[not={1}, select={2}] should traverse={3}")
  @MethodSource("testCases")
  void parkingFilters(
    Set<String> parkingTags,
    Set<String> not,
    Set<String> select,
    boolean shouldTraverse
  ) {
    var streetVertex = intersectionVertex(1, 1);
    final var entranceVertex = buildVertex(parkingTags);

    var req = StreetSearchRequest.of();
    req.withMode(StreetMode.BIKE_TO_PARK);
    req.withBike(bike ->
      bike.withParking(parkingPreferences -> {
        parkingPreferences.withFilter(
          new ParkingFilter(List.of(new TagsSelect(not)), List.of(new TagsSelect(select)))
        );
        parkingPreferences.withCost(Cost.ZERO);
      })
    );

    var edge = StreetVehicleParkingLink.createStreetVehicleParkingLink(
      streetVertex,
      entranceVertex
    );

    var result = traverse(streetVertex, edge, req.build());
    if (shouldTraverse) {
      assertFalse(State.isEmpty(result));
    } else {
      assertTrue(State.isEmpty(result));
    }
  }

  @Test
  void notLinkedToGraph() {
    var vertex = buildVertex(Set.of());
    assertFalse(vertex.isLinkedToGraph());
  }

  @Test
  void linkedToGraphWithIncoming() {
    var vertex = buildVertex(Set.of());
    var streetVertex = StreetModelFactory.intersectionVertex(1, 1);
    vertex.addIncoming(
      StreetVehicleParkingLink.createStreetVehicleParkingLink(streetVertex, vertex)
    );
    assertTrue(vertex.isLinkedToGraph());
  }

  @Test
  void linkedToGraphWithOutgoing() {
    var vertex = buildVertex(Set.of());
    var streetVertex = StreetModelFactory.intersectionVertex(1, 1);
    vertex.addOutgoing(
      StreetVehicleParkingLink.createStreetVehicleParkingLink(streetVertex, vertex)
    );
    assertTrue(vertex.isLinkedToGraph());
  }

  private static VehicleParkingEntranceVertex buildVertex(Set<String> parkingTags) {
    var parking = VehicleParking.builder()
      .id(id("parking"))
      .coordinate(new WgsCoordinate(1, 1))
      .tags(parkingTags)
      .build();

    var entrance = VehicleParkingEntrance.builder()
      .vehicleParking(parking)
      .entranceId(id("entrance"))
      .coordinate(new WgsCoordinate(1, 1))
      .name(new NonLocalizedString("entrance"))
      .walkAccessible(true)
      .carAccessible(true)
      .build();

    return new VehicleParkingEntranceVertex(entrance);
  }

  private State[] traverse(Vertex fromV, Edge edge, StreetSearchRequest request) {
    var state = new State(fromV, request);

    assertEquals(0, state.weight);
    return edge.traverse(state);
  }
}
