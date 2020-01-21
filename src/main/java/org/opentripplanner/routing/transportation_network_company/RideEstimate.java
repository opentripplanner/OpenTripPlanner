package org.opentripplanner.routing.transportation_network_company;

// A class to model the estimated ride time while using service from a transportation network company
public class RideEstimate {

    public TransportationNetworkCompany company;
    public String currency;
    public int duration;  // in seconds
    public double maxCost;
    public double minCost;
    public String rideType;
    public boolean wheelchairAccessible;

    public RideEstimate(
        TransportationNetworkCompany company,
        String currency,
        int duration,
        double maxCost,
        double minCost,
        String rideType,
        boolean wheelchairAccessible
    ) {
        this.company = company;
        this.currency = currency;
        this.duration = duration;
        this.maxCost = maxCost;
        this.minCost = minCost;
        this.rideType = rideType;
        this.wheelchairAccessible = wheelchairAccessible;
    }
}
