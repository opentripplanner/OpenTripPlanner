package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

import java.util.HashSet;


/**
 * Build a BikeRentalStation object from BayAreaBikeShare data source JsonNode object.
 *
 * @see GenericJsonBikeRentalDataSource
 */
public class SanFranciscoBayAreaBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private String networkName;

    public SanFranciscoBayAreaBikeRentalDataSource(String networkName) {
        super("stationBeanList");
        if (networkName != null && !networkName.isEmpty()) {
            this.networkName = networkName;
        } else {
            this.networkName = "BayAreaBikeShare";
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
