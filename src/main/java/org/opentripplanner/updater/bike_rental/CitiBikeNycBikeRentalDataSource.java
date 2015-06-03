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

package org.opentripplanner.updater.bike_rental;

import java.util.HashSet;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Build a BikeRentalStation object from CitiBike data source JsonNode object.
 *
 * @see GenericJsonBikeRentalDataSource
 */
public class CitiBikeNycBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private String networkName;

    public CitiBikeNycBikeRentalDataSource(String networkName) {
        super("stationBeanList");
        if (networkName != null && !networkName.isEmpty()) {
            this.networkName = networkName;
        } else {
            this.networkName = "CitiBike";
        }
    }

    public BikeRentalStation makeStation(JsonNode stationNode) {

        if (!stationNode.path("statusValue").asText().equals("In Service")) {
            return null;
        }

        if (stationNode.path("testStation").asText().equals("true")) {
            return null;
        }

        BikeRentalStation brstation = new BikeRentalStation();

        brstation.networks = new HashSet<String>();
        brstation.networks.add(this.networkName);

        brstation.id = stationNode.path("id").toString();
        brstation.x = stationNode.path("longitude").asDouble();
        brstation.y = stationNode.path("latitude").asDouble();
        brstation.name =  new NonLocalizedString(stationNode.path("stationName").asText());
        brstation.bikesAvailable = stationNode.path("availableBikes").asInt();
        brstation.spacesAvailable = stationNode.path("availableDocks").asInt();

        return brstation;
    }
}
