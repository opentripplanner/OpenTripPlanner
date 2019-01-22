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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Implementation of a BikeRentalDataSource for the Smoove GIR SabiWeb used in Helsinki.
 * @see BikeRentalDataSource
 */
public class SmooveBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private static final Logger log = LoggerFactory.getLogger(SmooveBikeRentalDataSource.class);

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
     *          "coordinates" : "60.167913,24.952269",
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
        BikeRentalStation station = new BikeRentalStation();
        station.id = node.path("name").asText().split("\\s", 2)[0];
        station.name = new NonLocalizedString(node.path("name").asText().split("\\s", 2)[1]);
        String[] coordinates = node.path("coordinates").asText().split(",");
        try {
            station.y = Double.parseDouble(coordinates[0].trim());
            station.x = Double.parseDouble(coordinates[1].trim());
        } catch (NumberFormatException e) {
            // E.g. coordinates is empty
            log.warn("Error parsing bike rental station " + station.id, e);
            return null;
        }
        if (!node.path("operative").asText().equals("true")) {
            station.bikesAvailable = 0;
            station.spacesAvailable = 0;
        } else {
            station.bikesAvailable = node.path("avl_bikes").asInt();
            station.spacesAvailable = node.path("free_slots").asInt();
        }
        return station;
    }
}
