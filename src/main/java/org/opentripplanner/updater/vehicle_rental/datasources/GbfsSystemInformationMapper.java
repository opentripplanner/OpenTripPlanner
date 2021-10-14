package org.opentripplanner.updater.vehicle_rental.datasources;

import org.entur.gbfs.v2_2.system_information.GBFSData;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Locale;
import java.util.TimeZone;

public class GbfsSystemInformationMapper {
    private static final Logger LOG = LoggerFactory.getLogger(GbfsSystemInformationMapper.class);

    public VehicleRentalSystem mapSystemInformation(GBFSData systemInformation) {
        VehicleRentalSystem.AppInformation android = null;
        VehicleRentalSystem.AppInformation ios = null;

        if (systemInformation.getRentalApps() != null) {
            if (systemInformation.getRentalApps().getAndroid() != null) {
                try {
                    android = new VehicleRentalSystem.AppInformation(
                            new URI(systemInformation.getRentalApps().getAndroid().getStoreUri()),
                            new URI(systemInformation.getRentalApps().getAndroid().getDiscoveryUri())
                    );
                } catch (URISyntaxException e) {
                    LOG.warn("Unable to parse rental URIs");
                }
            }
            if (systemInformation.getRentalApps().getIos() != null) {
                try {
                    ios = new VehicleRentalSystem.AppInformation(
                            new URI(systemInformation.getRentalApps().getIos().getStoreUri()),
                            new URI(systemInformation.getRentalApps().getIos().getDiscoveryUri())
                    );
                } catch (URISyntaxException e) {
                    LOG.warn("Unable to parse rental URIs");
                }
            }
        }

        URL url = null;
        if (systemInformation.getUrl() != null) {
            try {
                url = new URL(systemInformation.getUrl());
            } catch (MalformedURLException e) {
                LOG.warn("Unable to parse system URL");
            }
        }

        URL purchaseUrl = null;
        if (systemInformation.getPurchaseUrl() != null) {
            try {
                purchaseUrl = new URL(systemInformation.getPurchaseUrl());
            } catch (MalformedURLException e) {
                LOG.warn("Unable to parse system URL");
            }
        }

        return new VehicleRentalSystem(
                systemInformation.getSystemId(),
                Locale.forLanguageTag(systemInformation.getLanguage()),
                systemInformation.getName(),
                systemInformation.getShortName(),
                systemInformation.getOperator(),
                url,
                purchaseUrl,
                systemInformation.getStartDate() != null
                        ? LocalDate.parse(systemInformation.getStartDate())
                        : null,
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
