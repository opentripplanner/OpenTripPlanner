package org.opentripplanner.updater.vehicle_rental.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;

/**
 * This tests the mapping between data coming from a {@link GbfsFeedLoader} to OTP station models.
 */
class GbfsVehicleRentalDataSourceTest {

  @Test
  void makeStationFromV22() {
    var dataSource = new GbfsVehicleRentalDataSource(
      new GbfsVehicleRentalDataSourceParameters(
        "file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json",
        "nb",
        false,
        new HashMap<>(),
        null
      )
    );

    dataSource.setup();

    assertTrue(dataSource.update());

    List<VehicleRentalPlace> stations = dataSource.getUpdates();
    assertEquals(6, stations.size());
    assertTrue(
      stations
        .stream()
        .anyMatch(vehicleRentalStation ->
          vehicleRentalStation.getName().toString().equals("TORVGATA")
        )
    );
    assertTrue(
      stations.stream().allMatch(vehicleRentalStation -> vehicleRentalStation.isAllowDropoff())
    );
    assertTrue(
      stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isFloatingVehicle())
    );
    assertTrue(
      stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isCarStation())
    );
    assertTrue(
      stations
        .stream()
        .allMatch(vehicleRentalStation ->
          vehicleRentalStation.getNetwork().equals("lillestrombysykkel")
        )
    );
    assertTrue(
      stations
        .stream()
        .noneMatch(vehicleRentalStation ->
          vehicleRentalStation.isArrivingInRentalVehicleAtDestinationAllowed()
        )
    );
  }

  @Test
  void makeStationFromV10() {
    var network = "helsinki_gbfs";
    var dataSource = new GbfsVehicleRentalDataSource(
      new GbfsVehicleRentalDataSourceParameters(
        "file:src/test/resources/gbfs/helsinki/gbfs.json",
        "en",
        false,
        new HashMap<>(),
        network
      )
    );

    dataSource.setup();

    assertTrue(dataSource.update());

    List<VehicleRentalPlace> stations = dataSource.getUpdates();
    // There are 10 stations in the data but 5 are missing required data
    assertEquals(5, stations.size());
    assertTrue(
      stations
        .stream()
        .anyMatch(vehicleRentalStation ->
          vehicleRentalStation.getName().toString().equals("Viiskulma")
        )
    );
    assertTrue(
      stations.stream().anyMatch(vehicleRentalStation -> vehicleRentalStation.isAllowDropoff())
    );
    assertTrue(
      stations.stream().anyMatch(vehicleRentalStation -> !vehicleRentalStation.isAllowDropoff())
    );
    assertTrue(
      stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isFloatingVehicle())
    );
    assertTrue(
      stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.isCarStation())
    );
    assertTrue(
      stations
        .stream()
        .allMatch(vehicleRentalStation -> vehicleRentalStation.getNetwork() == network)
    );
    assertTrue(
      stations
        .stream()
        .noneMatch(vehicleRentalStation ->
          vehicleRentalStation.isArrivingInRentalVehicleAtDestinationAllowed()
        )
    );
  }
}
