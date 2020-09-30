package org.opentripplanner.updater.bike_rental.datasources;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.util.NonLocalizedString;

import java.util.HashSet;


/**
 * Build a BikeRentalStation object from a B-Cycle data source JsonNode object.
 *
 * @see GenericJsonBikeRentalDataSource
 */
class BCycleBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private final String networkName;

    BCycleBikeRentalDataSource(BikeRentalDataSourceParameters config) {
        super(config, "", "ApiKey", config.getApiKey());
        networkName = config.getNetwork("B-Cycle");
    }

    public BikeRentalStation makeStation(JsonNode kioskNode) {

        if (!kioskNode.path("Status").asText().equals("Active")) {
            return null;
        }

        BikeRentalStation brstation = new BikeRentalStation();

        brstation.networks = new HashSet<>();
        brstation.networks.add(this.networkName);

        brstation.id = kioskNode.path("Id").asText();
        brstation.x = kioskNode.path("Location").path("Longitude").asDouble();
        brstation.y = kioskNode.path("Location").path("Latitude").asDouble();
        brstation.name =  new NonLocalizedString(kioskNode.path("Name").asText());
        brstation.bikesAvailable = kioskNode.path("BikesAvailable").asInt();
        brstation.spacesAvailable = kioskNode.path("DocksAvailable").asInt();

        return brstation;
    }
}
