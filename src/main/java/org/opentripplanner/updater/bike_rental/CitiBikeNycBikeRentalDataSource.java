package org.opentripplanner.updater.bike_rental;

import java.util.HashSet;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.vehicle_rental.RentalStation;
import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.util.NonLocalizedString;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Build a BikeRentalStation object from CitiBike data source JsonNode object.
 *
 * @see GenericJsonBikeRentalDataSource
 */
public class CitiBikeNycBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private String networkName;

    public CitiBikeNycBikeRentalDataSource(String networkName) {
        super(RentalUpdaterError.Severity.ALL_STATIONS, "stationBeanList");
        if (networkName != null && !networkName.isEmpty()) {
            this.networkName = networkName;
        } else {
            this.networkName = "CitiBike";
        }
    }

    public BikeRentalStation makeStation(JsonNode stationNode, Integer feedUpdateEpochSeconds) {

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
        brstation.lastReportedEpochSeconds = RentalStation.getLastReportedTimeUsingFallbacks(
            stationNode.path("last_reported").asLong(),
            feedUpdateEpochSeconds
        );
        brstation.x = stationNode.path("longitude").asDouble();
        brstation.y = stationNode.path("latitude").asDouble();
        brstation.name =  new NonLocalizedString(stationNode.path("stationName").asText());
        brstation.bikesAvailable = stationNode.path("availableBikes").asInt();
        brstation.spacesAvailable = stationNode.path("availableDocks").asInt();

        return brstation;
    }
}
