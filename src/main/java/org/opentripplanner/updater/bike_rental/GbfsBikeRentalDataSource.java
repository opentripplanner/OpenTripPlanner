package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;

/**
 * Created by demory on 3/14/17.
 */
public class GbfsBikeRentalDataSource implements BikeRentalDataSource, JsonConfigurable {

    private static final Logger log = LoggerFactory.getLogger(GbfsBikeRentalDataSource.class);

    private GbfsStationDataSource stationSource;

    private String baseUrl;
    private String apiKey;

    public GbfsBikeRentalDataSource () {

        stationSource = new GbfsStationDataSource();
    }

    //private boolean read


    public void setBaseUrl (String url) {
        baseUrl = url;
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        stationSource.setUrl(baseUrl + "station_information.json");
    }

    @Override
    public boolean update() {
        return stationSource.update();
    }

    @Override
    public List<BikeRentalStation> getStations() {
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
}
