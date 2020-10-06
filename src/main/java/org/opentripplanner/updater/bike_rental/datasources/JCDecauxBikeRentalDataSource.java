package org.opentripplanner.updater.bike_rental.datasources;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.util.NonLocalizedString;

/**
 * Implementation of a BikeRentalDataSource for the generic JCDecaux Open-Data API.
 * 
 * See https://developer.jcdecaux.com
 * @see BikeRentalDataSource
 */
class JCDecauxBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    public JCDecauxBikeRentalDataSource(BikeRentalDataSourceParameters config) {
        super(config, "");
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
