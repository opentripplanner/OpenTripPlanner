package org.opentripplanner.updater.vehicle_rental.datasources;

import java.util.TimeZone;
import org.entur.gbfs.v2_2.system_information.GBFSData;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalSystem;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalSystemAppInformation;

public class GbfsSystemInformationMapper {

  public VehicleRentalSystem mapSystemInformation(GBFSData systemInformation, String network) {
    VehicleRentalSystemAppInformation android = null;
    VehicleRentalSystemAppInformation ios = null;

    if (systemInformation.getRentalApps() != null) {
      if (systemInformation.getRentalApps().getAndroid() != null) {
        android =
          new VehicleRentalSystemAppInformation(
            systemInformation.getRentalApps().getAndroid().getStoreUri(),
            systemInformation.getRentalApps().getAndroid().getDiscoveryUri()
          );
      }
      if (systemInformation.getRentalApps().getIos() != null) {
        ios =
          new VehicleRentalSystemAppInformation(
            systemInformation.getRentalApps().getIos().getStoreUri(),
            systemInformation.getRentalApps().getIos().getDiscoveryUri()
          );
      }
    }

    String systemId = network != null ? network : systemInformation.getSystemId();

    return new VehicleRentalSystem(
      systemId,
      systemInformation.getLanguage(),
      systemInformation.getName(),
      systemInformation.getShortName(),
      systemInformation.getOperator(),
      systemInformation.getUrl(),
      systemInformation.getPurchaseUrl(),
      systemInformation.getStartDate(),
      systemInformation.getPhoneNumber(),
      systemInformation.getEmail(),
      systemInformation.getFeedContactEmail(),
      TimeZone.getTimeZone(systemInformation.getTimezone()),
      systemInformation.getLicenseUrl(),
      android,
      ios
    );
  }
}
