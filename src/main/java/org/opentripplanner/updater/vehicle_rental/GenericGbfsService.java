package org.opentripplanner.updater.vehicle_rental;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.analyst.UnsupportedGeometryException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.RentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalRegion;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.FreeBikeStatus;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.GbfsResponse;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.StationInformation;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.StationStatus;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.SystemInformation;
import org.opentripplanner.util.DateUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.opentripplanner.util.GeoJsonUtils.parsePolygonOrMultiPolygonFromJsonNode;
import static org.opentripplanner.util.HttpUtils.getDataFromUrlOrFile;

/**
 * A standalone service for consuming a GBFS
 */
public class GenericGbfsService implements VehicleRentalDataSource, JsonConfigurable {
    private static final Logger LOG = LoggerFactory.getLogger(GenericGbfsService.class);

    private static final ObjectMapper mapper = new ObjectMapper();

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
    private List<RentalUpdaterError> errors;
    private SystemInformation.SystemInformationData systemInformationData;
    private Date systemStartDate;

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
     * The following is ideally a bit of temporary code that reads in the contents from a URL (or file) that describes
     * the allowable dropoff areas for this vehicle rental company
     *
     * TODO: It is quite likely that in the near future, there will be a way to fetch information about vehicle rental
     *  regions from an updated version of the GBFS. Once that happens, this will probably need to be refactored.
     */
    private void setRegionsFromConfig(JsonNode config) {
        JsonNode regionsUrlNode = config.get("regionsUrl");
        if (regionsUrlNode != null) {
            regionsUrl = regionsUrlNode.asText();
        }
        JsonNode regionsGeoJson = config.get("regionsGeoJson");
        if (regionsGeoJson != null) {
            regions = parseRegionJson(regionsGeoJson);
        } else if (regionsUrl != null) {
            try {
                // fetch regions
                InputStream data = fetchFromUrl(regionsUrl);

                if (data == null) {
                    LOG.warn("Failed to get data from url " + regionsUrl);
                    return;
                }

                // parse the region and update the regions
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

    @Override public List<RentalUpdaterError> getErrors() {
        return errors;
    }

    @Override public List<VehicleRentalStation> getStations() {
        return stations;
    }

    @Override public List<VehicleRentalRegion> getRegions() {
        return regions;
    }

    @Override public boolean regionsUpdated() {
        // return a one-time update if the regions were loaded from config
        if (regionsLoadedFromConfig) {
            regionsLoadedFromConfig = false;
            return true;
        }
        return regionsUpdated;
    }

    @Override public SystemInformation.SystemInformationData getSystemInformation() {
        return systemInformationData;
    }

    /**
     * Helper method for adding an error with a template String and associated values
     */
    private void addError(RentalUpdaterError.Severity severity, String template, Object... values) {
        addError(severity, String.format(template, values));
    }

    /**
     * Adds an error message to the list of errors and also logs the error message.
     */
    private void addError(RentalUpdaterError.Severity severity, String message) {
        message = String.format("%s (feed: %s)", message, networkName);
        errors.add(new RentalUpdaterError(severity, message));
        LOG.error(String.format("[severity: %s] %s", severity, message));
    }

    @Override
    public void update () {
        // reset update statuses and data
        vehiclesUpdated = false;
        regionsUpdated = false;
        stations = new LinkedList<>();
        errors = new LinkedList<>();
        systemInformationData = null;
        systemStartDate = null;

        String systemInformationUrl = null;
        String stationInformationUrl = null;
        String stationStatusUrl = null;
        String freeBikeStatusUrl = null;
        String systemHoursUrl = null;
        String systemCalendarUrl = null;
        String systemRegionsUrl = null;
        String systemPricingPlansUrl = null;
        String systemAlertsUrl = null;

        // fetch data from root URL. This file/endpoint is actually not required per the GBFS spec
        // See https://github.com/NABSA/gbfs/blob/master/gbfs.md#files
        InputStream rootData = fetchFromUrl(makeGbfsEndpointUrl("gbfs.json"));

        // Check to see if data from the root url was able to be fetched. The GBFS.json file is not required.
        if (rootData == null) {
            // Root GBFS.json file not able to be fetched, set default endpoints.
            systemInformationUrl = makeGbfsEndpointUrl("system_information.json");
            stationInformationUrl = makeGbfsEndpointUrl("station_information.json");
            stationStatusUrl = makeGbfsEndpointUrl("station_status.json");
            freeBikeStatusUrl = makeGbfsEndpointUrl("free_bike_status.json");
            systemHoursUrl = makeGbfsEndpointUrl("system_hours.json");
            systemCalendarUrl = makeGbfsEndpointUrl("system_calendar.json");
            systemRegionsUrl = makeGbfsEndpointUrl("system_regions.json");
            systemPricingPlansUrl = makeGbfsEndpointUrl("system_pricing_plans.json");
            systemAlertsUrl = makeGbfsEndpointUrl("system_alerts.json");
        } else {
            // GBFS.json file is found. Parse data from response and set all of the corresponding URLs as they are
            // available in the response data.
            GbfsResponse gbfsResponse = null;
            try {
                gbfsResponse = mapper.readValue(rootData, GbfsResponse.class);
            } catch (IOException e) {
                addError(
                    RentalUpdaterError.Severity.FEED_WIDE,
                    "Failed to deserialize gbfs.json response: %s",
                    e
                );
                return;
            } finally {
                try {
                    rootData.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (gbfsResponse.data == null) {
                addError(RentalUpdaterError.Severity.FEED_WIDE, "Failed to read gbfs.json, no data found");
                return;
            }

            // Get the GBFS feeds for the configured language.
            // FIXME: the configured language always defaults to "en" in this current implementation.
            GbfsResponse.GbfsFeeds feeds = gbfsResponse.data.get(language);
            if (feeds == null) {
                addError(
                    RentalUpdaterError.Severity.FEED_WIDE,
                    "requested language (%s) not available in GBFS: %s",
                    language,
                    rootUrl
                );
                return;
            }

            // iterate through all feed endpoints and update as needed
            for (GbfsResponse.GbfsFeed feed : feeds.feeds) {
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

        // get basic system information. Although this URL/file is technically required, don't fail fast if fetching
        // data from this service doesn't work for some reason.
        updateSystemInformation(systemInformationUrl);

        // TODO: make the following methods asynchronous
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
    private String makeGbfsEndpointUrl(String file) {
        String baseUrl = rootUrl;
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        return String.format("%s%s", baseUrl, file);
    }

    /**
     * Update the system information URL.
     */
    private void updateSystemInformation(String url) {
        if (url == null) {
            addError(
                RentalUpdaterError.Severity.SYSTEM_INFORMATION,
                "system_information URL is required, but none was found."
            );
            return;
        }
        SystemInformation data = fetchAndParseFromUrl(url, SystemInformation.class, true);
        if (data == null) {
            addError(
                RentalUpdaterError.Severity.SYSTEM_INFORMATION,
                "failed to fetch or parse required data from system_information URL."
            );
            return;
        }
        systemInformationData = data.data;
    }

    /**
     * Only add and log an error if the config specifically notes that docking stations are expected in this feed.
     */
    private void addErrorIfDocksExpected(String message) {
        if (hasDocks) {
            addError(RentalUpdaterError.Severity.ALL_STATIONS, message);
        }
    }

    private void updateDockedStationInformation(String stationInformationUrl, String stationStatusUrl) {
        // get the station information
        if (stationInformationUrl == null) {
            // this URL/file is not required unless the system uses docks.
            addErrorIfDocksExpected("Conditionally required station_information URL is not defined.");

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
            addErrorIfDocksExpected("Failed to fetched/parse station info.");
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
            addError(
                RentalUpdaterError.Severity.ALL_STATIONS,
                "Station information found, but station status URL is not defined."
            );
            return;
        }
        StationStatus stationStatus = fetchAndParseFromUrl(stationStatusUrl, StationStatus.class, hasDocks);
        if (stationStatus == null) {
            // this file is required, so something went wrong.
            addErrorIfDocksExpected("Failed to fetch/parse station status.");
            // Don't process null station status info
            return;
        }
        // update each vehicle rental station found in station information URL/file.
        for (StationStatus.DockingStationStatusInformation station : stationStatus.data.stations) {
            VehicleRentalStation vehicleRentalStation = stationsByStationId.get(station.station_id);
            if (vehicleRentalStation == null) {
                addError(
                    RentalUpdaterError.Severity.INDIVIDUAL_DOCKING_STATION,
                    "Station with id: %s not found in station information data.",
                    station.station_id
                );
                continue;
            }
            if (station.num_bikes_available == null) {
                addError(
                    RentalUpdaterError.Severity.INDIVIDUAL_DOCKING_STATION,
                    "Station with id: %s missing required information on number of vehicles available.",
                    station.station_id
                );
                continue;
            }
            if (station.num_docks_available == null) {
                addError(
                    RentalUpdaterError.Severity.INDIVIDUAL_DOCKING_STATION,
                    "Station with id: %s missing required information on number of docks available.",
                    station.station_id
                );
                continue;
            }
            vehicleRentalStation.spacesAvailable = station.num_docks_available;
            vehicleRentalStation.vehiclesAvailable = station.num_bikes_available;

            // assume pickups and dropoffs are allowed if installed if optional data is not provided
            vehicleRentalStation.allowPickup = (station.is_installed == null || station.is_installed == 1) &&
                (station.is_renting == null || station.is_renting == 1);
            vehicleRentalStation.allowDropoff = (station.is_installed == null || station.is_installed == 1) &&
                (station.is_returning == null || station.is_returning == 1);

            // set the last reported time
            vehicleRentalStation.lastReportedEpochSeconds = RentalStation.getLastReportedTimeUsingFallbacks(
                station.last_reported,
                stationStatus.last_updated
            );
        }
        stations.addAll(stationsByStationId.values());
        vehiclesUpdated = true;
    }

    private void updateFreeFloatingVehicles(String freeBikeStatusUrl) {
        FreeBikeStatus floatingBikes = fetchAndParseFromUrl(freeBikeStatusUrl, FreeBikeStatus.class);
        if (floatingBikes == null) {
            addError(
                RentalUpdaterError.Severity.ALL_FLOATING_VEHICLES,
                "Unable to fetch/parse floating vehicles."
            );
            return;
        }
        for (FreeBikeStatus.FreeBike bike : floatingBikes.data.bikes) {
            if (!bike.is_disabled && !bike.is_reserved) {
                VehicleRentalStation floatingVehicle = new VehicleRentalStation();
                // some GBFS feeds have `null` as the value for bike_id. If that happens, just set the id to be a UUID.
                floatingVehicle.id = bike.bike_id == null
                    ? UUID.randomUUID().toString()
                    : bike.bike_id;
                floatingVehicle.name = new NonLocalizedString(bike.bike_id);
                floatingVehicle.x = bike.lon;
                floatingVehicle.y = bike.lat;

                floatingVehicle.allowDropoff = false;
                floatingVehicle.allowPickup = true;
                floatingVehicle.isFloatingVehicle = true;
                floatingVehicle.networks = Sets.newHashSet(networkName);
                floatingVehicle.spacesAvailable = 0;
                floatingVehicle.vehiclesAvailable = 1;
                floatingVehicle.lastReportedEpochSeconds = RentalStation.getLastReportedTimeUsingFallbacks(
                    bike.last_reported,
                    floatingBikes.last_updated
                );

                stations.add(floatingVehicle);
            }
        }
        vehiclesUpdated = true;
    }

    /**
     * Parse any updates to the vehicle rental regions as received from the GBFS.
     */
    private void updateRegions() {
        // FIXME currently the vehicle rental regions are only loaded upon the startup of OTP and have no way of being
        //   updated while the application is running.
    }

    /**
     * Attempt to parse geojson and set that to be the region.  Currently supported types include
     * either a single Feature or a FeatureCollection.
     */
    private List<VehicleRentalRegion> parseRegionJson(JsonNode regionJson) {
        final VehicleRentalRegion region = new VehicleRentalRegion();
        region.network = networkName;
        try {
            region.geometry = parsePolygonOrMultiPolygonFromJsonNode(regionJson);
        } catch (UnsupportedGeometryException | IOException e) {
            LOG.error("Could not deserialize GeoJSON for {}", networkName);
            return new ArrayList<>();
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
            data = getDataFromUrlOrFile(url, headerName, headerValue);
        } catch (IOException e) {
            LOG.warn("Failed to fetch from url: {}. Error: {}", url, e);
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
