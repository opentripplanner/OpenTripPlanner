package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.URIBuilder;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.bike_rental.BikeRentalRegion;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class CoordBikeRentalDataSource implements BikeRentalDataSource, JsonConfigurable {

    private static final Logger LOG = LoggerFactory.getLogger(CoordBikeRentalDataSource.class);

    private static final ObjectMapper jsonDeserializer = new ObjectMapper();

    private String coordApiHost;
    private Integer coordApiPort;
    private List<CoordBikeDataSource> stationSources = new ArrayList<>();;
    private List<CoordBikeRegionDataSource> regionSources = new ArrayList<>();
    private String accessKey;
    private String coordApiScheme;

    private class CoordAPIParam {
        String name;
        String lat;
        String lng;
        String radius_km;

        public CoordAPIParam(String name, double lat, double lng, double radius_km) {
            this.name = name;
            this.lat = String.format("%3.6f", lat);
            this.lng = String.format("%3.6f", lng);
            this.radius_km = String.format("%3.4f", radius_km);
        }
    }

    @Override
    public boolean update() {
        regionSources.forEach(r -> r.update());
        return updateBikeStations();
    }

    /**
     * @return true if any of the sources succeeds, so that the updater takes the
     *         updates and apply them to the graph. False otherwise.
     */
    private boolean updateBikeStations() {
        boolean anySuccess = false;
        for (CoordBikeDataSource source : stationSources) {
            if (source.update()) {
                anySuccess = true;
            }
        }
        return anySuccess;
    }

    @Override
    public List<BikeRentalStation> getStations() {
        final List<BikeRentalStation> stations = new ArrayList<>();
        stationSources.forEach(s -> stations.addAll(s.getStations()));
        return stations;
    }

    @Override
    public List<BikeRentalRegion> getRegions() {
        final Map<String, BikeRentalRegion> regions = new HashMap<>();
        regionSources.forEach(s -> s.getRegions().forEach(r -> regions.put(r.network, r)));

        if (regions.containsKey("JumpDC")) {
            // TODO(mahmood): Fix this hard-coded line when SpinDC gets its own geometry
            // SpinDC does not have a geometry at the moment, we use JumpDC's geom for SpinDC"
            regions.put("SpinDC", new BikeRentalRegion("SpinDC", regions.get("JumpDC").geometry));
        }
        return new ArrayList<>(regions.values());
    }

    /**
     * Note that the JSON being passed in here is for configuration of the OTP component, it's completely separate
     * from the JSON coming in from the update source.
     */
    @Override
    public void configure(Graph graph, JsonNode jsonNode) {
        this.coordApiHost = jsonNode.path("host").asText();
        this.coordApiScheme = jsonNode.path("scheme").asText();
        this.coordApiPort = jsonNode.path("port").asInt();
        this.accessKey = System.getenv("COORD_API_KEY");
        for (CoordAPIParam param : createCoordAPIParams(jsonNode)) {
            stationSources.add(createStationSource(param));
            regionSources.add(createRegionSource(param));
        }
    }

    private CoordBikeDataSource createStationSource(CoordAPIParam param) {
        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme(this.coordApiScheme)
                    .setHost(this.coordApiHost)
                    .setPort(this.coordApiPort)
                    .setPath("/v1/bike/location")
                    .addParameter("latitude", param.lat)
                    .addParameter("longitude", param.lng)
                    .addParameter("radius_km", param.radius_km)
                    .addParameter("access_key", this.accessKey)
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Error while configuring Coord Data Source: " + e.getMessage());
        }
        final String uriString = uri.toString();
        LOG.info("Configured a Bike Station source for {} with url: {}", param.name, uriString);

        final CoordBikeDataSource stationSource = new CoordBikeDataSource();
        stationSource.setUrl(uriString);
        return stationSource;
    }

    /**
     * Reads Coord's API params from the jsonNode
     * @param jsonNode
     * @return a list of CoordAPIParam
     */
    private List<CoordAPIParam> createCoordAPIParams(JsonNode jsonNode) {
        final JsonNode locations = jsonNode.path("locations");
        List<CoordAPIParam> params = new ArrayList<>();
        for (Iterator<JsonNode> iter = locations.iterator(); iter.hasNext();){
            JsonNode location = iter.next();
            params.add(
                    new CoordAPIParam(
                            location.path("name").asText(),
                            location.path("lat").asDouble(),
                            location.path("lng").asDouble(),
                            location.path("radius_km").asDouble()
                    ));

        }
        return params;
    }

    private CoordBikeRegionDataSource createRegionSource(CoordAPIParam param) {
        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme(this.coordApiScheme)
                    .setHost(this.coordApiHost)
                    .setPort(this.coordApiPort)
                    .setPath("/v1/bike/system")
                    .addParameter("latitude", param.lat)
                    .addParameter("longitude", param.lng)
                    .addParameter("radius_km", param.radius_km)
                    .addParameter("access_key", this.accessKey)
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Error while configuring Coord Data Source: " + e.getMessage());
        }
        final String uriRegionString = uri.toString();
        LOG.info("Configured a Bike Region source with url: {}", uriRegionString);
        final CoordBikeRegionDataSource regionSource = new CoordBikeRegionDataSource();
        regionSource.setUrl(uriRegionString);
        return regionSource;
    }

    class CoordBikeDataSource extends GenericJsonBikeStationDataSource {

        public CoordBikeDataSource() {
            super("features");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            final JsonNode properties = stationNode.path("properties");
            final JsonNode coordinates = stationNode.path("geometry").path("coordinates");
            final Iterator<JsonNode> coordinateIterator = coordinates.iterator();

            final BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("id").asText();
            brstation.name = new NonLocalizedString(stationNode.path("id").asText());
            brstation.x = coordinateIterator.next().asDouble();
            brstation.y = coordinateIterator.next().asDouble();

            JsonNode numBikes = properties.get("num_bikes_available");
            JsonNode numDocks = properties.get("num_docks_available");
            JsonNode isReturning = properties.get("is_returning");
            JsonNode isRenting = properties.get("is_renting");

            brstation.bikesAvailable = numBikes != null ? numBikes.asInt() : 0;
            brstation.spacesAvailable = numDocks != null ? numDocks.asInt() : 0;
            brstation.allowDropoff = isReturning != null ? isReturning.asBoolean() : false;
            brstation.allowPickup = isRenting != null ? isRenting.asBoolean() : false;

            String locationType = properties.get("location_type").asText();
            brstation.isFloatingBike = locationType.equals("free_bike") || locationType.equals("bike_station_hub");

            // Set the system ID as the network.
            brstation.networks = new HashSet<>(Arrays.asList(properties.path("system_id").asText()));
            return brstation;
        }
    }

    class CoordBikeRegionDataSource extends GenericJsonBikeRegionDataSource {

        public CoordBikeRegionDataSource() {
            super("features");
        }

        @Override
        public BikeRentalRegion makeRegion(JsonNode jsonNode) {
            final JsonNode properties = jsonNode.path("properties");
            final String stationType = properties.get("station_type").asText();
            final boolean isFloatingBike = "dockless_with_hub".equals(stationType) || "dockless".equals(stationType);
            if (!isFloatingBike) {
                return null; // Skipping this bike network as it's not dockless
            }

            final BikeRentalRegion region = new BikeRentalRegion();
            region.network = jsonNode.path("id").asText();
            try {
                Feature geoJsonFeature;
                geoJsonFeature = jsonDeserializer.readValue(jsonNode.traverse(), Feature.class);
                GeoJsonObject geometry = geoJsonFeature.getGeometry();
                if (geometry == null) {
                    LOG.warn("The geometry for bike network {} is missing.", region.network);
                    return null;
                }
                region.geometry = GeometryUtils.convertGeoJsonToJtsGeometry(geometry);
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("Unsupported geometry exception: {}", region.network);
                return null;
            }
            return region;
        }
    }
}
