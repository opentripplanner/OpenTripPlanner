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
        android = new VehicleRentalSystemAppInformation(
          systemInformation.getRentalApps().getAndroid().getStoreUri(),
          systemInformation.getRentalApps().getAndroid().getDiscoveryUri()
        );
      }
      if (systemInformation.getRentalApps().getIos() != null) {
        ios = new VehicleRentalSystemAppInformation(
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
      systemInformation.getLicenseUrl(),
      systemInformation.getTimezone().value(),
      android,
      ios
    );
  }
}
