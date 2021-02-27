package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.RentalStation;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.GbfsResponse;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.opentripplanner.util.HttpUtils.getDataFromUrlOrFile;

/**
 * Created by demory on 2017-03-14.
 */
public class GbfsBikeRentalDataSource implements BikeRentalDataSource, JsonConfigurable {

    private static final Logger LOG = LoggerFactory.getLogger(GbfsBikeRentalDataSource.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private GbfsStationDataSource stationInformationSource;  // station_information.json required by GBFS spec
    private GbfsStationStatusDataSource stationStatusSource; // station_status.json required by GBFS spec
    private GbfsFloatingBikeDataSource floatingBikeSource;   // free_bike_status.json declared OPTIONAL by GBFS spec

    private String baseUrl;
    private String networkName;
    // The language id to use in the GBFS.json. Default is "en".
    private String language = "en";

    /** Some car rental systems and flex transit systems work exactly like bike rental, but with cars. */
    private boolean routeAsCar;

    public GbfsBikeRentalDataSource (String networkName) {
        stationInformationSource = new GbfsStationDataSource();
        stationStatusSource = new GbfsStationStatusDataSource();
        floatingBikeSource = new GbfsFloatingBikeDataSource();
        if (networkName != null && !networkName.isEmpty()) {
            this.networkName = networkName;
        } else {
            this.networkName = "GBFS";
        }
    }

    public void setBaseUrl (String url) {
        baseUrl = url;
        if (!baseUrl.endsWith("/")) baseUrl += "/";
    }

    @Override
    public boolean update() {
        updateUrls();
        // These first two GBFS files are required.
        boolean updatesFound = stationInformationSource.update();
        updatesFound |= stationStatusSource.update();
        // This floating-bikes file is optional, and does not appear in all GBFS feeds.
        updatesFound |= floatingBikeSource.update();
        // Return true if ANY of the sub-updaters found any updates.
        return updatesFound;
    }

    /**
     * Reads the GBFS.json url (if it exists) and sets the urls of the other sources
     */
    private void updateUrls() {
        // fetch data from root URL. This file/endpoint is actually not required per the GBFS spec
        // See https://github.com/NABSA/gbfs/blob/master/gbfs.md#files
        InputStream rootData = fetchFromUrl(makeGbfsEndpointUrl("gbfs.json"));

        // Check to see if data from the root url was able to be fetched. The gbfs.json file is not required.
        if (rootData == null) {
            // root data is null, however some feeds don't specifically have a gbfs.json endpoint and just use the root
            // url itself. Therefore try again with the root url to see if that works.
            rootData = fetchFromUrl(makeGbfsEndpointUrl(""));
        }

        if (rootData == null) {
            // Root GBFS.json file not able to be fetched, set default endpoints.
            stationInformationSource.setUrl(makeGbfsEndpointUrl("station_information.json"));
            stationStatusSource.setUrl(makeGbfsEndpointUrl("station_status.json"));
            floatingBikeSource.setUrl(makeGbfsEndpointUrl("free_bike_status.json"));
        } else {
            // GBFS.json file is found. Parse data from response and set all of the corresponding URLs as they are
            // available in the response data.
            GbfsResponse gbfsResponse = null;
            try {
                gbfsResponse = mapper.readValue(rootData, GbfsResponse.class);
            } catch (IOException e) {
                LOG.error("failed to deserialize gbfs.json response: {}", e);
                return;
            } finally {
                try {
                    rootData.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (gbfsResponse.data == null) {
                LOG.error("failed to read gbfs.json, no data found");
                return;
            }

            // Get the GBFS feeds for the configured language.
            GbfsResponse.GbfsFeeds feeds = gbfsResponse.data.get(language);
            if (feeds == null) {
                LOG.error("requested language ({}) not available in GBFS: {}", language, baseUrl);
                return;
            }

            // iterate through all feed endpoints and update as needed
            for (GbfsResponse.GbfsFeed feed : feeds.feeds) {
                switch (feed.name) {
                case "system_information":
                    // FIXME: not supported yet
                    break;
                case "station_information":
                    stationInformationSource.setUrl(feed.url);
                    break;
                case "station_status":
                    stationStatusSource.setUrl(feed.url);
                    break;
                case "free_bike_status":
                    floatingBikeSource.setUrl(feed.url);
                    break;
                case "system_hours":
                    // FIXME: not supported yet
                    break;
                case "system_calendar":
                    // FIXME: not supported yet
                    break;
                case "system_regions":
                    // FIXME: not supported yet
                    break;
                case "system_pricing_plans":
                    // FIXME: not supported yet
                    break;
                case "system_alerts":
                    // FIXME: not supported yet
                    break;
                }
            }
        }
    }

    /**
     * Construct a url based on the root url and the desired file
     */
    private String makeGbfsEndpointUrl(String file) {
        return String.format("%s%s", baseUrl, file);
    }

    /**
     * Helper method to fetch from a URL where a response is not required.
     */
    private InputStream fetchFromUrl(String url) {
        return fetchFromUrl(url, false);
    }

    /**
     * Helper to fetch data from a URL, or file or something.
     *
     * @param url The URL or file or something to fetch from
     * @param fetchRequired whether or not a failed fetch of any data should warrant logging an error
     * @return An inputStream if successful or null if not successful
     */
    private InputStream fetchFromUrl(String url, boolean fetchRequired) {
        InputStream data = null;
        try {
            data = getDataFromUrlOrFile(url);
        } catch (IOException e) {
            LOG.warn("Failed to fetch from url: {}. Error: {}", url, e);
        }
        if (data == null && fetchRequired) {
            LOG.error("Received no data from URL fetch from: {}", url);
        }
        return data;
    }

    @Override
    public List<BikeRentalStation> getStations() {

        // Index all the station status entries on their station ID.
        Map<String, BikeRentalStation> statusLookup = new HashMap<>();
        for (BikeRentalStation station : stationStatusSource.getStations()) {
            statusLookup.put(station.id, station);
        }

        // Iterate over all known stations, and if we have any status information add it to those station objects.
        for (BikeRentalStation station : stationInformationSource.getStations()) {
            if (!statusLookup.containsKey(station.id)) continue;
            BikeRentalStation status = statusLookup.get(station.id);
            station.bikesAvailable = status.bikesAvailable;
            station.spacesAvailable = status.spacesAvailable;
        }

        // Copy the full list of station objects (with status updates) into a List, appending the floating bike stations.
        List<BikeRentalStation> stations = new LinkedList<>(stationInformationSource.getStations());
        stations.addAll(floatingBikeSource.getStations());

        // Set identical network ID on all stations
        Set<String> networkIdSet = Sets.newHashSet(this.networkName);
        for (BikeRentalStation station : stations) station.networks = networkIdSet;

        return stations;
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
        String language = jsonNode.path("language").asText();
        if (language != null && language != "") {
            this.language = language;
        }
        this.routeAsCar = jsonNode.path("routeAsCar").asBoolean(false);
        if (routeAsCar) {
            LOG.info("This 'bike rental' system will be treated as a car rental system.");
        }
    }

    class GbfsStationDataSource extends GenericJsonBikeRentalDataSource {

        public GbfsStationDataSource () {
            super("data/stations");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode, Integer feedUpdateEpochSeconds) {
            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("station_id").toString();
            brstation.x = stationNode.path("lon").asDouble();
            brstation.y = stationNode.path("lat").asDouble();
            brstation.name =  new NonLocalizedString(stationNode.path("name").asText());
            brstation.lastReportedEpochSeconds = RentalStation.getLastReportedTimeUsingFallbacks(
                stationNode.path("last_reported").asLong(),
                feedUpdateEpochSeconds
            );
            brstation.isCarStation = routeAsCar;
            return brstation;
        }
    }

    class GbfsStationStatusDataSource extends GenericJsonBikeRentalDataSource {

        public GbfsStationStatusDataSource () {
            super("data/stations");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode, Integer feedUpdateEpochSeconds) {
            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("station_id").toString();
            brstation.bikesAvailable = stationNode.path("num_bikes_available").asInt();
            brstation.spacesAvailable = stationNode.path("num_docks_available").asInt();
            brstation.lastReportedEpochSeconds = RentalStation.getLastReportedTimeUsingFallbacks(
                stationNode.path("last_reported").asLong(),
                feedUpdateEpochSeconds
            );
            brstation.isCarStation = routeAsCar;
            return brstation;
        }
    }

    class GbfsFloatingBikeDataSource extends GenericJsonBikeRentalDataSource {

        public GbfsFloatingBikeDataSource () {
            super("data/bikes");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode, Integer feedUpdateEpochSeconds) {
            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("bike_id").toString();
            brstation.name = new NonLocalizedString(stationNode.path("name").asText());
            brstation.x = stationNode.path("lon").asDouble();
            brstation.y = stationNode.path("lat").asDouble();
            brstation.lastReportedEpochSeconds = RentalStation.getLastReportedTimeUsingFallbacks(
                stationNode.path("last_reported").asLong(),
                feedUpdateEpochSeconds
            );
            brstation.bikesAvailable = 1;
            brstation.spacesAvailable = 0;
            brstation.allowDropoff = false;
            brstation.isFloatingBike = true;
            brstation.isCarStation = routeAsCar;
            return brstation;
        }
    }
}
