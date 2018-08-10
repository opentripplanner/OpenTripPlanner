package org.opentripplanner.updater.car_rental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.opentripplanner.analyst.UnsupportedGeometryException;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.car_rental.CarFuelType;
import org.opentripplanner.routing.car_rental.CarRentalRegion;
import org.opentripplanner.routing.car_rental.CarRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Car2GoCarRentalDataSource implements CarRentalDataSource, JsonConfigurable {
    private static final Logger LOG = LoggerFactory.getLogger(CarRentalDataSource.class);

    // the url to fetch the car2go vehicle data from
    private String vehiclesUrl;
    private List<CarRentalStation> stations;
    private List<CarRentalRegion> regions;

    @Override
    public void configure(Graph graph, JsonNode jsonNode) throws Exception {
        String vehiclesUrl = jsonNode.path("vehiclesUrl").asText(); // path() returns MissingNode not null.
        if (vehiclesUrl == null) {
            throw new IllegalArgumentException("Missing mandatory 'vehiclesUrl' configuration.");
        }
        this.setVehiclesUrl(vehiclesUrl);

        // parse region geojson if present
        JsonNode regionJson = jsonNode.get("regionGeoJson");
        if (regionJson == null) {
            LOG.warn("regionGeoJson not found in configuration for Car2Go Car Rental Datasource." +
                "Dropoffs are assumed to be allowed in full extent of graph.");
            GeometryFactory geometryFactory = new GeometryFactory();
            CarRentalRegion entireEarthRegion = new CarRentalRegion();
            entireEarthRegion.network = "car2go";
            entireEarthRegion.geometry = geometryFactory.toGeometry(new Envelope(-180,180,-90,90));
            regions = Arrays.asList(entireEarthRegion);
        } else {
            regions = parseRegionJson(regionJson);
        }
    }

    public void setVehiclesUrl(String url) {
        this.vehiclesUrl = url;
    }

    public List<CarRentalRegion> parseRegionJson(JsonNode regionJson) {
        ObjectMapper jsonDeserializer = new ObjectMapper();
        final CarRentalRegion region = new CarRentalRegion();
        region.network = "car2go";
        Feature geoJsonFeature = null;
        try {
            geoJsonFeature = jsonDeserializer.readValue(regionJson.traverse(), Feature.class);
        } catch (IOException e) {
            LOG.error("Could not deserialize geojson");
            return new ArrayList<>();
        }
        GeoJsonObject geometry = geoJsonFeature.getGeometry();
        try {
            region.geometry = GeometryUtils.convertGeoJsonToJtsGeometry(geometry);
        } catch (UnsupportedGeometryException e) {
            LOG.error("Could not convert geojson to geometry");
            return new ArrayList<>();
        }
        return Arrays.asList(region);
    }

    @Override
    public boolean update() {
        return updateStations() && updateRegions();
    }

    // placeholder for potential future updates of region
    private boolean updateRegions() {
        return true;
    }

    private boolean updateStations() {
        try {
            InputStream data = null;

            URL url2 = new URL(vehiclesUrl);

            String proto = url2.getProtocol();
            if (proto.equals("http") || proto.equals("https")) {
                data = HttpUtils.getData(vehiclesUrl);
            } else {
                // Local file probably, try standard java
                data = url2.openStream();
            }

            if (data == null) {
                LOG.warn("Failed to get data from url " + vehiclesUrl);
                return false;
            }
            parseVehiclesJSON(data);
            data.close();
        } catch (IOException e) {
            LOG.warn("Error reading bike rental feed from " + vehiclesUrl, e);
            return false;
        }
        return true;
    }

    private void parseVehiclesJSON(InputStream data) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(data);
        JsonNode vehicles = rootNode.get("vehicles");
        ArrayList<CarRentalStation> stations = new ArrayList<>();
        if (vehicles.isArray()) {
            for (JsonNode vehicle : vehicles) {
                stations.add(makeStation(vehicle));
            }
        }
        this.stations = stations;
    }

    private CarRentalStation makeStation(JsonNode vehicle) {
        CarRentalStation car2go = new CarRentalStation();
        car2go.address = vehicle.get("address").asText();
        car2go.allowDropoff = false;
        car2go.allowPickup = vehicle.get("freeForRental").asBoolean();
        car2go.carsAvailable = 1;
        car2go.fuelType = CarFuelType.forValue(vehicle.get("fuelType").asText());
        car2go.id = vehicle.get("plate").asText();
        car2go.isFloatingCar = true;
        car2go.licensePlate = vehicle.get("plate").asText();
        car2go.name = new NonLocalizedString(vehicle.get("plate").asText());
        car2go.networks = new HashSet<>(Arrays.asList("car2go"));
        car2go.spacesAvailable = 0;
        car2go.x = vehicle.path("geoCoordinate").path("longitude").asDouble();
        car2go.y = vehicle.path("geoCoordinate").path("latitude").asDouble();

        return car2go;
    }

    @Override
    public List<CarRentalStation> getStations() {
        return stations;
    }

    @Override
    public List<CarRentalRegion> getRegions() {
        return regions;
    }

}
