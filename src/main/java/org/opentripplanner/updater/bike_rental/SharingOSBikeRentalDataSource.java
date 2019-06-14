package org.opentripplanner.updater.bike_rental;

import java.util.HashSet;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Implementation of a BikeRentalDataSource for the SharingOS API used in Vantaa.
 * @see BikeRentalDataSource
 */
public class SharingOSBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private static final Logger log = LoggerFactory.getLogger(SharingOSBikeRentalDataSource.class);

    private String networkName;

    public SharingOSBikeRentalDataSource(String networkName) {
        super("data");
        this.networkName = defaultIfEmpty(networkName, "sharingos");
    }
    
    private String defaultIfEmpty(String value, String defaultValue) {
        if (value == null || value.isEmpty())
            return defaultValue;
        
        return value;
    }

    public BikeRentalStation makeStation(JsonNode node) {
        BikeRentalStation station = new BikeRentalStation();
        station.id = node.path("id").asText();
        station.name = new NonLocalizedString(node.path("name").asText());
        station.state = node.path("style").asText();
        station.networks = new HashSet<String>();
        station.networks.add(this.networkName);
        try {
            station.y = node.path("latitude").asDouble();
            station.x = node.path("longitude").asDouble();
            if (node.path("is_enable").asInt() == 0) {
                station.state = "Station on";
                station.bikesAvailable = node.path("available_capacity").asInt() < 0 ? 0 :
                        node.path("available_capacity").asInt();
                station.spacesAvailable = station.bikesAvailable >
                        node.path("total_capacity").asInt() ? 0 :
                        node.path("total_capacity").asInt() - station.bikesAvailable;
            } else if (node.path("is_enable").asInt() == 1) {
                station.state = "Station off";
                station.spacesAvailable = 0;
                station.bikesAvailable = 0;
            } else {
                station.state = "Station closed";
                station.spacesAvailable = 0;
                station.bikesAvailable = 0;
            }
            return station;
        } catch (NumberFormatException e) {
            // E.g. coordinates is empty
            log.info("Error parsing bike rental station " + station.id, e);
            return null;
        }
    }
}
