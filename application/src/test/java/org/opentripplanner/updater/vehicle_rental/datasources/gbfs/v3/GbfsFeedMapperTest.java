package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleType;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.datasources.gbfs.GbfsVehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;
import org.slf4j.LoggerFactory;

/**
 * This tests the mapping between data coming from a {@link GbfsFeedLoader} to OTP station
 * models.
 */
class GbfsFeedMapperTest {

  @Test
  void makeStationFromV30() {
    var params = new GbfsVehicleRentalDataSourceParameters(
      "file:src/test/resources/gbfs/ridecheck/almere/gbfs.json",
      null,
      false,
      HttpHeaders.empty(),
      null,
      false,
      false,
      RentalPickupType.ALL
    );
    var otpHttpClient = new OtpHttpClientFactory()
      .create(LoggerFactory.getLogger(GbfsFeedMapperTest.class));
    var loader = new GbfsFeedLoader(params.url(), params.httpHeaders(), otpHttpClient);
    var mapper = new GbfsFeedMapper(loader, params);

    assertTrue(loader.update());

    List<VehicleRentalPlace> stations = mapper.getUpdates();
    assertEquals(6, stations.size());

    assertTrue(
      stations
        .stream()
        .allMatch(vehicleRentalPlace ->
          vehicleRentalPlace.availablePickupFormFactors(true).equals(Set.of(RentalFormFactor.MOPED))
        )
    );
    assertTrue(stations.stream().allMatch(VehicleRentalPlace::isFloatingVehicle));
    assertTrue(stations.stream().noneMatch(VehicleRentalPlace::isCarStation));
    assertTrue(stations.stream().noneMatch(VehicleRentalPlace::overloadingAllowed));
    assertTrue(
      stations
        .stream()
        .allMatch(vehicleRentalStation -> vehicleRentalStation.network().equals("check_almere"))
    );
    assertTrue(
      stations
        .stream()
        .noneMatch(vehicleRentalStation ->
          vehicleRentalStation.isArrivingInRentalVehicleAtDestinationAllowed()
        )
    );

    assertEquals(4, stations.stream().filter(VehicleRentalPlace::allowPickupNow).count());
    assertEquals(5, stations.stream().filter(VehicleRentalPlace::isAllowPickup).count());
    assertEquals(0, stations.stream().filter(VehicleRentalPlace::allowDropoffNow).count());
    assertEquals(0, stations.stream().filter(VehicleRentalPlace::isAllowDropoff).count());

    assertTrue(
      stations
        .stream()
        .allMatch(vehicleRentalPlace ->
          vehicleRentalPlace.name().toString().equals("Default vehicle type")
        )
    );

    var system = stations.getFirst().vehicleRentalSystem();
    assertEquals("check_almere", system.systemId());
    assertEquals(
      TranslatedString.getI18NString(
        Map.of("en", "Check Technologies", "nl", "Check Technologies (nl)"),
        false,
        false
      ),
      system.name()
    );
    assertNull(system.shortName());
    assertNull(system.operator());
    assertNull(system.url());
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
        "file:src/test/resources/gbfs/ridecheck/almere/gbfs.json",
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

    assertEquals(14, zones.size());

    var hubBergnet = zones
      .stream()
      .filter(z -> z.name().toString().equals("Hub Bergnet"))
      .findFirst()
      .get();

    assertTrue(hubBergnet.dropOffBanned());
    assertFalse(hubBergnet.traversalBanned());

    var almereHaven = zones
      .stream()
      .filter(z -> z.name().toString().equals("Almere Haven"))
      .findFirst()
      .get();

    assertFalse(almereHaven.dropOffBanned());
    assertTrue(almereHaven.traversalBanned());

    var businessAreas = zones.stream().filter(GeofencingZone::isBusinessArea).toList();

    assertEquals(12, businessAreas.size());
    var almereStad = zones
      .stream()
      .filter(z -> z.name().toString().equals("Almere Stad"))
      .findFirst()
      .get();

    assertEquals("Almere Stad", almereStad.name().toString(Locale.forLanguageTag("en")));
    assertEquals("Almere Stad (nl)", almereStad.name().toString(Locale.forLanguageTag("nl")));
    assertEquals("check_almere:fb345775", almereStad.id().toString());
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
