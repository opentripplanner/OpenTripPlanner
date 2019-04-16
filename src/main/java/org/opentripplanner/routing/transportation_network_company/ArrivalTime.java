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

// A class to model estimated arrival times of service from a transportation network company
public class ArrivalTime {
    public TransportationNetworkCompany company;
    public String productId;
    public String displayName;
    public int estimatedSeconds;
    public boolean wheelchairAccessible;

    public ArrivalTime(
        TransportationNetworkCompany company,
        String productId,
        String displayName,
        int estimatedSeconds,
        boolean wheelchairAccessible
    ) {
        this.company = company;
        this.productId = productId;
        this.displayName = displayName;
        this.estimatedSeconds = estimatedSeconds;
        this.wheelchairAccessible = wheelchairAccessible;
    }
}
