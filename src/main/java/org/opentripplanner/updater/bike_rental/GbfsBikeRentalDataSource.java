package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Created by demory on 2017-03-14.
 */
public class GbfsBikeRentalDataSource implements BikeRentalDataSource, JsonConfigurable {

    private static final Logger LOG = LoggerFactory.getLogger(GbfsBikeRentalDataSource.class);

    private GbfsStationDataSource stationInformationSource;  // station_information.json required by GBFS spec
    private GbfsStationStatusDataSource stationStatusSource; // station_status.json required by GBFS spec
    private GbfsFloatingBikeDataSource floatingBikeSource;   // free_bike_status.json declared OPTIONAL by GBFS spec
    private GbfsSystemHoursDataSource systemHoursDataSource;

    private String baseUrl;

    private String networkName;

    /** Some car rental systems and flex transit systems work exactly like bike rental, but with cars. */
    private boolean routeAsCar;

    public GbfsBikeRentalDataSource (String networkName) {
        stationInformationSource = new GbfsStationDataSource();
        stationStatusSource = new GbfsStationStatusDataSource();
        floatingBikeSource = new GbfsFloatingBikeDataSource();
        systemHoursDataSource = new GbfsSystemHoursDataSource();

        if (networkName != null && !networkName.isEmpty()) {
            this.networkName = networkName;
        } else {
            this.networkName = "GBFS";
        }
    }

    public void setBaseUrl (String url) {
        baseUrl = url;
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        stationInformationSource.setUrl(baseUrl + "station_information.json");
        stationStatusSource.setUrl(baseUrl + "station_status.json");
        floatingBikeSource.setUrl(baseUrl + "free_bike_status.json");
        systemHoursDataSource.setUrl(baseUrl + "system_hours.json");
    }

    @Override
    public boolean update() {
        // These first two GBFS files are required.
        boolean updatesFound = stationInformationSource.update();
        updatesFound |= stationStatusSource.update();
        // This floating-bikes file is optional, and does not appear in all GBFS feeds.
        updatesFound |= floatingBikeSource.update();
        // This system hours file is optional, and does not appear in all GBFS feeds.
        updatesFound |= systemHoursDataSource.update();
        // FIXME we are repeatedly polling all these URLs even if they're not declared to be present in gbfs.json
        // Return true if ANY of the sub-updaters found any updates.
        return updatesFound;
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

        // Set identical opening hours on all stations within this system
        for (BikeRentalStation station : stations) station.rentalHoursList = systemHoursDataSource.rentalHoursList;

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
        public BikeRentalStation makeStation(JsonNode stationNode) {
            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("station_id").toString();
            brstation.x = stationNode.path("lon").asDouble();
            brstation.y = stationNode.path("lat").asDouble();
            brstation.name =  new NonLocalizedString(stationNode.path("name").asText());
            brstation.isCarStation = routeAsCar;
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
            brstation.isCarStation = routeAsCar;
            return brstation;
        }
    }

    class GbfsFloatingBikeDataSource extends GenericJsonBikeRentalDataSource {

        public GbfsFloatingBikeDataSource () {
            super("data/bikes");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("bike_id").toString();
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

    /**
     * This is ugly because unlike the others, it does not return a list of rental station information.
     * It assembles some global opening hours information about all the stations in the system.
     * For the time being I'm making this extend GenericJsonBikeRentalDataSource so it reuses
     * the HTTP/local file fetching code, JSON decoding, etc. but those methods are closely coupled to the
     * decoding of individual station representations. Those methods should eventually be pulled out and
     * made into more reuseable static methods that are not so closely tied to the decoding of stations.
     *
     * This updater is also a little strange in that instead of returning a list of new objects, it
     * updates its own internal field that contains the operating hours, and provides methods to check
     * the contents of that list.
     *
     * Another way to implement this would be to make 2 abstract classes: `GenericJsonBikeRentalDataSource` with an
     * abstract `processNodes()` method and `IndividualStationJsonBikeRentalDataSource`.
     */
    class GbfsSystemHoursDataSource extends GenericJsonBikeRentalDataSource {

        List<BikeRentalHours> rentalHoursList = null;

        public GbfsSystemHoursDataSource() {
            super("data/rental_hours");
        }

        @Override
        public void processNodes(JsonNode rootNode) {
            List<BikeRentalHours> newRentalHoursList = new ArrayList<>();
            for (JsonNode node : rootNode) {
                newRentalHoursList.add(BikeRentalHours.fromJsonNode(node));
            }
            // Atomic replace of the full list of hours, so as not to confuse other instance methods.
            synchronized (this) {
                rentalHoursList = newRentalHoursList;
            }
        }

        @Override
        public BikeRentalStation makeStation(JsonNode rentalStationNode) {
            // This method should never be called in this class's implementation of processNodes.
            throw new UnsupportedOperationException();
        }

    }
}
