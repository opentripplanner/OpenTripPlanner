package org.opentripplanner.api.model;

import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.SystemInformation;

import java.util.List;

/**
 * Information about a particular vehicle rental company
 */
public class RentalInfo {
    /**
     * A list of errors encountered while fetching and parsing the vehicle rental feed.
     */
    public final List<RentalUpdaterError> errors;

    /**
     * The system information about the vehicle rental company. This is take directly from the company's GBFS system
     * information data if it is available.
     */
    public final SystemInformation.SystemInformationData systemInformationData;

    public RentalInfo(
        List<RentalUpdaterError> errors, SystemInformation.SystemInformationData systemInformationData
    ) {
        this.errors = errors;
        this.systemInformationData = systemInformationData;
    }
}
