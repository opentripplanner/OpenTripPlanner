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
        super("", "ApiKey",apiKey);
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
