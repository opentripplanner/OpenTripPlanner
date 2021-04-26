package org.opentripplanner.updater.car_rental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.analyst.UnsupportedGeometryException;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.car_rental.CarRentalRegion;
import org.opentripplanner.routing.car_rental.CarRentalStation;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public abstract class GenericCarRentalDataSource implements CarRentalDataSource, JsonConfigurable {
    private static final Logger LOG = LoggerFactory.getLogger(GenericCarRentalDataSource.class);

    protected String networkName;

    // the minimum amount of time in milliseconds to wait before fetching the regions again
    // default is 1 day
    private static final long MINIMUM_REGION_UPDATE_TIME = 86400000;

    // the url from which to fetch region geojson data
    private String regionsUrl;

    // the url from which to fetch the vehicle data
    private String vehiclesUrl;

    // a list of car rental stations that are currently available to pickup/dropoff vehicles at
    private List<CarRentalStation> stations;

    // a list of regions within which it is possible to dropoff a rental car
    private List<CarRentalRegion> regions;

    // the last time in milliseconds that the regions were updated
    private long lastRegionsUpdateTime = -1;

    // the hash of the last regions response so we can compare and not perform any unneccessary
    // updates if the responses are the same
    private HashCode lastRegionsHash;

    // whether the regions were updated in the last update
    private boolean regionsUpdated;

    // any errors that occured in the last update
    private List<RentalUpdaterError> errors;

    // abstract method for parsing json into a list of CarRentalStations that should be implemented
    // by a company-specific car rental updater
    protected abstract List<CarRentalStation> parseVehicles(InputStream json)
        throws IOException;

    @Override public List<RentalUpdaterError> getErrors() {
        return errors;
    }

    @Override public List<CarRentalStation> getStations() { return stations; }

    @Override public List<CarRentalRegion> getRegions() { return regions; }

    @Override public boolean regionsUpdated() { return regionsUpdated; }

    protected void configure(JsonNode config) {
        String vehiclesUrl = config.path("vehiclesUrl").asText(); // path() returns MissingNode not null.
        if (vehiclesUrl == null) {
            throw new IllegalArgumentException("Missing mandatory 'vehiclesUrl' configuration.");
        }
        this.vehiclesUrl = vehiclesUrl;
        this.setRegionsFromConfig(config);
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
    public void update() {
        regionsUpdated = false;
        stations = new LinkedList<>();
        errors = new LinkedList<>();
        
        updateRegions();
        updateStations();
    }

    /**
     * Update the allowable dropoff regions for the car rental datasource.
     */
    private boolean updateRegions() {
        // regions url is optional, so if it's not set, return false
        if (regionsUrl == null) return false;
        
        // regions do not change that often, so only check after a certain amount of time has 
        // elapsed
        long now = new Date().getTime();
        if (lastRegionsUpdateTime > now - MINIMUM_REGION_UPDATE_TIME) return false;

        InputStream data = null;
        try {
            LOG.debug("fetching data for region of network {}", networkName);

            // fetch regions
            URL url2 = new URL(regionsUrl);

            String proto = url2.getProtocol();
            if (proto.equals("http") || proto.equals("https")) {
                data = HttpUtils.getData(regionsUrl);
            } else {
                // Local file probably, try standard java
                data = url2.openStream();
            }

            if (data == null) {
                addError(
                    RentalUpdaterError.Severity.ALL_REGIONS,
                    "Failed to get data from url " + regionsUrl
                );
                return false;
            }

            // parse the region and update the regions
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(data);
            HashCode responseHash = Hashing.sha256().hashString(
                rootNode.toString(),
                Charset.defaultCharset()
            );

            // no need to update if the response was the same as the last
            if (lastRegionsHash != null && lastRegionsHash.equals(responseHash)) return false;
            lastRegionsHash = responseHash;

            // response is different, update regions
            this.regions = parseRegionJson(rootNode);
            this.regionsUpdated = true;
        } catch (IOException e) {
            addError(
                RentalUpdaterError.Severity.ALL_REGIONS,
                "Failed to get data from url " + regionsUrl
            );
            return false;
        } finally {
            try {
                data.close();
            } catch (IOException e) {
                LOG.warn("Error encountered while trying to close car rental region input stream", e);
                e.printStackTrace();
            }
        }

        LOG.debug("successfully updated regions for network {}", networkName);
        lastRegionsUpdateTime = now;
        return true;
    }

    /**
     * Update all car rental stations.  Fetch from the url and then have the implementing datasource
     * parse the inputstream and return a list of CarRentalStations.
     */
    private boolean updateStations() {
        InputStream data = null;
        try {
            // fetch stations
            URL url2 = new URL(vehiclesUrl);

            String proto = url2.getProtocol();
            if (proto.equals("http") || proto.equals("https")) {
                data = HttpUtils.getData(vehiclesUrl);
            } else {
                // Local file probably, try standard java
                data = url2.openStream();
            }

            if (data == null) {
                addError(
                    // it is assumed that all car rental stations are floating vehicles
                    RentalUpdaterError.Severity.ALL_FLOATING_VEHICLES,
                    "Failed to get data from url " + vehiclesUrl
                );
                return false;
            }

            // delegate parsing of json to implementing datasource and set the result as the
            // stations
            this.stations = parseVehicles(data);
        } catch (IOException e) {
            addError(
                // it is assumed that all car rental stations are floating vehicles
                RentalUpdaterError.Severity.ALL_FLOATING_VEHICLES,
                "Failed to get data from url " + vehiclesUrl
            );
            return false;
        } finally {
            try {
                data.close();
            } catch (IOException e) {
                LOG.warn("Error encountered while trying to close car rental station input stream", e);
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Set the regions from the config.  If the config includes the element regionGeoJson then parse
     * that and set the regions to that.  If the config includes a url, then set that for the
     * updater to use.  If both are included, then the url will take precedence each polling update.
     * If neither are included, the the allowable dropoff region will be the whole earth.
     */
    private void setRegionsFromConfig(JsonNode config) {
        this.regionsUrl = config.get("regionsUrl").asText();
        JsonNode regionGeoJson = config.get("regionsGeoJson");
        if (regionGeoJson == null && regionsUrl == null) {
            LOG.warn("regionGeoJson not found in configuration for Car2Go Car Rental Datasource." +
                         "Dropoffs are assumed to be allowed in full extent of graph.");
            GeometryFactory geometryFactory = new GeometryFactory();
            CarRentalRegion entireEarthRegion = new CarRentalRegion();
            entireEarthRegion.network = networkName;
            entireEarthRegion.geometry = geometryFactory.toGeometry(new Envelope(-180, 180, -90, 90));
            regions = Arrays.asList(entireEarthRegion);
        } else if (regionGeoJson != null) {
            regions = parseRegionJson(regionGeoJson);
        }
    }

    /**
     * Attempt to parse geojson and set that to be the region.  Currently supported types include
     * either a single Feature or a FeatureCollection.
     */
    private List<CarRentalRegion> parseRegionJson(JsonNode regionJson) {
        ObjectMapper jsonDeserializer = new ObjectMapper();
        final CarRentalRegion region = new CarRentalRegion();
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
}
