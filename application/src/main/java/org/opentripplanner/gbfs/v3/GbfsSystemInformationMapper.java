package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3.GbfsFeedMapper.localizedString;
import static org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3.GbfsFeedMapper.optionalLocalizedString;

import javax.annotation.Nullable;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSData;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSName;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSOperator;
import org.mobilitydata.gbfs.v3_0.system_information.GBFSShortName;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;

class GbfsSystemInformationMapper {

  public VehicleRentalSystem mapSystemInformation(
    GBFSData systemInformation,
    @Nullable String network
  ) {
    String systemId = network != null ? network : systemInformation.getSystemId();

    return new VehicleRentalSystem(
      systemId,
      localizedString(systemInformation.getName(), GBFSName::getLanguage, GBFSName::getText),
      optionalLocalizedString(
        systemInformation.getShortName(),
        GBFSShortName::getLanguage,
        GBFSShortName::getText
      ),
      optionalLocalizedString(
        systemInformation.getOperator(),
        GBFSOperator::getLanguage,
        GBFSOperator::getText
      ),
      systemInformation.getUrl()
    );
  }
}
