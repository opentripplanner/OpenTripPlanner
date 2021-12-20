package org.opentripplanner.updater.vehicle_parking;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingState;
import org.opentripplanner.updater.GenericJsonDataSource;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TranslatedString;

abstract class ParkAPIUpdater extends GenericJsonDataSource<VehicleParking> {

    private static final String JSON_PARSE_PATH = "lots";

    private final String feedId;
    private final Collection<String> staticTags;

    public ParkAPIUpdater(
            String url,
            String feedId,
            Map<String, String> httpHeaders,
            Collection<String> staticTags
    ) {
        super(url, JSON_PARSE_PATH, httpHeaders);
        this.feedId = feedId;
        this.staticTags = staticTags;
    }

    @Override
    protected VehicleParking parseElement(JsonNode jsonNode) {

        var capacity = parseCapacity(jsonNode);
        var availability = parseAvailability(jsonNode);

        if (capacity == null) {
            return null;
        }

        I18NString note = null;
        if (jsonNode.has("notes") && !jsonNode.get("notes").isEmpty()) {
            var noteFieldIterator = jsonNode.path("notes").fields();
            Map<String, String> noteLocalizations = new HashMap<>();
            while (noteFieldIterator.hasNext()) {
                var noteFiled = noteFieldIterator.next();
                noteLocalizations.put(noteFiled.getKey(), noteFiled.getValue().asText());
            }
            note = TranslatedString.getI18NString(noteLocalizations);
        }

        var vehicleParkId = createIdForNode(jsonNode);
        double x = jsonNode.path("coords").path("lng").asDouble();
        double y = jsonNode.path("coords").path("lat").asDouble();

        VehicleParking.VehicleParkingEntranceCreator entrance = builder -> builder
                .entranceId(new FeedScopedId(feedId, vehicleParkId.getId() + "/entrance"))
                .name(new NonLocalizedString(jsonNode.path("name").asText()))
                .x(x)
                .y(y)
                .walkAccessible(true)
                .carAccessible(true);

        var stateText = jsonNode.get("state").asText();
        var state = stateText.equals("closed")
                ? VehicleParkingState.CLOSED
                : VehicleParkingState.OPERATIONAL;

        var tags = parseTags(jsonNode, "lot_type", "address", "forecast", "state");
        tags.addAll(staticTags);

        return VehicleParking.builder()
                .id(vehicleParkId)
                .name(new NonLocalizedString(jsonNode.path("name").asText()))
                .state(state)
                .x(x)
                .y(y)
                // TODO
                // .openingHours(parseOpeningHours(jsonNode.path("opening_hours")))
                // .feeHours(parseOpeningHours(jsonNode.path("fee_hours")))
                .detailsUrl(jsonNode.has("url") ? jsonNode.get("url").asText() : null)
                .note(note)
                .capacity(capacity)
                .availability(availability)
                .bicyclePlaces(hasPlaces(capacity.getBicycleSpaces()))
                .carPlaces(hasPlaces(capacity.getCarSpaces()))
                .wheelchairAccessibleCarPlaces(hasPlaces(capacity.getWheelchairAccessibleCarSpaces()))
                .entrance(entrance)
                .tags(tags)
                .build();
    }

    abstract VehicleParkingSpaces parseCapacity(JsonNode jsonNode);

    abstract VehicleParkingSpaces parseAvailability(JsonNode jsonNode);

    protected VehicleParkingSpaces parseVehicleSpaces(
            JsonNode node,
            String bicycleTag,
            String carTag,
            String wheelchairAccessibleCarTag
    ) {
        var bicycleSpaces = parseSpacesValue(node, bicycleTag);
        var carSpaces = parseSpacesValue(node, carTag);
        var wheelchairAccessibleCarSpaces = parseSpacesValue(node, wheelchairAccessibleCarTag);

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

    private boolean hasPlaces(Integer spaces) {
        return spaces != null && spaces > 0;
    }

    // TODO
    // @SneakyThrows
    // private TimeRestriction parseOpeningHours(JsonNode jsonNode) {
    //     if (jsonNode == null || jsonNode.asText().isBlank()) {
    //         return null;
    //     }

    //     return OsmOpeningHours.parseFromOsm(jsonNode.asText());
    // }

    private Integer parseSpacesValue(JsonNode jsonNode, String fieldName) {
        if (!jsonNode.has(fieldName)) {
            return null;
        }
        return jsonNode.get(fieldName).asInt();
    }

    private FeedScopedId createIdForNode(JsonNode jsonNode) {
        String id;
        if (jsonNode.has("id")) {
            id = jsonNode.path("id").asText();
        }
        else {
            id = String.format("%s/%f/%f", jsonNode.get("name"),
                    jsonNode.path("coords").path("lng").asDouble(),
                    jsonNode.path("coords").path("lat").asDouble()
            );
        }
        return new FeedScopedId(feedId, id);
    }

    private List<String> parseTags(JsonNode node, String... tagNames) {
        var tagList = new ArrayList<String>();
        for (var tagName : tagNames) {
            if (node.has(tagName)) {
                tagList.add(tagName + ":" + node.get(tagName).asText());
            }
        }
        return tagList;
    }
}
