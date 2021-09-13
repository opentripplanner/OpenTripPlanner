package org.opentripplanner.routing.vehicle_rental;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.Locale;
import java.util.TimeZone;

public class VehicleRentalSystem {
    public final String systemId;
    public final Locale language;
    public final String name;
    public final String shortName;
    public final String operator;
    public final URL url;
    public final URL purchaseUrl;
    public final LocalDate startDate;
    public final String phoneNumber;
    public final String email;
    public final String feedContactEmail;
    public final TimeZone timezone;
    public final String licenseUrl;
    public final AppInformation androidApp;
    public final AppInformation iosApp;

    public VehicleRentalSystem(
            String systemId,
            Locale language,
            String name,
            String shortName,
            String operator,
            URL url,
            URL purchaseUrl,
            LocalDate startDate,
            String phoneNumber,
            String email,
            String feedContactEmail,
            TimeZone timezone,
            String licenseUrl,
            AppInformation androidApp,
            AppInformation iosApp
    ) {
        this.systemId = systemId;
        this.language = language;
        this.name = name;
        this.shortName = shortName;
        this.operator = operator;
        this.url = url;
        this.purchaseUrl = purchaseUrl;
        this.startDate = startDate;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.feedContactEmail = feedContactEmail;
        this.timezone = timezone;
        this.licenseUrl = licenseUrl;
        this.androidApp = androidApp;
        this.iosApp = iosApp;
    }

    public static class AppInformation {
        public final URI storeUri;
        public final URI discoveryUri;

        public AppInformation(URI storeUri, URI discoveryUri) {
            this.storeUri = storeUri;
            this.discoveryUri = discoveryUri;
        }
    }
}
