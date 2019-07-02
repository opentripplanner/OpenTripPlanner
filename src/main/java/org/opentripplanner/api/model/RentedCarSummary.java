package org.opentripplanner.api.model;

import java.util.Set;

/**
 * Detailed data about a rental car included with itinerary {@link Leg}.
 */
public class RentedCarSummary {
    public Set<String> companies;

    public RentedCarSummary(Set<String> companies) {
        this.companies = companies;
    }
}
