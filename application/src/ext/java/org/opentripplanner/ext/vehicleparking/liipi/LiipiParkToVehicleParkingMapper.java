package org.opentripplanner.ext.vehicleparking.liipi;

import com.bedatadriven.jackson.datatype.jts.parsers.GenericGeometryParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingGroup;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps a Liipi Park facility into a {@link VehicleParking}.
 */
public class LiipiParkToVehicleParkingMapper {

  private static final Logger log = LoggerFactory.getLogger(LiipiParkToVehicleParkingMapper.class);

  private static final GenericGeometryParser GEOMETRY_PARSER = new GenericGeometryParser(
    GeometryUtils.getGeometryFactory()
  );

  private final String feedId;

  private final OpeningHoursCalendarService openingHoursCalendarService;

  private final ZoneId zoneId;

  public LiipiParkToVehicleParkingMapper(
    String feedId,
    OpeningHoursCalendarService openingHoursCalendarService,
    ZoneId zoneId
  ) {
    this.feedId = feedId;
    this.openingHoursCalendarService = openingHoursCalendarService;
    this.zoneId = zoneId;
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

  public VehicleParking parsePark(
    JsonNode jsonNode,
    Map<FeedScopedId, VehicleParkingGroup> hubForPark
  ) {
    var vehicleParkId = createIdForNode(jsonNode, "id", feedId);
    try {
      var capacity = parseVehicleSpaces(
        jsonNode.path("builtCapacity"),
        "BICYCLE",
        "CAR",
        "DISABLED"
      );
      Map<String, String> translations = new HashMap<>();
      JsonNode nameNode = jsonNode.path("name");
      nameNode
        .fieldNames()
        .forEachRemaining(lang -> {
          String name = nameNode.path(lang).asText();
          if (!name.isEmpty()) {
            translations.put(lang, nameNode.path(lang).asText());
          }
        });
      I18NString name = translations.isEmpty()
        ? new NonLocalizedString(vehicleParkId.getId())
        : TranslatedString.getI18NString(translations, true, false);
      Geometry geometry = GEOMETRY_PARSER.geometryFromJson(jsonNode.path("location"));

      var stateText = jsonNode.path("status").asText();
      var state = stateMapper(stateText);

      var tags = parseTags(jsonNode);
      var maybeCapacity = Optional.ofNullable(capacity);
      var bicyclePlaces = maybeCapacity
        .map(c -> hasPlaces(capacity.getBicycleSpaces()))
        .orElse(false);
      var carPlaces = maybeCapacity.map(c -> hasPlaces(capacity.getCarSpaces())).orElse(false);
      var wheelChairAccessiblePlaces = maybeCapacity
        .map(c -> hasPlaces(capacity.getWheelchairAccessibleCarSpaces()))
        .orElse(false);
      var openingHoursByDayType = jsonNode.path("openingHours").path("byDayType");
      var openingHoursCalendar = parseOpeningHours(openingHoursByDayType);
      VehicleParkingGroup vehicleParkingGroup = hubForPark.get(vehicleParkId);

      return VehicleParking.builder()
        .id(vehicleParkId)
        .name(name)
        .state(state)
        .coordinate(new WgsCoordinate(geometry.getCentroid()))
        .capacity(capacity)
        .bicyclePlaces(bicyclePlaces)
        .carPlaces(carPlaces)
        .wheelchairAccessibleCarPlaces(wheelChairAccessiblePlaces)
        .tags(tags)
        .openingHoursCalendar(openingHoursCalendar)
        .entrance(builder ->
          builder
            .entranceId(new FeedScopedId(feedId, vehicleParkId.getId() + "/entrance"))
            .name(name)
            .coordinate(new WgsCoordinate(geometry.getCentroid()))
            .walkAccessible(true)
            .carAccessible(carPlaces || wheelChairAccessiblePlaces)
        )
        .vehicleParkingGroup(vehicleParkingGroup)
        .build();
    } catch (Exception e) {
      log.warn("Error parsing park {}", vehicleParkId, e);
      return null;
    }
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

  private record DayTypeAndDays(String typeKey, String name, List<DayOfWeek> days) {}

  private static final List<DayTypeAndDays> DAYS_FOR_DAY_TYPES = List.of(
    new DayTypeAndDays(
      "BUSINESS_DAY",
      "Business days",
      List.of(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
      )
    ),
    new DayTypeAndDays("SATURDAY", "Saturday", List.of(DayOfWeek.SATURDAY)),
    new DayTypeAndDays("SUNDAY", "Sunday", List.of(DayOfWeek.SUNDAY))
  );

  private OHCalendar parseOpeningHours(JsonNode openingHoursByDayType) {
    if (zoneId == null) {
      return null;
    }
    var calendarBuilder = openingHoursCalendarService.newBuilder(zoneId);
    for (DayTypeAndDays dayTypeAndDays : DAYS_FOR_DAY_TYPES) {
      String key = dayTypeAndDays.typeKey();
      if (openingHoursByDayType.has(key) && openingHoursByDayType.path(key).has("from")) {
        LocalTime fromTime = convertTimeStringLocalTime(
          openingHoursByDayType.path(key).path("from").asText()
        );
        LocalTime toTime = convertTimeStringLocalTime(
          openingHoursByDayType.path(key).path("until").asText()
        );
        var openingHoursBuilder = calendarBuilder.openingHours(
          dayTypeAndDays.name(),
          fromTime,
          toTime
        );
        for (DayOfWeek day : dayTypeAndDays.days()) {
          openingHoursBuilder.on(day);
        }
        openingHoursBuilder.add();
      }
    }
    return calendarBuilder.build();
  }

  /**
   * Parses a string with format "05" or "05:30" to a {@link LocalTime}.
   * If a park is open until 24h, the end time will be 24 but it should be
   * adjusted to be 23:59 for opening hours.
   */
  private LocalTime convertTimeStringLocalTime(String timeString) {
    int hours = Integer.parseInt(timeString.substring(0, 2));
    int minutes = timeString.length() > 2 ? Integer.parseInt(timeString.substring(3, 5)) : 0;
    return hours == 24 ? LocalTime.MAX : LocalTime.of(hours, minutes);
  }
}
