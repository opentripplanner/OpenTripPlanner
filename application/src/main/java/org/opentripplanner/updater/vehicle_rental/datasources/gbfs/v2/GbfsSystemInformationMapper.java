package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v2;

import org.mobilitydata.gbfs.v2_3.system_information.GBFSData;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;

class GbfsSystemInformationMapper {

  public VehicleRentalSystem mapSystemInformation(GBFSData systemInformation, String network) {
    String systemId = network != null ? network : systemInformation.getSystemId();

    return new VehicleRentalSystem(
      systemId,
      I18NString.of(systemInformation.getName()),
      NonLocalizedString.ofNullable(systemInformation.getShortName()),
      NonLocalizedString.ofNullable(systemInformation.getOperator()),
      systemInformation.getUrl()
    );
  }
}
