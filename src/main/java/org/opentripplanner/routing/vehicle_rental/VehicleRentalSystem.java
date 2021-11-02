package org.opentripplanner.routing.vehicle_rental;

import java.util.TimeZone;

/**
 * Based on https://github.com/NABSA/gbfs/blob/master/gbfs.md#system_informationjson
 */
public class VehicleRentalSystem {
    public final String systemId;
    public final String language;
    public final String name;
    public final String shortName;
    public final String operator;
    public final String url;
    public final String purchaseUrl;
    public final String startDate;
    public final String phoneNumber;
    public final String email;
    public final String feedContactEmail;
    public final TimeZone timezone;
    public final String licenseUrl;
    public final VehicleRentalSystemAppInformation androidApp;
    public final VehicleRentalSystemAppInformation iosApp;

    public VehicleRentalSystem(
            String systemId,
            String language,
            String name,
            String shortName,
            String operator,
            String url,
            String purchaseUrl,
            String startDate,
            String phoneNumber,
            String email,
            String feedContactEmail,
            TimeZone timezone,
            String licenseUrl,
            VehicleRentalSystemAppInformation androidApp,
            VehicleRentalSystemAppInformation iosApp
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

}
