package org.opentripplanner.api.model;

import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.RideEstimate;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class TransportationNetworkCompanyResponse {

    private String error;
    private List<ArrivalTime> estimates;
    private RideEstimate rideEstimate;

    @XmlElement(required=false)
    public List<ArrivalTime> getEstimates() {
        return estimates;
    }

    public void setEtaEstimates(List<ArrivalTime> estimates) {
        this.estimates = estimates;
    }

    @XmlElement(required=false)
    public RideEstimate getRideEstimate() {
        return rideEstimate;
    }

    public void setRideEstimate(RideEstimate rideEstimate) {
        this.rideEstimate = rideEstimate;
    }

    /** The error (if any) that this response raised. */
    @XmlElement(required=false)
    public String getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error.getClass().getSimpleName() + ": " + error.getMessage();
    }
}
