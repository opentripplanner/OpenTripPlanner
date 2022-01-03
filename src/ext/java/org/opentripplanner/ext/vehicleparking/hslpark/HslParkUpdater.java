package org.opentripplanner.ext.vehicleparking.hslpark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.GeometryDeserializer;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingState;
import org.opentripplanner.updater.DataSource;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;
import org.opentripplanner.util.xml.JsonDataListDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vehicle parking updater class for https://github.com/HSLdevcom/parkandrideAPI format APIs. There
 * has been further development in a private repository (the current state is documented in
 * https://p.hsl.fi/docs/index.html) but this updater supports both formats.
 */
public class HslParkUpdater implements DataSource<VehicleParking> {

    private static final Logger log = LoggerFactory.getLogger(HslParkUpdater.class);

    private static final String JSON_PARSE_PATH = "results";

    private final JsonDataListDownloader facilitiesDownloader;
    private final int facilitiesFrequencySec;
    private final String feedId;
    private final JsonDataListDownloader utilizationsDownloader;

    private long lastFacilitiesFetchTime;

    private Map<String, Integer> utilizations = Collections.EMPTY_MAP;

    private List<VehicleParking> parks;

    public HslParkUpdater(HslParkUpdaterParameters parameters) {
        facilitiesDownloader =
                new JsonDataListDownloader<>(
                        parameters.getFacilitiesUrl(), JSON_PARSE_PATH, this::parsePark, null);
        utilizationsDownloader =
                new JsonDataListDownloader<>(
                        parameters.getUtilizationsUrl(), "", this::parseUtilization, null);
        this.facilitiesFrequencySec = parameters.getFacilitiesFrequencySec();
        this.feedId = parameters.getFeedId();
    }

    private VehicleParking parsePark(JsonNode jsonNode) {
        var vehicleParkId = createIdForNode(jsonNode, "id");
        try {
            var capacity = parseVehicleSpaces(jsonNode.path("builtCapacity"), "BICYCLE", "CAR",
                    "DISABLED"
            );
            Map<String, String> translations = new HashMap<>();
            JsonNode nameNode = jsonNode.path("name");
            nameNode.fieldNames().forEachRemaining(lang -> {
                String name = nameNode.path(lang).asText();
                if (!name.equals("")) {
                    translations.put(lang, nameNode.path(lang).asText());
                }
            });
            I18NString name = translations.isEmpty()
                    ? new NonLocalizedString(vehicleParkId.getId())
                    : TranslatedString.getI18NString(translations);
            Geometry geometry = GeometryDeserializer.parseGeometry(jsonNode.path("location"));
            double x = geometry.getCentroid().getX();
            double y = geometry.getCentroid().getY();

            var stateText = jsonNode.path("status").asText();
            var state = stateMapper(stateText);

            var tags = parseTags(jsonNode);
            var maybeCapacity = Optional.ofNullable(capacity);
            var bicyclePlaces =
                    maybeCapacity.map(c -> hasPlaces(capacity.getBicycleSpaces())).orElse(false);
            var carPlaces =
                    maybeCapacity.map(c -> hasPlaces(capacity.getCarSpaces())).orElse(false);
            var wheelChairAccessiblePlaces =
                    maybeCapacity.map(c -> hasPlaces(capacity.getWheelchairAccessibleCarSpaces()))
                            .orElse(false);

            this.lastFacilitiesFetchTime = System.currentTimeMillis();
            return VehicleParking.builder()
                    .id(vehicleParkId)
                    .name(name)
                    .state(state)
                    .x(x)
                    .y(y)
                    .capacity(capacity)
                    .bicyclePlaces(bicyclePlaces)
                    .carPlaces(carPlaces)
                    .wheelchairAccessibleCarPlaces(wheelChairAccessiblePlaces)
                    .tags(tags)
                    .entrance((builder) -> builder
                            .entranceId(
                                    new FeedScopedId(feedId, vehicleParkId.getId() + "/entrance"))
                            .name(name)
                            .x(x)
                            .y(y)
                            .walkAccessible(true)
                            .carAccessible(carPlaces || wheelChairAccessiblePlaces))
                    .build();
        }
        catch (Exception e) {
            log.warn("Error parsing park " + vehicleParkId, e);
            return null;
        }
    }

    private HslParkPatch parseUtilization(JsonNode jsonNode) {
        var vehicleParkId = createIdForNode(jsonNode, "facilityId");
        try {
            String capacityType = jsonNode.path("capacityType").asText();
            Integer spacesAvailable = parseIntegerValue(jsonNode, "spacesAvailable");
            return new HslParkPatch(vehicleParkId, capacityType, spacesAvailable);
        }
        catch (Exception e) {
            log.warn("Error parsing park utilization" + vehicleParkId, e);
            return null;
        }
    }

    public Integer parseIntegerValue(JsonNode jsonNode, String fieldName) {
        if (!jsonNode.has(fieldName)) {
            return null;
        }
        return jsonNode.get(fieldName).asInt();
    }

    private VehicleParkingSpaces parseVehicleSpaces(
            JsonNode node,
            String bicycleTag,
            String carTag,
            String wheelchairAccessibleCarTag
    ) {
        var bicycleSpaces = parseIntegerValue(node, bicycleTag);
        var carSpaces = parseIntegerValue(node, carTag);
        var wheelchairAccessibleCarSpaces = parseIntegerValue(node, wheelchairAccessibleCarTag);

        if (bicycleSpaces == null && carSpaces == null && wheelchairAccessibleCarSpaces == null) {
            return null;
        }

        return createVehiclePlaces(carSpaces, wheelchairAccessibleCarSpaces, bicycleSpaces);
    }

    private VehicleParkingSpaces createVehicleAvailability(List<HslParkPatch> patches) {
        Integer carSpaces = null;
        Integer wheelchairAccessibleCarSpaces = null;
        Integer bicycleSpaces = null;

        for (int i = 0; i < patches.size(); i++) {
            HslParkPatch patch = patches.get(i);
            String type = patch.getCapacityType();

            if (type != null) {
                Integer spaces = patch.getSpacesAvailable();

                switch (type) {
                    case "CAR":
                        carSpaces = spaces;
                        break;
                    case "BICYCLE":
                        bicycleSpaces = spaces;
                        break;
                    case "DISABLED":
                        wheelchairAccessibleCarSpaces = spaces;
                        break;
                }
            }
        }
        if (bicycleSpaces == null && carSpaces == null && wheelchairAccessibleCarSpaces == null) {
            return null;
        }
        return createVehiclePlaces(carSpaces, wheelchairAccessibleCarSpaces, bicycleSpaces);
    }

    private VehicleParkingSpaces createVehiclePlaces(
            Integer carSpaces,
            Integer wheelchairAccessibleCarSpaces,
            Integer bicycleSpaces
    ) {
        return VehicleParkingSpaces.builder()
                .bicycleSpaces(bicycleSpaces)
                .carSpaces(carSpaces)
                .wheelchairAccessibleCarSpaces(wheelchairAccessibleCarSpaces)
                .build();
    }

    private VehicleParkingState stateMapper(String stateText) {
        if (stateText == null) {
            return VehicleParkingState.OPERATIONAL;
        }
        switch (stateText) {
            case "INACTIVE":
                return VehicleParkingState.CLOSED;
            case "TEMPORARILY_CLOSED":
                return VehicleParkingState.TEMPORARILY_CLOSED;
            case "IN_OPERATION":
            case "EXCEPTIONAL_SITUATION":
            default:
                return VehicleParkingState.OPERATIONAL;
        }
    }

    private boolean hasPlaces(Integer spaces) {
        return spaces != null && spaces > 0;
    }

    private FeedScopedId createIdForNode(JsonNode jsonNode, String idName) {
        String id = jsonNode.path(idName).asText();
        return new FeedScopedId(feedId, id);
    }

    private List<String> parseTags(JsonNode node) {
        var tagList = new ArrayList<String>();
        ArrayNode servicesArray = (ArrayNode) node.get("services");
        if (servicesArray != null && servicesArray.isArray()) {
            for (JsonNode jsonNode : servicesArray) {
                tagList.add(feedId + ":SERVICE_" + jsonNode.asText());
            }
        }
        ArrayNode authenticationMethods = (ArrayNode) node.get("authenticationMethods");
        if (authenticationMethods != null && authenticationMethods.isArray()) {
            for (JsonNode jsonNode : authenticationMethods) {
                tagList.add(feedId + ":AUTHENTICATION_METHOD_" + jsonNode.asText());
            }
        }
        if (node.has("pricingMethod")) {
            tagList.add(feedId + ":PRICING_METHOD_" + node.path("pricingMethod").asText());
        }
        return tagList;
    }

    /**
     * Update the data from the sources. It first fetches parks from the facilities URL and then
     * realtime updates from utilizations URL. If facilitiesFrequencySec is configured to be over 0,
     * it also occasionally retches the parks as new parks might have been added or the state of the
     * old parks might have changed.
     *
     * @return true if there might have been changes
     */
    @Override
    public boolean update() {
        // Only refetch parks when facilitiesFrequencySec > 0 and over facilitiesFrequencySec has passed since last successful fetch
        List<VehicleParking> parks =
                this.parks == null || (
                        facilitiesFrequencySec > 0 && System.currentTimeMillis()
                                > lastFacilitiesFetchTime + facilitiesFrequencySec * 1000
                ) ? facilitiesDownloader.download() : this.parks;
        if (parks != null) {
            List<HslParkPatch> utilizations = utilizationsDownloader.download();
            if (utilizations != null) {
                Map<FeedScopedId, List<HslParkPatch>> patches = utilizations.stream()
                        .collect(Collectors.groupingBy(utilization -> utilization.getId()));
                parks.forEach(park -> {
                    List<HslParkPatch> patchesForPark = patches.get(park.getId());
                    if (patchesForPark != null) {
                        park.updateAvailability(createVehicleAvailability(patchesForPark));
                    }
                });
            }
            else if (this.parks != null) {
                return false;
            }
            synchronized (this) {
                // Update atomically
                this.parks = parks;
            }
            return true;
        }
        return false;
    }

    @Override
    public synchronized List<VehicleParking> getUpdates() {
        return parks;
    }
}
