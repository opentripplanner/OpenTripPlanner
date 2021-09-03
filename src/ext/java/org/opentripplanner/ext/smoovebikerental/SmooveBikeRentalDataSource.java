package org.opentripplanner.ext.smoovebikerental;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.GenericJsonVehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a VehicleRentalDataSource for the Smoove GIR SabiWeb used in Helsinki.
 * @see VehicleRentalDataSource
 */
public class SmooveBikeRentalDataSource extends GenericJsonVehicleRentalDataSource<VehicleRentalDataSourceParameters> {

    private static final Logger log = LoggerFactory.getLogger(SmooveBikeRentalDataSource.class);

    public static final String DEFAULT_NETWORK_NAME = "smoove";

    public SmooveBikeRentalDataSource(VehicleRentalDataSourceParameters config) {
        super(config,"result");
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
    public VehicleRentalStation makeStation(JsonNode node) {
        VehicleRentalStation station = new VehicleRentalStation();
        station.id = node.path("name").asText().split("\\s", 2)[0];
        station.name = new NonLocalizedString(node.path("name").asText().split("\\s", 2)[1]);
        station.network = config.getNetwork(DEFAULT_NETWORK_NAME);
        String[] coordinates = node.path("coordinates").asText().split(",");
        try {
            station.latitude = Double.parseDouble(coordinates[0].trim());
            station.longitude = Double.parseDouble(coordinates[1].trim());
        } catch (NumberFormatException e) {
            // E.g. coordinates is empty
            log.warn("Error parsing bike rental station " + station.id, e);
            return null;
        }
        if (!node.path("operative").asText().equals("true")) {
            station.vehiclesAvailable = 0;
            station.spacesAvailable = 0;
        } else {
            station.vehiclesAvailable = node.path("avl_bikes").asInt();
            station.spacesAvailable = node.path("free_slots").asInt();
        }
        return station;
    }
}
