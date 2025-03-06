package org.opentripplanner.ext.vehicleparking.parkapi;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.osm.OsmOpeningHoursParser;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.spi.GenericJsonDataSource;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vehicle parking updater class for https://github.com/offenesdresden/ParkAPI format APIs.
 */
abstract class ParkAPIUpdater extends GenericJsonDataSource<VehicleParking> {

  private static final Logger LOG = LoggerFactory.getLogger(ParkAPIUpdater.class);

  private static final String JSON_PARSE_PATH = "lots";

  private final String feedId;
  private final Collection<String> staticTags;

  private final OsmOpeningHoursParser osmOpeningHoursParser;
  private final String url;

  public ParkAPIUpdater(
    ParkAPIUpdaterParameters parameters,
    OpeningHoursCalendarService openingHoursCalendarService
  ) {
    super(parameters.url(), JSON_PARSE_PATH, parameters.httpHeaders());
    this.feedId = parameters.feedId();
    this.staticTags = parameters.tags();
    this.osmOpeningHoursParser = new OsmOpeningHoursParser(
      openingHoursCalendarService,
      parameters.timeZone()
    );
    this.url = parameters.url();
  }

  @Override
  protected VehicleParking parseElement(JsonNode jsonNode) {
    var capacity = parseCapacity(jsonNode);
    var availability = parseAvailability(jsonNode);

    I18NString note = null;
    if (jsonNode.has("notes") && !jsonNode.get("notes").isEmpty()) {
      var noteFieldIterator = jsonNode.path("notes").fields();
      Map<String, String> noteLocalizations = new HashMap<>();
      while (noteFieldIterator.hasNext()) {
        var noteFiled = noteFieldIterator.next();
        noteLocalizations.put(noteFiled.getKey(), noteFiled.getValue().asText());
      }
      note = TranslatedString.getI18NString(noteLocalizations, true, false);
    }

    var vehicleParkId = createIdForNode(jsonNode);
    double x = jsonNode.path("coords").path("lng").asDouble();
    double y = jsonNode.path("coords").path("lat").asDouble();

    VehicleParking.VehicleParkingEntranceCreator entrance = builder ->
      builder
        .entranceId(new FeedScopedId(feedId, vehicleParkId.getId() + "/entrance"))
        .name(new NonLocalizedString(jsonNode.path("name").asText()))
        .coordinate(new WgsCoordinate(y, x))
        .walkAccessible(true)
        .carAccessible(true);

    var stateText = jsonNode.get("state").asText();
    var state = stateText.equals("closed")
      ? VehicleParkingState.CLOSED
      : VehicleParkingState.OPERATIONAL;

    var tags = parseTags(jsonNode, "lot_type", "address", "forecast", "state");
    tags.addAll(staticTags);

    var maybeCapacity = Optional.ofNullable(capacity);
    var bicyclePlaces = maybeCapacity
      .map(c -> hasPlaces(capacity.getBicycleSpaces()))
      .orElse(false);
    var carPlaces = maybeCapacity.map(c -> hasPlaces(capacity.getCarSpaces())).orElse(true);
    var wheelChairAccessiblePlaces = maybeCapacity
      .map(c -> hasPlaces(capacity.getWheelchairAccessibleCarSpaces()))
      .orElse(false);

    return VehicleParking.builder()
      .id(vehicleParkId)
      .name(new NonLocalizedString(jsonNode.path("name").asText()))
      .state(state)
      .coordinate(new WgsCoordinate(y, x))
      .openingHoursCalendar(parseOpeningHours(jsonNode.path("opening_hours"), vehicleParkId))
      .detailsUrl(jsonNode.has("url") ? jsonNode.get("url").asText() : null)
      .imageUrl(jsonNode.has("image_url") ? jsonNode.get("image_url").asText() : null)
      .note(note)
      .capacity(capacity)
      .availability(availability)
      .bicyclePlaces(bicyclePlaces)
      .carPlaces(carPlaces)
      .wheelchairAccessibleCarPlaces(wheelChairAccessiblePlaces)
      .entrance(entrance)
      .tags(tags)
      .build();
  }

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

  abstract VehicleParkingSpaces parseCapacity(JsonNode jsonNode);

  abstract VehicleParkingSpaces parseAvailability(JsonNode jsonNode);

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

  private OHCalendar parseOpeningHours(JsonNode jsonNode, FeedScopedId id) {
    if (jsonNode == null || jsonNode.asText().isBlank()) {
      return null;
    }

    try {
      return osmOpeningHoursParser.parseOpeningHours(jsonNode.asText(), id.toString(), null);
    } catch (OpeningHoursParseException e) {
      LOG.info("Parsing of opening hours failed for park {}, it is now always open:\n{}", id, e);
      return null;
    }
  }

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
    } else {
      id = String.format(
        "%s/%f/%f",
        jsonNode.get("name"),
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

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass()).addStr("feedId", feedId).addObj("url", url).toString();
  }
}
