package org.opentripplanner.updater.vehicle_rental.datasources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationUris;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by demory on 2017-03-14.
 *
 * Leaving OTPFeature.FloatingBike turned off both prevents floating bike updaters added to
 * router-config.json from being used, but more importantly, floating bikes added by a
 * VehicleRentalServiceDirectoryFetcher endpoint (which may be outside our control) will not be used.
 */
class GbfsVehicleRentalDataSource implements VehicleRentalDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GbfsVehicleRentalDataSource.class);

    private static final String DEFAULT_NETWORK_NAME = "GBFS";

    // station_information.json required by GBFS spec
    private final GbfsStationDataSource stationInformationSource;

    // station_status.json required by GBFS spec
    private final GbfsStationStatusDataSource stationStatusSource;

    // free_bike_status.json declared OPTIONAL by GBFS spec
    private final GbfsFloatingVehicleDataSource floatingBikeSource;

    private final String networkName;

    /** Some car rental systems and flex transit systems work exactly like bike rental, but with cars. */
    private final boolean routeAsCar;

    public GbfsVehicleRentalDataSource(GbfsVehicleRentalDataSourceParameters parameters) {
        routeAsCar = parameters.routeAsCar();
        stationInformationSource = new GbfsStationDataSource(parameters);
        stationStatusSource = new GbfsStationStatusDataSource(parameters);
        floatingBikeSource = OTPFeature.FloatingBike.isOn()
            ? new GbfsFloatingVehicleDataSource(parameters)
            : null;

        configureUrls(parameters.getUrl(), parameters.getHttpHeaders());
        this.networkName = parameters.getNetwork(DEFAULT_NETWORK_NAME);
    }

    private void configureUrls(String url, Map<String, String> headers) {
        GbfsAutoDiscoveryDataSource gbfsAutoDiscoveryDataSource = new GbfsAutoDiscoveryDataSource(url, headers);
        stationInformationSource.setUrl(gbfsAutoDiscoveryDataSource.stationInformationUrl);
        stationStatusSource.setUrl(gbfsAutoDiscoveryDataSource.stationStatusUrl);
        if (OTPFeature.FloatingBike.isOn()) {
            floatingBikeSource.setUrl(gbfsAutoDiscoveryDataSource.freeBikeStatusUrl);
        }
    }

    @Override
    public boolean update() {
        // These first two GBFS files are required.
        boolean updatesFound = stationInformationSource.update();
        updatesFound |= stationStatusSource.update();
        // This floating-bikes file is optional, and does not appear in all GBFS feeds.
        if (OTPFeature.FloatingBike.isOn()) {
            updatesFound |= floatingBikeSource.update();
        }
        // Return true if ANY of the sub-updaters found any updates.
        return updatesFound;
    }

    @Override
    public List<VehicleRentalStation> getStations() {

        // Index all the station status entries on their station ID.
        Map<String, VehicleRentalStation> statusLookup = new HashMap<>();
        for (VehicleRentalStation station : stationStatusSource.getStations()) {
            statusLookup.put(station.id, station);
        }

        // Iterate over all known stations, and if we have any status information add it to those station objects.
        for (VehicleRentalStation station : stationInformationSource.getStations()) {
            if (!statusLookup.containsKey(station.id)) continue;
            VehicleRentalStation status = statusLookup.get(station.id);
            station.vehiclesAvailable = status.vehiclesAvailable;
            station.spacesAvailable = status.spacesAvailable;
        }

        // Copy the full list of station objects (with status updates) into a List, appending the floating bike stations.
        List<VehicleRentalStation> stations = new LinkedList<>(stationInformationSource.getStations());
        if (OTPFeature.FloatingBike.isOn()) {
            stations.addAll(floatingBikeSource.getStations());
        }

        // Set identical network ID on all stations
        Set<String> networkIdSet = Sets.newHashSet(this.networkName);
        for (VehicleRentalStation station : stations) station.networks = networkIdSet;

        return stations;
    }

    private static class GbfsAutoDiscoveryDataSource {
        private String stationInformationUrl;
        private String stationStatusUrl;
        private String freeBikeStatusUrl;

        public GbfsAutoDiscoveryDataSource(String autoDiscoveryUrl, Map<String, String> headers) {

            try {
                InputStream is = HttpUtils.getData(autoDiscoveryUrl, headers);
                JsonNode node = (new ObjectMapper()).readTree(is);
                JsonNode languages = node.get("data");

                for (JsonNode language : languages) {

                    JsonNode feeds = language.get("feeds");

                    for (JsonNode feed : feeds) {
                        String url = feed.get("url").asText();
                        switch (feed.get("name").asText()) {
                            case "station_information":
                                stationInformationUrl = url;
                                break;
                            case "station_status":
                                stationStatusUrl = url;
                                break;
                            case "free_bike_status":
                                freeBikeStatusUrl = url;
                                break;
                        }
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                LOG.warn("Error reading auto discovery file at {}. Using default values.",
                    autoDiscoveryUrl, e);
                // If the GBFS auto-discovery file (gbfs.json) can't be downloaded, fall back to the
                // v1 logic of finding the files under the given base path.
                var baseUrl = getBaseUrl(autoDiscoveryUrl);
                stationInformationUrl = baseUrl + "station_information.json";
                stationStatusUrl = baseUrl + "station_status.json";
                freeBikeStatusUrl = baseUrl + "free_bike_status.json";
            }
        }

        private String getBaseUrl(String url) {
            String baseUrl = url;
            if (baseUrl.endsWith("gbfs.json")) {
                baseUrl = baseUrl.substring(0, url.length() - "gbfs.json".length());
            }
            if (baseUrl.endsWith("gbfs")) {
                baseUrl = baseUrl.substring(0, url.length() - "gbfs".length());
            }
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            return baseUrl;
        }
    }

    class GbfsStationDataSource extends GenericJsonVehicleRentalDataSource<GbfsVehicleRentalDataSourceParameters> {

        public GbfsStationDataSource (GbfsVehicleRentalDataSourceParameters config) {
            super(config, "data/stations");
        }

        @Override
        public VehicleRentalStation makeStation(JsonNode stationNode) {
            VehicleRentalStation brstation = new VehicleRentalStation();
            brstation.id = stationNode.path("station_id").asText();
            brstation.longitude = stationNode.path("lon").asDouble();
            brstation.latitude = stationNode.path("lat").asDouble();
            brstation.name =  new NonLocalizedString(stationNode.path("name").asText());
            brstation.isKeepingVehicleRentalAtDestinationAllowed = config.allowKeepingRentedVehicleAtDestination();
            brstation.isCarStation = routeAsCar;

            if (stationNode.has("rental_uris")) {
                var rentalUrisObject = stationNode.path("rental_uris");
                String androidUri = rentalUrisObject.has("android") ? rentalUrisObject.get("android").asText() : null;
                String iosUri = rentalUrisObject.has("ios") ? rentalUrisObject.get("ios").asText() : null;
                String webUri = rentalUrisObject.has("web") ? rentalUrisObject.get("web").asText() : null;
                brstation.rentalUris = new VehicleRentalStationUris(androidUri, iosUri, webUri);
            }

            return brstation;
        }
    }

    class GbfsStationStatusDataSource extends GenericJsonVehicleRentalDataSource<GbfsVehicleRentalDataSourceParameters> {

        public GbfsStationStatusDataSource (GbfsVehicleRentalDataSourceParameters config) {
            super(config, "data/stations");
        }

        @Override
        public VehicleRentalStation makeStation(JsonNode stationNode) {
            VehicleRentalStation brstation = new VehicleRentalStation();
            brstation.id = stationNode.path("station_id").asText();
            brstation.vehiclesAvailable = stationNode.path("num_bikes_available").asInt();
            brstation.spacesAvailable = stationNode.path("num_docks_available").asInt();
            brstation.isKeepingVehicleRentalAtDestinationAllowed = config.allowKeepingRentedVehicleAtDestination();
            brstation.isCarStation = routeAsCar;
            return brstation;
        }
    }

    // TODO This is not currently safe to use. See javadoc on GbfsVehicleRentalDataSource class.
    class GbfsFloatingVehicleDataSource extends GenericJsonVehicleRentalDataSource<GbfsVehicleRentalDataSourceParameters> {

        public GbfsFloatingVehicleDataSource(GbfsVehicleRentalDataSourceParameters config) {
            super(config, "data/bikes");
        }

        @Override
        public VehicleRentalStation makeStation(JsonNode stationNode) {
            if (stationNode.path("station_id").asText().isBlank() &&
                    stationNode.has("lon") &&
                    stationNode.has("lat")
            ) {
                VehicleRentalStation brstation = new VehicleRentalStation();
                brstation.id = stationNode.path("bike_id").asText();
                brstation.name = new NonLocalizedString(stationNode.path("name").asText());
                brstation.longitude = stationNode.path("lon").asDouble();
                brstation.latitude = stationNode.path("lat").asDouble();
                brstation.vehiclesAvailable = 1;
                brstation.spacesAvailable = 0;
                brstation.allowDropoff = false;
                brstation.isFloatingBike = true;
                brstation.isCarStation = routeAsCar;
                return brstation;
            } else {
                return null;
            }
        }
    }

}
