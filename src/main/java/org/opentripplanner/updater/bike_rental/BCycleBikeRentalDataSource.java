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
 * Build a BikeRentalStation object from a B-Cycle data source JsonNode object.
 *
 * @see GenericJsonBikeRentalDataSource
 */
public class BCycleBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private String networkName;

    public BCycleBikeRentalDataSource(String apiKey, String networkName) {
        super("", apiKey);
        if (networkName != null && !networkName.isEmpty()) {
            this.networkName = networkName;
        } else {
            this.networkName = "B-Cycle";
        }
    }

    public BikeRentalStation makeStation(JsonNode kioskNode) {

        if (!kioskNode.path("Status").asText().equals("Active")) {
            return null;
        }

        BikeRentalStation brstation = new BikeRentalStation();

        brstation.networks = new HashSet<String>();
        brstation.networks.add(this.networkName);

        brstation.id = kioskNode.path("Id").toString();
        brstation.x = kioskNode.path("Location").path("Longitude").asDouble();
        brstation.y = kioskNode.path("Location").path("Latitude").asDouble();
        brstation.name =  new NonLocalizedString(kioskNode.path("Name").asText());
        brstation.bikesAvailable = kioskNode.path("BikesAvailable").asInt();
        brstation.spacesAvailable = kioskNode.path("DocksAvailable").asInt();

        return brstation;
    }
}
