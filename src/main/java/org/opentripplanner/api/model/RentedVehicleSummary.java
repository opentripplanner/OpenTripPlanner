package org.opentripplanner.api.model;

import java.util.Set;

/**
 * Detailed data about a rental vehicle included with itinerary {@link Leg}.
 */
public class RentedVehicleSummary {
    public Set<String> companies;

    public RentedVehicleSummary(Set<String> companies) {
        this.companies = companies;
    }
}
