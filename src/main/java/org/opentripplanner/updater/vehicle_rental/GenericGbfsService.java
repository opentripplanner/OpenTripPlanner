package org.opentripplanner.updater.vehicle_rental;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.opentripplanner.analyst.UnsupportedGeometryException;
import org.opentripplanner.api.resource.VehicleRental;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalRegion;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.FreeBikeStatus;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.GbfsRespone;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.StationInformation;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.StationStatus;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.*;

/**
 * A standalone service for consuming a GBFS
 */
public class GenericGbfsService implements VehicleRentalDataSource, JsonConfigurable {
    private static final Logger LOG = LoggerFactory.getLogger(GenericGbfsService.class);

    private ObjectMapper mapper = new ObjectMapper();

    // config items
    private String rootUrl;
    private String headerName;
    private String headerValue;
    private String language;
    private String networkName;
    // whether or not this system has docks. If the feed does not have the required URLs/files about docks, errors will
    // be logged as a result
    private boolean hasDocks;
    private String regionsUrl;

    // items used in updating
    private boolean vehiclesUpdated = false;
    private boolean regionsUpdated = false;
    private List<VehicleRentalStation> stations;
    private boolean regionsLoadedFromConfig;
    private List<VehicleRentalRegion> regions;

    public GenericGbfsService() {
        this(null, null, "en");
    }

    /**
     * @param headerName header name
     * @param headerValue header value
     */
    public GenericGbfsService(String headerName, String headerValue, String language) {
        this.headerName = headerName;
        this.headerValue = headerValue;
        this.language = language;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Note that the JSON being passed in here is for configuration of the OTP component, it's completely separate
     * from the JSON coming in from the update source.
     */
    @Override
    public void configure (Graph graph, JsonNode config) {
        String url = config.path("url").asText(); // path() returns MissingNode not null.
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' configuration.");
        }
        this.rootUrl = url;

        this.networkName = config.path("network").asText();
        if (networkName == null || networkName.isEmpty()) {
            LOG.warn(
                "Received an vehicle rental updater config without a network name. This is not advised if this OTP "
                    + "instance uses more than 1 vehicle rental updater. Root URL: {}",
                rootUrl
            );
            this.networkName = "GBFS";
        }
        this.hasDocks = config.path("hasDocks").asBoolean();
        setRegionsFromConfig(config);
    }

    /**
     * the following is ideally a bit of temporary code that reads in the contents from a URL (or file) that describes
     * the allowable dropoff areas for this vehicle rental company
     *
     * ideally in the future, the url will be built into the GBFS
     */
    private void setRegionsFromConfig(JsonNode config) {
        JsonNode regionsUrlNode = config.get("regionsUrl");
        if (regionsUrlNode != null) {
            regionsUrl = regionsUrlNode.asText();
        }
        JsonNode regionGeoJson = config.get("regionsGeoJson");
        if (regionGeoJson != null) {
            regions = parseRegionJson(regionGeoJson);
        } else if (regionsUrl != null) {
            try {
                // fetch regions
                InputStream data = fetchFromUrl(regionsUrl);

                if (data == null) {
                    LOG.warn("Failed to get data from url " + regionsUrl);
                    return;
                }

                // parse the region and update the regions
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(data);
                regions = parseRegionJson(rootNode);
                data.close();
            } catch (IOException e) {
                LOG.warn("Error reading vehicle rental regions from " + regionsUrl, e);
                return;
            }
        } else {
            LOG.warn("region GeoJson not found in configuration for Vehicle Rental Datasource." +
                "Dropoffs are assumed to be allowed in full extent of graph.");
            GeometryFactory geometryFactory = new GeometryFactory();
            VehicleRentalRegion entireEarthRegion = new VehicleRentalRegion();
            entireEarthRegion.network = networkName;
            entireEarthRegion.geometry = geometryFactory.toGeometry(new Envelope(-180, 180, -90, 90));
            regions = Arrays.asList(entireEarthRegion);
        }
        if (regions != null) {
            regionsLoadedFromConfig = true;
        }
    }

    @Override public boolean regionsUpdated() {
        // return a one-time update if the regions were loaded from config
        if (regionsLoadedFromConfig) {
            regionsLoadedFromConfig = false;
            return true;
        }
        return regionsUpdated;
    }

    @Override public boolean stationsUpdated() {
        return vehiclesUpdated;
    }

    @Override public List<VehicleRentalStation> getStations() {
        return stations;
    }

    @Override public List<VehicleRentalRegion> getRegions() {
        return regions;
    }

    @Override
    public void update () {
        // reset update statuses and data
        vehiclesUpdated = false;
        regionsUpdated = false;
        stations = new ArrayList<>();

        // fetch data from root URL
        InputStream rootData = fetchFromUrl(makeUrl("gbfs.json"));

        String systemInformationUrl = null;
        String stationInformationUrl = null;
        String stationStatusUrl = null;
        String freeBikeStatusUrl = null;
        String systemHoursUrl = null;
        String systemCalendarUrl = null;
        String systemRegionsUrl = null;
        String systemPricingPlansUrl = null;
        String systemAlertsUrl = null;

        if (rootData == null) {
            // populate URLs with default values
            systemInformationUrl = makeUrl("system_information.json");
            stationInformationUrl = makeUrl("station_information.json");
            stationStatusUrl = makeUrl("station_status.json");
            freeBikeStatusUrl = makeUrl("free_bike_status.json");
            systemHoursUrl = makeUrl("system_hours.json");
            systemCalendarUrl = makeUrl("system_calendar.json");
            systemRegionsUrl = makeUrl("system_regions.json");
            systemPricingPlansUrl = makeUrl("system_pricing_plans.json");
            systemAlertsUrl = makeUrl("system_alerts.json");
        } else {
            GbfsRespone gbfsRespone = null;
            try {
                gbfsRespone = mapper.readValue(rootData, GbfsRespone.class);
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
            if (gbfsRespone.data == null) {
                LOG.error("failed to read gbfs.json, no data found");
                return;
            }
            GbfsRespone.GbfsFeeds feeds = gbfsRespone.data.get(language);
            if (feeds == null) {
                LOG.error("requested language ({}) not available in GBFS: {}", language, rootUrl);
                return;
            }
            for (GbfsRespone.GbfsFeed feed : feeds.feeds) {
                switch (feed.name) {
                    case "system_information":
                        systemInformationUrl = feed.url;
                        break;
                    case "station_information":
                        stationInformationUrl = feed.url;
                        break;
                    case "station_status":
                        stationStatusUrl = feed.url;
                        break;
                    case "free_bike_status":
                        freeBikeStatusUrl = feed.url;
                        break;
                    case "system_hours":
                        systemHoursUrl = feed.url;
                        break;
                    case "system_calendar":
                        systemCalendarUrl = feed.url;
                        break;
                    case "system_regions":
                        systemRegionsUrl = feed.url;
                        break;
                    case "system_pricing_plans":
                        systemPricingPlansUrl = feed.url;
                        break;
                    case "system_alerts":
                        systemAlertsUrl = feed.url;
                        break;
                }
            }
        }

        // TODO: make the following methods asynchronous
        // get basic system information. Although this URL/file is technically required, don't fail fast if fetching
        // data from this service doesn't work for some reason.
        updateSystemInformation(systemInformationUrl);

        // get information related to docking stations
        updateDockedStationInformation(stationInformationUrl, stationStatusUrl);

        // get information related to free-floating vehicles
        updateFreeFloatingVehicles(freeBikeStatusUrl);

        // TODO add more processors for the other stuff

        // TODO have NABSA make rental regions a part of their spec somehow. See https://github.com/NABSA/gbfs/issues/65
        updateRegions();
    }

    /**
     * Construct a url based on the root url and the desired file
     */
    private String makeUrl(String file) {
        String baseUrl = rootUrl;
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        return String.format("%s%s", baseUrl, file);
    }

    /**
     * Update the system information URL
     * @param url
     */
    private void updateSystemInformation(String url) {
        if (url == null) {
            LOG.error("system_information URL is required, but none was found for feed: {}", networkName);
            return;
        }
        InputStream data = fetchFromUrl(url, true);
        if (data == null) {
            LOG.error("failed to fetch required data from system_information URL for feed: {}", networkName);
            return;
        }
        // TODO consume data regarding system start date
        try {
            data.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateDockedStationInformation(String stationInformationUrl, String stationStatusUrl) {
        // get the station information
        if (stationInformationUrl == null) {
            // this URL/file is not required unless the system uses docks. Only log a warning if the config specifically
            // notes that docking stations are expected in this feed.
            if (hasDocks) {
                LOG.error("Conditionally required station_information URL is not defined for feed: {}", networkName);
            }

            // There is no point in continuing without gps info on the station locations
            return;
        }
        StationInformation stationInfo = fetchAndParseFromUrl(
            stationInformationUrl,
            StationInformation.class,
            hasDocks
        );
        if (stationInfo == null) {
            // There is no point in continuing without gps info on the station locations
            return;
        }

        // organize stations by id
        Map<String, VehicleRentalStation> stationsByStationId = new HashMap<>();
        for (StationInformation.DockingStationInformation station : stationInfo.data.stations) {
            VehicleRentalStation vehicleRentalStation = parseDockingStation(station);
            if (vehicleRentalStation != null) {
                stationsByStationId.put(vehicleRentalStation.id, vehicleRentalStation);
            }
        }

        // get status statuses
        if (stationStatusUrl == null) {
            LOG.error("Station information found, but station status URL is not defined for feed: {}", networkName);
            return;
        }
        StationStatus stationStatus = fetchAndParseFromUrl(stationStatusUrl, StationStatus.class, hasDocks);
        if (stationStatus == null) {
            if (hasDocks) {
                // this file is required, so something went wrong.
                LOG.error(
                    "Failed to fetch and/or parse conditionally required data from station information for feed: {}",
                    networkName
                );
            }
            // Don't process null station status info
            return;
        }
        // update each vehicle rental station found in station information URL/file.
        for (StationStatus.DockingStationStatusInformation station : stationStatus.data.stations) {
            VehicleRentalStation vehicleRentalStation = stationsByStationId.get(station.station_id);
            if (vehicleRentalStation == null) {
                LOG.error(
                    "Station with id: {} not found in station information data for feed: {}",
                    station.station_id,
                    networkName
                );
                continue;
            }
            vehicleRentalStation.spacesAvailable = station.num_docks_available;
            vehicleRentalStation.vehiclesAvailable = station.num_bikes_available;
            if (station.num_bikes_available == null) {
                LOG.error(
                    "Station with id: {} missing required information on number of vehicles available within feed: {}",
                    station.station_id,
                    networkName
                );
                continue;
            }
            if (station.num_docks_available == null) {
                LOG.error(
                    "Station with id: {} missing required information on number of docks available within feed: {}",
                    station.station_id,
                    networkName
                );
                continue;
            }
            // assume pickups and dropoffs are allowed if installed if optional data is not provided
            vehicleRentalStation.allowPickup = station.num_bikes_available > 0 &&
                (station.is_installed == null || station.is_installed == 1) &&
                (station.is_renting == null || station.is_renting == 1);
            vehicleRentalStation.allowDropoff = station.num_docks_available > 0 &&
                (station.is_installed == null || station.is_installed == 1) &&
                (station.is_returning == null || station.is_returning == 1);
        }
        stations.addAll(stationsByStationId.values());
        vehiclesUpdated = true;
    }

    private void updateFreeFloatingVehicles(String freeBikeStatusUrl) {
        FreeBikeStatus floatingBikes = fetchAndParseFromUrl(freeBikeStatusUrl, FreeBikeStatus.class);
        if (floatingBikes == null) {
            return;
        }
        for (FreeBikeStatus.FreeBike bike : floatingBikes.data.bikes) {
            if (!bike.is_disabled && !bike.is_reserved) {
                VehicleRentalStation floatingVehicle = new VehicleRentalStation();
                floatingVehicle.id = bike.bike_id;
                floatingVehicle.name = new NonLocalizedString(bike.bike_id);
                floatingVehicle.x = bike.lon;
                floatingVehicle.y = bike.lat;

                floatingVehicle.allowDropoff = false;
                floatingVehicle.allowPickup = true;
                floatingVehicle.isFloatingVehicle = true;
                floatingVehicle.networks = Sets.newHashSet(networkName);
                floatingVehicle.spacesAvailable = 0;
                floatingVehicle.vehiclesAvailable = 1;

                stations.add(floatingVehicle);
            }
        }
    }

    private void updateRegions() {
        // TODO implement
    }

    /**
     * Attempt to parse geojson and set that to be the region.  Currently supported types include
     * either a single Feature or a FeatureCollection.
     */
    private List<VehicleRentalRegion> parseRegionJson(JsonNode regionJson) {
        ObjectMapper jsonDeserializer = new ObjectMapper();
        final VehicleRentalRegion region = new VehicleRentalRegion();
        region.network = networkName;

        // first try to deserialize as a feature
        try {
            Feature geoJsonFeature = jsonDeserializer.readValue(
                regionJson.traverse(),
                Feature.class
            );
            GeoJsonObject geometry = geoJsonFeature.getGeometry();
            region.geometry = GeometryUtils.convertGeoJsonToJtsGeometry(geometry);
        } catch (IllegalArgumentException | IOException | UnsupportedGeometryException e) {
            LOG.debug("Could not parse as a Feature, trying as a FeatureCollection");
            try {
                List<Geometry> geometries = new ArrayList<>();
                FeatureCollection geoJsonFeatureCollection = jsonDeserializer.readValue(
                    regionJson.traverse(),
                    FeatureCollection.class
                );

                // convert all features to geometry
                for (Feature feature : geoJsonFeatureCollection.getFeatures()) {
                    geometries.add(GeometryUtils.convertGeoJsonToJtsGeometry(feature.getGeometry()));
                }

                // union all geometries into a single geometry
                GeometryFactory geometryFactory = new GeometryFactory();
                GeometryCollection geometryCollection =
                    (GeometryCollection) geometryFactory.buildGeometry(geometries);
                region.geometry = geometryCollection.union();
            } catch (IllegalArgumentException | IOException | UnsupportedGeometryException e1) {
                e1.printStackTrace();
                LOG.error("Could not deserialize GeoJSON for {}", networkName);
                return new ArrayList<>();
            }
        }
        return Arrays.asList(region);
    }

    /**
     * Translate info from the GBFS into OTP's vehicle rental station data structure.
     */
    private VehicleRentalStation parseDockingStation(StationInformation.DockingStationInformation station) {
        VehicleRentalStation vehicleRentalStation = new VehicleRentalStation();
        vehicleRentalStation.id = station.station_id;
        // TODO localize. We should know the locale from the feed info.
        vehicleRentalStation.name = new NonLocalizedString(station.name);
        vehicleRentalStation.x = station.lon;
        vehicleRentalStation.y = station.lat;

        vehicleRentalStation.isBorderDropoff = false;
        vehicleRentalStation.isFloatingVehicle = false;
        vehicleRentalStation.networks = Sets.newHashSet(networkName);

        return vehicleRentalStation;
    }

    /**
     * Helper method to fetch and parse from a non-required URL/file.
     */
    private <T> T fetchAndParseFromUrl(String url, Class<T> clazz) {
        return fetchAndParseFromUrl(url, clazz, false);
    }

    /**
     * Helper method to fetch from a URL/file and then deserialize into the given type.
     *
     * @param url The URL/file to fetch from.
     * @param clazz The class type to serialize into
     * @param required Whether or not data should be required from the URL/file.
     * @return An instance of type clazz or null if some error were encountered.
     */
    private <T> T fetchAndParseFromUrl(
        String url,
        Class<T> clazz,
        boolean required
    ) {
        InputStream data = fetchFromUrl(url, required);
        if (data == null) {
            return null;
        }
        try {
            return mapper.readValue(data, clazz);
        } catch (IOException e) {
            LOG.error("Failed to parse data fetched from feed: {} at URL: {}", networkName, url);
            e.printStackTrace();
        } finally {
            try {
                data.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
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
        	URL url2 = new URL(url);

            String proto = url2.getProtocol();
            if (proto.equals("http") || proto.equals("https")) {
            	data = HttpUtils.getData(url, headerName, headerValue);
            } else {
                // Local file probably, try standard java
                data = url2.openStream();
            }
        } catch (SocketTimeoutException e) {
            LOG.warn("timeout encountered while trying to fetch from URL: {}", url);
        } catch (IOException e) {
            LOG.warn("Error reading data from " + url, e);
        }
        if (data == null && fetchRequired) {
            LOG.error("Received no data from URL fetch from: {}", url);
        }
        return data;
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + networkName + " " + rootUrl + ")";
    }
}
