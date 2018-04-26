package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.api.resource.BikeRental;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class CoordBikeRentalDataSource implements BikeRentalDataSource, JsonConfigurable {

    private static final Logger LOG = LoggerFactory.getLogger(CoordBikeRentalDataSource.class);

    private CoordBikeDataSource stationSource;
    private CoordFakeStationDataSource fakeSource;

    public CoordBikeRentalDataSource () {
        // TODO(danieljy): Set this from configuration.
        stationSource = new CoordBikeDataSource();
        stationSource.setUrl("https://api.coord.co/v1/bike/location?latitude=38.9072&longitude=-77.0369&radius_km=10&access_key=");

        fakeSource = new CoordFakeStationDataSource();
        fakeSource.setUrl("file:coord/test-gbfs/fake_stations.json");
    }


    @Override
    public boolean update() {
        return stationSource.update() && fakeSource.update();
    }

    @Override
    public List<BikeRentalStation> getStations() {
        // Copy the full list of station objects (with status updates) into a List, appending the floating bike stations.
        List<BikeRentalStation> stations = new LinkedList<>(stationSource.getStations());
        stations.addAll(fakeSource.getStations());
        return stations;
    }

    /**
     * Note that the JSON being passed in here is for configuration of the OTP component, it's completely separate
     * from the JSON coming in from the update source.
     */
    @Override
    public void configure (Graph graph, JsonNode jsonNode) {
        // This allows conifguration from the router-config.json.
        // TODO(danieljy): Implement this.
    }

    class CoordBikeDataSource extends GenericJsonBikeRentalDataSource {

        public CoordBikeDataSource () {
            super("features");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            JsonNode properties = stationNode.path("properties");
            JsonNode coordinates = stationNode.path("geometry").path("coordinates");
            Iterator<JsonNode> coordinateIterator = coordinates.iterator();

            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("id").toString();
            brstation.name = new NonLocalizedString(stationNode.path("id").toString());
            brstation.x =  coordinateIterator.next().asDouble();
            brstation.y =  coordinateIterator.next().asDouble();

            JsonNode numBikes = properties.get("num_bikes_available");
            JsonNode numDocks = properties.get("num_docks_available");
            JsonNode isReturning = properties.get("is_returning");

            brstation.bikesAvailable = numBikes != null ? numBikes.asInt() : 0;
            brstation.spacesAvailable = numDocks != null ? numDocks.asInt() : 0;
            brstation.allowDropoff = isReturning != null ? isReturning.asBoolean() : false;
            // TODO(danieljy): Update with allowPickup once Mahmood adds this in.

            // TODO(danieljy): I don't think this is used in the codebase as is, but we should use the info so that
            // we don't have to fake end locations.
            brstation.isFloatingBike = properties.get("location_type").asText().equals("free_bike");


            // Set the system ID as the network.
            brstation.networks = new HashSet<>(Arrays.asList(properties.path("system_id").asText()));

            return brstation;
        }
    }

    class CoordFakeStationDataSource extends GenericJsonBikeRentalDataSource {

        public CoordFakeStationDataSource () {
            super("data/stations");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            BikeRentalStation brstation = new BikeRentalStation();

            brstation.id = stationNode.path("station_id").toString();
            brstation.x = stationNode.path("lon").asDouble();
            brstation.y = stationNode.path("lat").asDouble();
            brstation.name =  new NonLocalizedString(stationNode.path("name").asText());
            brstation.allowDropoff = true;
            brstation.bikesAvailable = 0;
            brstation.spacesAvailable = Integer.MAX_VALUE;
            // brstation.allowPickup = false;

            // Add all the DC dockless networks.
            brstation.networks = new HashSet<>(Arrays.asList("JumpDC", "MobikeDC", "SpinDC"));

            return brstation;
        }
    }
}
