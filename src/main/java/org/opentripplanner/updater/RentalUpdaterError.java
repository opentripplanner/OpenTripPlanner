package org.opentripplanner.updater;

public class RentalUpdaterError {
    /**
     * A description of the error.
     */
    public final String message;

    /**
     * The severity type of the error.
     */
    public final Severity severity;

    public RentalUpdaterError(Severity severity, String message) {
        this.message = message;
        this.severity = severity;
    }

    public enum Severity {
        /**
         * An error that affects all elements of the data feed.
         */
        FEED_WIDE,
        /**
         * An error that made it impossible to parse the system information. Information about docking stations and
         * vehicles might still be available.
         */
        SYSTEM_INFORMATION,
        /**
         * An error that made it impossible to parse the docking station information (either the station_information or
         * station_status files)
         */
        ALL_STATIONS,
        /**
         * An error that made it impossible to parse the floating vehicle information.
         */
        ALL_FLOATING_VEHICLES,
        /**
         * An error that affects just an individual docking station
         */
        INDIVIDUAL_DOCKING_STATION,
        /**
         * An error that affects just an individual vehicle
         */
        INDIVIDUAL_VEHICLE
    }
}
