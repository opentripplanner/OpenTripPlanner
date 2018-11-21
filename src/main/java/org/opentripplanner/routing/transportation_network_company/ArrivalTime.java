package org.opentripplanner.routing.transportation_network_company;

// A class to model estimated arrival times of service from a transportaiton network company
public class ArrivalTime {
    public TransportationNetworkCompany company;
    public String productId;
    public String displayName;
    public int estimatedSeconds;

    public ArrivalTime(TransportationNetworkCompany company, String productId, String displayName, int estimatedSeconds) {
        this.company = company;
        this.productId = productId;
        this.displayName = displayName;
        this.estimatedSeconds = estimatedSeconds;
    }
}
