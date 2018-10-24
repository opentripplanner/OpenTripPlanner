package org.opentripplanner.api.model;

import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.RideEstimate;
import org.opentripplanner.routing.transportation_network_company.TransportationNetworkCompany;

/**
 * Simplified TNC summary included with itinerary {@link Leg}.
 */
public class TransportationNetworkCompanySummary {
    public TransportationNetworkCompany company;
    public String currency;
    public int travelDuration;  // in seconds
    public double maxCost;
    public double minCost;
    public String productId;
    public String displayName;
    public int estimatedArrival;

    public TransportationNetworkCompanySummary (RideEstimate estimate, ArrivalTime time) {
        if (estimate != null) {
            this.company = estimate.company;
            this.currency = estimate.currency;
            this.travelDuration = estimate.duration;
            this.maxCost = estimate.maxCost;
            this.minCost = estimate.minCost;
        }
        if (time != null) {
            this.productId = time.productId;
            this.displayName = time.displayName;
            this.estimatedArrival = time.estimatedSeconds;
        }
    }
}
