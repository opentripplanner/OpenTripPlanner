package org.opentripplanner.ext.vehicleparking.hslpark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.GeometryDeserializer;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingState;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps a HSL Park facility into a {@link VehicleParking}.
 */
public class HslParkToVehicleParkingMapper {

    private static final Logger log = LoggerFactory.getLogger(HslParkToVehicleParkingMapper.class);

    private String feedId;

    public HslParkToVehicleParkingMapper(String feedId) {
        this.feedId = feedId;
    }

    public VehicleParking parsePark(JsonNode jsonNode) {
        var vehicleParkId = createIdForNode(jsonNode, "id", feedId);
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

    public static FeedScopedId createIdForNode(JsonNode jsonNode, String idName, String feedId) {
        String id = jsonNode.path(idName).asText();
        return new FeedScopedId(feedId, id);
    }

    public static Integer parseIntegerValue(JsonNode jsonNode, String fieldName) {
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
}
