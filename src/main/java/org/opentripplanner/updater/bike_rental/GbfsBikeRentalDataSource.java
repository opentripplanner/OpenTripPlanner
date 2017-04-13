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

/**
 * Created by demory on 3/14/17.
 */
public class GbfsBikeRentalDataSource implements BikeRentalDataSource, JsonConfigurable {

    private static final Logger log = LoggerFactory.getLogger(GbfsBikeRentalDataSource.class);

    private GbfsStationDataSource stationSource;
    private GbfsStationStatusDataSource stationStatusSource;

    private String baseUrl;
    private String apiKey;

    public GbfsBikeRentalDataSource () {
        stationSource = new GbfsStationDataSource();
        stationStatusSource = new GbfsStationStatusDataSource();
    }

    //private boolean read


    public void setBaseUrl (String url) {
        baseUrl = url;
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        stationSource.setUrl(baseUrl + "station_information.json");
        stationStatusSource.setUrl(baseUrl + "station_status.json");
    }

    @Override
    public boolean update() {
        return stationSource.update() && stationStatusSource.update();
    }

    @Override
    public List<BikeRentalStation> getStations() {
        //List<BikeRentalStation> = new LinkedList<>();
        Map<String, BikeRentalStation> statusLookup = new HashMap<>();

        for (BikeRentalStation station : stationStatusSource.getStations()) {
            statusLookup.put(station.id, station);
        }

        for (BikeRentalStation station : stationSource.getStations()) {
            if (!statusLookup.containsKey(station.id)) continue;
            BikeRentalStation status = statusLookup.get(station.id);
            station.bikesAvailable = status.bikesAvailable;
            station.spacesAvailable = status.spacesAvailable;
        }

        return stationSource.getStations();
    }

    /**
     * Note that the JSON being passed in here is for configuration of the OTP component, it's completely separate
     * from the JSON coming in from the update source.
     */
    @Override
    public void configure (Graph graph, JsonNode jsonNode) {
        String url = jsonNode.path("url").asText(); // path() returns MissingNode not null.
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' configuration.");
        }
        this.setBaseUrl(url);
    }

    class GbfsStationDataSource extends GenericJsonBikeRentalDataSource {

        public GbfsStationDataSource () {
            super("data/stations");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            BikeRentalStation brstation = new BikeRentalStation();

            brstation.id = stationNode.path("station_id").toString();
            brstation.x = stationNode.path("lon").asDouble();
            brstation.y = stationNode.path("lat").asDouble();
            brstation.name =  new NonLocalizedString(stationNode.path("name").asText());

            return brstation;
        }
    }

    class GbfsStationStatusDataSource extends GenericJsonBikeRentalDataSource {

        public GbfsStationStatusDataSource () {
            super("data/stations");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            BikeRentalStation brstation = new BikeRentalStation();

            brstation.id = stationNode.path("station_id").toString();
            brstation.bikesAvailable = stationNode.path("num_bikes_available").asInt();
            brstation.spacesAvailable = stationNode.path("num_docks_available").asInt();

            return brstation;
        }
    }
}
