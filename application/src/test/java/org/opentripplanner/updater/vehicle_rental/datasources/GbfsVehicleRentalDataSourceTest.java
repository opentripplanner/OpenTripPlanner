package org.opentripplanner.updater.vehicle_rental.datasources;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v2_3.vehicle_types.GBFSVehicleType;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;

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
        HttpHeaders.empty(),
        null,
        false,
        false,
        RentalPickupType.ALL
      ),
      new OtpHttpClientFactory()
    );

    dataSource.setup();

    assertTrue(dataSource.update());

    List<VehicleRentalPlace> stations = dataSource.getUpdates();
    assertEquals(6, stations.size());
    assertTrue(
      stations
        .stream()
        .anyMatch(vehicleRentalStation -> vehicleRentalStation.name().toString().equals("TORVGATA"))
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
          vehicleRentalStation.network().equals("lillestrombysykkel")
        )
    );
    assertTrue(
      stations
        .stream()
        .noneMatch(vehicleRentalStation ->
          vehicleRentalStation.isArrivingInRentalVehicleAtDestinationAllowed()
        )
    );

    assertTrue(
      stations.stream().noneMatch(vehicleRentalStation -> vehicleRentalStation.overloadingAllowed())
    );
  }

  @Test
  void getEmptyListOfVehicleTypes() {
    GbfsVehicleTypeMapper vehicleTypeMapper = new GbfsVehicleTypeMapper("systemID");
    Map<String, RentalVehicleType> vehicleTypes = GbfsVehicleRentalDataSource.mapVehicleTypes(
      vehicleTypeMapper,
      Collections.emptyList()
    );
    assertTrue(vehicleTypes.isEmpty());
  }

  @Test
  void duplicatedVehicleTypesDoNotThrowException() {
    GbfsVehicleTypeMapper vehicleTypeMapper = new GbfsVehicleTypeMapper("systemID");

    List<GBFSVehicleType> vehicleTypes = getDuplicatedGbfsVehicleTypes();

    assertDoesNotThrow(() -> {
      GbfsVehicleRentalDataSource.mapVehicleTypes(vehicleTypeMapper, vehicleTypes);
    });
  }

  @Test
  void getOneVehicleTypeOfDuplicatedVehicleTypes() {
    GbfsVehicleTypeMapper vehicleTypeMapper = new GbfsVehicleTypeMapper("systemID");

    List<GBFSVehicleType> duplicatedVehicleTypes = getDuplicatedGbfsVehicleTypes();

    Map<String, RentalVehicleType> vehicleTypes = GbfsVehicleRentalDataSource.mapVehicleTypes(
      vehicleTypeMapper,
      duplicatedVehicleTypes
    );
    assertEquals(1, vehicleTypes.size());
  }

  @Test
  void geofencing() {
    var dataSource = new GbfsVehicleRentalDataSource(
      new GbfsVehicleRentalDataSourceParameters(
        "file:src/test/resources/gbfs/tieroslo/gbfs.json",
        "en",
        false,
        HttpHeaders.empty(),
        null,
        true,
        false,
        RentalPickupType.ALL
      ),
      new OtpHttpClientFactory()
    );

    dataSource.setup();

    assertTrue(dataSource.update());

    dataSource.getUpdates();

    var zones = dataSource.getGeofencingZones();

    assertEquals(2, zones.size());

    var frognerPark = zones
      .stream()
      .filter(z -> z.id().getId().equals("NP Frogner og vigelandsparken"))
      .findFirst()
      .get();

    assertTrue(frognerPark.dropOffBanned());
    assertFalse(frognerPark.traversalBanned());

    var businessAreas = zones.stream().filter(GeofencingZone::isBusinessArea).toList();

    assertEquals(1, businessAreas.size());

    assertEquals("tieroslo:OSLO Summer 2021", businessAreas.get(0).id().toString());
  }

  @Test
  void makeStationFromV10() {
    var network = "helsinki_gbfs";
    var dataSource = new GbfsVehicleRentalDataSource(
      new GbfsVehicleRentalDataSourceParameters(
        "file:src/test/resources/gbfs/helsinki/gbfs.json",
        "en",
        false,
        HttpHeaders.empty(),
        network,
        false,
        true,
        RentalPickupType.ALL
      ),
      new OtpHttpClientFactory()
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
          vehicleRentalStation.name().toString().equals("Viiskulma")
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
      stations.stream().allMatch(vehicleRentalStation -> vehicleRentalStation.network() == network)
    );
    assertTrue(
      stations
        .stream()
        .noneMatch(vehicleRentalStation ->
          vehicleRentalStation.isArrivingInRentalVehicleAtDestinationAllowed()
        )
    );
    assertTrue(
      stations.stream().allMatch(vehicleRentalStation -> vehicleRentalStation.overloadingAllowed())
    );
  }

  private static List<GBFSVehicleType> getDuplicatedGbfsVehicleTypes() {
    GBFSVehicleType gbfsVehicleType1 = new GBFSVehicleType();
    gbfsVehicleType1.setVehicleTypeId("sameId");
    gbfsVehicleType1.setFormFactor(GBFSVehicleType.FormFactor.BICYCLE);
    gbfsVehicleType1.setPropulsionType(GBFSVehicleType.PropulsionType.HUMAN);

    GBFSVehicleType gbfsVehicleType2 = new GBFSVehicleType();
    gbfsVehicleType2.setVehicleTypeId("sameId");
    gbfsVehicleType2.setFormFactor(GBFSVehicleType.FormFactor.BICYCLE);
    gbfsVehicleType2.setPropulsionType(GBFSVehicleType.PropulsionType.HUMAN);

    return List.of(gbfsVehicleType1, gbfsVehicleType2);
  }
}
