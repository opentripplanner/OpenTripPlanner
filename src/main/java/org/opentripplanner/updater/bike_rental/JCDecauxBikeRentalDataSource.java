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
 * Implementation of a BikeRentalDataSource for the generic JCDecaux Open-Data API.
 * 
 * @link https://developer.jcdecaux.com
 * @see BikeRentalDataSource
 */
public class JCDecauxBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    public JCDecauxBikeRentalDataSource() {
        super("");
    }

    /**
     * JSON JCDecaux API v1 format:
     * 
     * <pre>
     * [ { 
     *     "number" : 94,
     *     "name" : "00094-PETIT PORT",
     *     "address" : "PETIT PORT - BD DU PETIT PORT",
     *     "position" : {
     *       "lat" : 47.243263914975486,
     *       "lng" : -1.556344610167984 },
     *     "banking" : true,
     *     "bonus" : false,
     *     "status" : "OPEN",
     *     "bike_stands" : 20,
     *     "available_bike_stands" : 1,
     *     "available_bikes" : 19,
     *     "last_update" : 1368611914000
     *   },
     *   ...
     * ]
     * </pre>
     */
    public BikeRentalStation makeStation(JsonNode node) {
        if (!node.path("status").asText().equals("OPEN")) {
            return null;
        }
        BikeRentalStation station = new BikeRentalStation();
        station.id = String.format("%d", node.path("number").asInt());
        station.x = node.path("position").path("lng").asDouble();
        station.y = node.path("position").path("lat").asDouble();
        station.name = new NonLocalizedString(node.path("name").asText());
        station.bikesAvailable = node.path("available_bikes").asInt();
        station.spacesAvailable = node.path("available_bike_stands").asInt();
        return station;
    }
}
