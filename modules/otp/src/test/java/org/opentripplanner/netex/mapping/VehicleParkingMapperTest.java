package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.rutebanken.netex.model.ParkingVehicleEnumeration.AGRICULTURAL_VEHICLE;
import static org.rutebanken.netex.model.ParkingVehicleEnumeration.ALL_PASSENGER_VEHICLES;
import static org.rutebanken.netex.model.ParkingVehicleEnumeration.CAR;
import static org.rutebanken.netex.model.ParkingVehicleEnumeration.CAR_WITH_CARAVAN;
import static org.rutebanken.netex.model.ParkingVehicleEnumeration.CYCLE;
import static org.rutebanken.netex.model.ParkingVehicleEnumeration.PEDAL_CYCLE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Parking;
import org.rutebanken.netex.model.ParkingVehicleEnumeration;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

class VehicleParkingMapperTest {

  public static List<Set<ParkingVehicleEnumeration>> carCases() {
    return List.of(Set.of(), Set.of(CAR, AGRICULTURAL_VEHICLE, ALL_PASSENGER_VEHICLES));
  }

  @ParameterizedTest
  @MethodSource("carCases")
  void mapCarLot(Set<ParkingVehicleEnumeration> vehicleTypes) {
    var vp = mapper().map(parking(vehicleTypes));
    assertCommonProperties(vp);
    assertTrue(vp.hasAnyCarPlaces());
    assertEquals(VehicleParkingSpaces.builder().carSpaces(10).build(), vp.getCapacity());
  }

  public static List<Set<ParkingVehicleEnumeration>> bicycleCases() {
    return List.of(Set.of(CYCLE), Set.of(PEDAL_CYCLE, CAR, CAR_WITH_CARAVAN));
  }

  @ParameterizedTest
  @MethodSource("bicycleCases")
  void mapBicycleLot(Set<ParkingVehicleEnumeration> vehicleTypes) {
    var vp = mapper().map(parking(vehicleTypes));
    assertCommonProperties(vp);
    assertTrue(vp.hasBicyclePlaces());
    assertEquals(VehicleParkingSpaces.builder().bicycleSpaces(10).build(), vp.getCapacity());
  }

  @Test
  void dropEmptyCapacity() {
    var parking = parking(Set.of(CAR));
    parking.setTotalCapacity(null);
    var issueStore = new DefaultDataImportIssueStore();
    var vp = mapper(issueStore).map(parking);
    assertNull(vp);
    assertEquals(
      List.of("MissingParkingCapacity"),
      issueStore.listIssues().stream().map(DataImportIssue::getType).toList()
    );
  }

  private VehicleParkingMapper mapper() {
    return mapper(DataImportIssueStore.NOOP);
  }

  private static VehicleParkingMapper mapper(DataImportIssueStore issueStore) {
    return new VehicleParkingMapper(new FeedScopedIdFactory("parking"), issueStore);
  }

  private static void assertCommonProperties(VehicleParking vp) {
    assertEquals("A name", vp.getName().toString());
    assertEquals(new WgsCoordinate(10, 20), vp.getCoordinate());
    assertEquals(
      "[VehicleParkingEntrance{entranceId: parking:LOT1/entrance, coordinate: (10.0, 20.0), carAccessible: true, walkAccessible: true}]",
      vp.getEntrances().toString()
    );
  }

  private static Parking parking(Set<ParkingVehicleEnumeration> vehicleTypes) {
    var name = new MultilingualString();
    name.setValue("A name");
    var point = new SimplePoint_VersionStructure();
    var loc = new LocationStructure();
    loc.setLatitude(new BigDecimal(10));
    loc.setLongitude(new BigDecimal(20));
    point.setLocation(loc);

    var parking = new Parking();
    parking.setId("LOT1");
    parking.setName(name);
    parking.setCentroid(point);
    parking.setTotalCapacity(BigInteger.TEN);
    parking.getParkingVehicleTypes().addAll(vehicleTypes);
    return parking;
  }
}
