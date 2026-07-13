package org.opentripplanner.graph_builder.module.osm.moduletests.parking;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;

/// Invariants that hold for every parking lot regardless of how it is connected to the street
/// network: exactly one lot is created, the name tag is read correctly, and the car/bicycle place
/// flags match the OSM amenity type.
class ParkingLotInvariantsTest {

  private static final List<OsmNode> NODES = List.of(
    OsmNode.of().withId(1).withLatLon(0.0, 0.0).build(),
    OsmNode.of().withId(2).withLatLon(0.001, 0.0).build(),
    OsmNode.of().withId(3).withLatLon(0.001, 0.001).build(),
    OsmNode.of().withId(4).withLatLon(0.0, 0.001).build()
  );

  record TestCase(
    Map<String, String> tags,
    boolean expectedCarPlaces,
    boolean expectedBicyclePlaces
  ) {}

  static Stream<Named<TestCase>> cases() {
    return Stream.of(
      Named.of(
        "car P+R",
        new TestCase(
          Map.ofEntries(
            entry("amenity", "parking"),
            entry("park_ride", "yes"),
            entry("name", "Test P+R")
          ),
          true,
          false
        )
      ),
      Named.of(
        "bike parking",
        new TestCase(
          Map.ofEntries(entry("amenity", "bicycle_parking"), entry("name", "Test Bike Parking")),
          false,
          true
        )
      ),
      Named.of(
        "combined car and bike P+R",
        new TestCase(
          Map.ofEntries(
            entry("amenity", "parking"),
            entry("park_ride", "yes"),
            entry("capacity:bike", "10"),
            entry("name", "Test Combined P+R")
          ),
          true,
          true
        )
      )
    );
  }

  @ParameterizedTest
  @MethodSource("cases")
  void parkingLotHasCorrectTypeAndName(TestCase tc) {
    var wayBuilder = OsmWay.of().withId(1);
    tc.tags().forEach(wayBuilder::withTag);
    var way = wayBuilder.addNodeRef(1, 2, 3, 4, 1).build();

    var provider = new TestOsmProvider(List.of(), List.of(way), NODES);
    var parkingRepository = new DefaultVehicleParkingRepository();

    OsmModuleTestFactory.of(provider)
      .withVehicleParkingRepository(parkingRepository)
      .builder()
      .withStaticParkAndRide(true)
      .withStaticBikeParkAndRide(true)
      .build()
      .buildGraph();

    var parkings = List.copyOf(parkingRepository.listVehicleParkings());
    assertEquals(1, parkings.size());

    var parking = parkings.getFirst();
    assertEquals(tc.tags().get("name"), parking.getName().toString());
    assertEquals(tc.expectedCarPlaces(), parking.hasCarPlaces());
    assertEquals(tc.expectedBicyclePlaces(), parking.hasBicyclePlaces());
  }
}
