package org.opentripplanner.updater.vehicle_rental.datasources;

import org.entur.gbfs.v2_2.system_information.GBFSData;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalSystem;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalSystemAppInformation;

import java.util.TimeZone;

public class GbfsSystemInformationMapper {
    public VehicleRentalSystem mapSystemInformation(GBFSData systemInformation) {
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

        return new VehicleRentalSystem(
                systemInformation.getSystemId(),
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
