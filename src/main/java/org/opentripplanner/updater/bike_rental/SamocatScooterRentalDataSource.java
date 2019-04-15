package org.opentripplanner.updater.bike_rental;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Implementation of a BikeRentalDataSource for the Samocat scooter rental service used in Helsinki.
 * Since scooters rental works similarly as bike rental, there is no point to create own data structures
 * for them.
 * @see BikeRentalDataSource
 */
public class SamocatScooterRentalDataSource extends GenericJsonBikeRentalDataSource {

    private static final Logger log = LoggerFactory.getLogger(SamocatScooterRentalDataSource.class);

    private String networkName;

    public SamocatScooterRentalDataSource(String networkName) {
        super("results");
        this.networkName = defaultIfEmpty(networkName, "samocat");
    }
    
    private String defaultIfEmpty(String value, String defaultValue) {
        if (value == null || value.isEmpty())
            return defaultValue;
        
        return value;
    }

    /**
     * <pre>
     * {
     * "count": 10,
     * "next": null,
     * "previous": null,
     * "results": [
     *     {
     *       "type": "Feature",
     *       "geometry": {
     *           "type": "Point",
     *           "coordinates": [
     *             60.167913,
     *             24.952269
     *           ]
     *        },
     *       "properties": {
     *           "station_id": "0309",
     *           "city": "Helsinki",
     *           "country": "Finland",
     *           "address": "some address",
     *           "rack_sum": 12,
     *           "free_racks": 2,
     *           "available_devices": 1
     *       }
     *     }
     *   ]
     * }
     * </pre>
     */
    public BikeRentalStation makeStation(JsonNode node) {
        BikeRentalStation station = new BikeRentalStation();
        JsonNode properties = node.path("properties");
        JsonNode coordinates = node.path("geometry").path("coordinates");
        station.id = properties.path("station_id").asText();
        station.name = new NonLocalizedString(properties.path("address").asText());
        station.state = "Station on";
        station.networks = new HashSet<String>();
        station.networks.add(this.networkName);
        try {
            if (coordinates.get(0).isNull() || coordinates.isNull()) {
                return null;
            }
            station.x = coordinates.get(0).asDouble();
            station.y = coordinates.get(1).asDouble();
            station.bikesAvailable = properties.path("available_devices").asInt();
            station.spacesAvailable = properties.path("free_racks").asInt();
            return station;
        } catch (NumberFormatException e) {
            // E.g. coordinates is empty
            log.info("Error parsing bike rental station " + station.id, e);
            return null;
        }
    }
}