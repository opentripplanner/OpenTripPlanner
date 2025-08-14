package org.opentripplanner.updater.vehicle_rental.datasources;

import org.mobilitydata.gbfs.v2_3.system_information.GBFSData;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystemAppInformation;

public class GbfsSystemInformationMapper {

  public VehicleRentalSystem mapSystemInformation(GBFSData systemInformation, String network) {
    VehicleRentalSystemAppInformation android = null;
    VehicleRentalSystemAppInformation ios = null;

    if (systemInformation.getRentalApps() != null) {
      if (systemInformation.getRentalApps().getAndroid() != null) {
        android = VehicleRentalSystemAppInformation.of()
          .withStoreUri(systemInformation.getRentalApps().getAndroid().getStoreUri())
          .withDiscoveryUri(systemInformation.getRentalApps().getAndroid().getDiscoveryUri())
          .build();
      }
      if (systemInformation.getRentalApps().getIos() != null) {
        ios = VehicleRentalSystemAppInformation.of()
          .withStoreUri(systemInformation.getRentalApps().getIos().getStoreUri())
          .withDiscoveryUri(systemInformation.getRentalApps().getIos().getDiscoveryUri())
          .build();
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
      systemInformation.getLicenseUrl(),
      systemInformation.getTimezone().value(),
      android,
      ios
    );
  }
}
