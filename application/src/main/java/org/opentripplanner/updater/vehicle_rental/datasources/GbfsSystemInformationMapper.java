package org.opentripplanner.updater.vehicle_rental.datasources;

import org.mobilitydata.gbfs.v2_3.system_information.GBFSData;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;

public class GbfsSystemInformationMapper {

  public VehicleRentalSystem mapSystemInformation(GBFSData systemInformation, String network) {
    String systemId = network != null ? network : systemInformation.getSystemId();

    return new VehicleRentalSystem(
      systemId,
      systemInformation.getLanguage(),
      systemInformation.getName(),
      systemInformation.getShortName(),
      systemInformation.getOperator(),
      systemInformation.getUrl()
    );
  }
}
