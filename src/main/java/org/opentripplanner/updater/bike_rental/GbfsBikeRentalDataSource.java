package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalRegion;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by demory on 2017-03-14.
 */
public class GbfsBikeRentalDataSource implements BikeRentalDataSource, JsonConfigurable {

    private static final Logger LOG = LoggerFactory.getLogger(GbfsBikeRentalDataSource.class);

    private GbfsStationDataSource stationSource;
    private GbfsStationStatusDataSource stationStatusSource;
    private GbfsFloatingBikeDataSource floatingBikeSource;

    private String baseUrl;

    /** Some car rental systems and flex transit systems work exactly like bike rental, but with cars. */
    private boolean routeAsCar;

    public GbfsBikeRentalDataSource() {
        stationSource = new GbfsStationDataSource();
        stationStatusSource = new GbfsStationStatusDataSource();
        floatingBikeSource = new GbfsFloatingBikeDataSource();
    }

    public void setBaseUrl(String url) {
        baseUrl = url;
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        stationSource.setUrl(baseUrl + "station_information.json");
        stationStatusSource.setUrl(baseUrl + "station_status.json");
        // FIXME this is not a required file - it's only for systems with floating bikes
        floatingBikeSource.setUrl(baseUrl + "free_bike_status.json");
    }

    @Override
    public boolean update() {
        return stationSource.update() && stationStatusSource.update() && floatingBikeSource.update();
    }

    @Override
    public List<BikeRentalStation> getStations() {

        // Index all the station status entries on their station ID.
        Map<String, BikeRentalStation> statusLookup = new HashMap<>();
        for (BikeRentalStation station : stationStatusSource.getStations()) {
            statusLookup.put(station.id, station);
        }

        // Iterate over all known stations, and if we have any status information add it to those station objects.
        for (BikeRentalStation station : stationSource.getStations()) {
            if (!statusLookup.containsKey(station.id)) continue;
            BikeRentalStation status = statusLookup.get(station.id);
            station.bikesAvailable = status.bikesAvailable;
            station.spacesAvailable = status.spacesAvailable;
            station.allowDropoff = status.allowDropoff;
            station.allowPickup = status.allowPickup;
        }

        // Copy the full list of station objects (with status updates) into a List, appending the floating bike stations.
        List<BikeRentalStation> stations = new LinkedList<>(stationSource.getStations());
        stations.addAll(floatingBikeSource.getStations());
        return stations;
    }

    @Override
    public List<BikeRentalRegion> getRegions() {
        return new ArrayList<>();
    }

    /**
     * Note that the JSON being passed in here is for configuration of the OTP component, it's completely separate
     * from the JSON coming in from the update source.
     */
    @Override
    public void configure (Graph graph, JsonNode jsonNode) {
        // path() returns MissingNode not null, allowing chained function calls.
        String url = jsonNode.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' configuration.");
        }
        this.setBaseUrl(url);
        this.routeAsCar = jsonNode.path("routeAsCar").asBoolean(false);
        if (routeAsCar) {
            LOG.info("This 'bike rental' system will be treated as a car rental system.");
        }
    }

    class GbfsStationDataSource extends GenericJsonBikeStationDataSource {

        public GbfsStationDataSource() {
            super("data/stations");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("station_id").asText();
            brstation.x = stationNode.path("lon").asDouble();
            brstation.y = stationNode.path("lat").asDouble();
            brstation.name =  new NonLocalizedString(stationNode.path("name").asText());
            brstation.isCarStation = routeAsCar;
            return brstation;
        }
    }

    class GbfsStationStatusDataSource extends GenericJsonBikeStationDataSource {

        public GbfsStationStatusDataSource() {
            super("data/stations");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("station_id").asText();
            brstation.bikesAvailable = stationNode.path("num_bikes_available").asInt();
            brstation.spacesAvailable = stationNode.path("num_docks_available").asInt();
            brstation.isCarStation = routeAsCar;

            JsonNode isRenting = stationNode.path("is_renting");
            brstation.allowPickup = isRenting != null ? isRenting.asBoolean() : true;
            return brstation;
        }
    }

    class GbfsFloatingBikeDataSource extends GenericJsonBikeStationDataSource {

        public GbfsFloatingBikeDataSource() {
            super("data/bikes");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("bike_id").asText();
            brstation.name = new NonLocalizedString(stationNode.path("name").asText());
            brstation.x = stationNode.path("lon").asDouble();
            brstation.y = stationNode.path("lat").asDouble();
            brstation.bikesAvailable = 1;
            brstation.spacesAvailable = 0;
            brstation.allowDropoff = false;
            brstation.isFloatingBike = true;
            brstation.isCarStation = routeAsCar;
            return brstation;
        }
    }
}
