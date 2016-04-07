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

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Implementation of a BikeRentalDataSource for the Smoove GIR SabiWeb used in Helsinki.
 * @see BikeRentalDataSource
 */
public class SmooveBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    public SmooveBikeRentalDataSource() {
        super("result");
    }

    /**
     * <pre>
     * {
     *    "result" : [
     *       {
     *          "name" : "004 Hamn",
     *          "operative" : true,
     *          "coordinates" : "60.167913, 24.952269",
     *          "style" : "",
     *          "avl_bikes" : 1,
     *          "free_slots" : 11,
     *          "total_slots" : 12,
     *       },
     *       ...
     *    ]
     * }
     * </pre>
     */
    public BikeRentalStation makeStation(JsonNode node) {
        if (!node.path("operative").asText().equals("true")) {
            return null;
        }
        BikeRentalStation station = new BikeRentalStation();
        station.id = node.path("name").asText();
        station.name = new NonLocalizedString(node.path("name").asText());
        station.y = Double.parseDouble(node.path("coordinates").asText().split(", ")[0]);
        station.x = Double.parseDouble(node.path("coordinates").asText().split(", ")[1]);
        station.bikesAvailable = node.path("avl_bikes").asInt();
        station.spacesAvailable = node.path("free_slots").asInt();
        return station;
    }
}
