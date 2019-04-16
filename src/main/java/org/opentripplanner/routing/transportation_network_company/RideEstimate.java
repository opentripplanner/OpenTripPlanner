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
