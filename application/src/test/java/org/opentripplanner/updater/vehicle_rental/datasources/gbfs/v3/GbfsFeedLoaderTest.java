package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSGeofencingZones;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStationInformation;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSSystemInformation;
import org.mobilitydata.gbfs.v3_0.system_regions.GBFSSystemRegions;
import org.mobilitydata.gbfs.v3_0.vehicle_status.GBFSVehicleStatus;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleType;
import org.mobilitydata.gbfs.v3_0.vehicle_types.GBFSVehicleTypes;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.slf4j.LoggerFactory;

/**
 * This tests that {@link GbfsFeedLoader} handles loading of different versions of GBFS
 * correctly, that the optional language parameter works correctly, and that the different files in
 * a GBFS bundle are all included, with all information in them.
 */
class GbfsFeedLoaderTest {

  private static final OtpHttpClient otpHttpClient = new OtpHttpClientFactory()
    .create(LoggerFactory.getLogger(GbfsFeedLoaderTest.class));

  @Test
  void getV30Feed() {
    GbfsFeedLoader loader = new GbfsFeedLoader(
      "file:src/test/resources/gbfs/ridecheck/almere/gbfs.json",
      HttpHeaders.empty(),
      otpHttpClient
    );

    assertTrue(loader.update());

    GBFSSystemInformation systemInformation = loader.getFeed(GBFSSystemInformation.class);
    assertNotNull(systemInformation);
    assertEquals("check_almere", systemInformation.getData().getSystemId());
    assertEquals("Europe/Amsterdam", systemInformation.getData().getTimezone().value());
    assertNull(systemInformation.getData().getEmail());
    assertNull(systemInformation.getData().getOperator());
    assertNull(systemInformation.getData().getPhoneNumber());
    assertNull(systemInformation.getData().getShortName());
    assertNull(systemInformation.getData().getUrl());
    assertEquals("en", systemInformation.getData().getName().getFirst().getLanguage());
    assertEquals("Check Technologies", systemInformation.getData().getName().getFirst().getText());
    assertEquals("nl", systemInformation.getData().getName().getLast().getLanguage());
    assertEquals(
      "Check Technologies (nl)",
      systemInformation.getData().getName().getLast().getText()
    );

    GBFSVehicleTypes vehicleTypes = loader.getFeed(GBFSVehicleTypes.class);
    assertNotNull(vehicleTypes);
    assertEquals(1, vehicleTypes.getData().getVehicleTypes().size());
    GBFSVehicleType vehicleType = vehicleTypes.getData().getVehicleTypes().get(0);
    assertEquals("check_moped_almere_60", vehicleType.getVehicleTypeId());
    assertEquals(GBFSVehicleType.FormFactor.MOPED, vehicleType.getFormFactor());
    assertEquals(GBFSVehicleType.PropulsionType.ELECTRIC, vehicleType.getPropulsionType());
    assertNotNull(vehicleType.getMaxRangeMeters());

    GBFSVehicleStatus vehicleStatus = loader.getFeed(GBFSVehicleStatus.class);
    assertNotNull(vehicleStatus);
    assertEquals(6, vehicleStatus.getData().getVehicles().size());

    GBFSGeofencingZones geofencingZones = loader.getFeed(GBFSGeofencingZones.class);
    assertNotNull(geofencingZones);
    assertEquals(16, geofencingZones.getData().getGeofencingZones().getFeatures().size());

    assertNull(loader.getFeed(GBFSStationInformation.class));
    assertNull(loader.getFeed(GBFSSystemRegions.class));
  }
}
