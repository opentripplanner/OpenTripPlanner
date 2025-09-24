package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v2;

import static org.junit.jupiter.api.Assertions.*;

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
import org.opentripplanner.updater.vehicle_rental.datasources.gbfs.GbfsVehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;
import org.slf4j.LoggerFactory;

/**
 * This tests the mapping between data coming from a {@link GbfsFeedLoader} to OTP station models.
 */
class GbfsFeedMapperTest {

  @Test
  void makeStationFromV22() {
    var params = new GbfsVehicleRentalDataSourceParameters(
      "file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json",
      "nb",
      false,
      HttpHeaders.empty(),
      null,
      false,
      false,
      RentalPickupType.ALL
    );
    var otpHttpClient = new OtpHttpClientFactory()
      .create(LoggerFactory.getLogger(GbfsFeedMapperTest.class));
    var loader = new GbfsFeedLoader(
      params.url(),
      params.httpHeaders(),
      params.language(),
      otpHttpClient
    );
    var mapper = new GbfsFeedMapper(loader, params);

    assertTrue(loader.update());

    List<VehicleRentalPlace> stations = mapper.getUpdates();
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
    Map<String, RentalVehicleType> vehicleTypes = GbfsFeedMapper.mapVehicleTypes(
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
      GbfsFeedMapper.mapVehicleTypes(vehicleTypeMapper, vehicleTypes);
    });
  }

  @Test
  void getOneVehicleTypeOfDuplicatedVehicleTypes() {
    GbfsVehicleTypeMapper vehicleTypeMapper = new GbfsVehicleTypeMapper("systemID");

    List<GBFSVehicleType> duplicatedVehicleTypes = getDuplicatedGbfsVehicleTypes();

    Map<String, RentalVehicleType> vehicleTypes = GbfsFeedMapper.mapVehicleTypes(
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
      .filter(z -> z.name().toString().equals("NP Frogner og vigelandsparken"))
      .findFirst()
      .get();

    assertTrue(frognerPark.dropOffBanned());
    assertFalse(frognerPark.traversalBanned());

    var businessAreas = zones.stream().filter(GeofencingZone::isBusinessArea).toList();

    assertEquals(1, businessAreas.size());

    assertEquals("OSLO Summer 2021", businessAreas.get(0).name().toString());
    assertEquals("tieroslo:4640262c", businessAreas.get(0).id().toString());
  }

  @Test
  void makeStationFromV10() {
    var network = "helsinki_gbfs";
    var params = new GbfsVehicleRentalDataSourceParameters(
      "file:src/test/resources/gbfs/helsinki/gbfs.json",
      "en",
      false,
      HttpHeaders.empty(),
      network,
      false,
      true,
      RentalPickupType.ALL
    );
    var otpHttpClient = new OtpHttpClientFactory()
      .create(LoggerFactory.getLogger(GbfsFeedMapperTest.class));
    var loader = new GbfsFeedLoader(
      params.url(),
      params.httpHeaders(),
      params.language(),
      otpHttpClient
    );
    var mapper = new GbfsFeedMapper(loader, params);

    assertTrue(loader.update());

    List<VehicleRentalPlace> stations = mapper.getUpdates();
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
