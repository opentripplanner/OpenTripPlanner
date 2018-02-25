/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
