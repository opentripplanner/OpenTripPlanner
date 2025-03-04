package org.opentripplanner.apis.transmodel.model;

import graphql.schema.GraphQLEnumType;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import org.opentripplanner.framework.doc.DocumentedEnum;
import org.opentripplanner.model.plan.AbsoluteDirection;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alternativelegs.AlternativeLegsFilter;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.booking.BookingMethod;
import org.opentripplanner.transit.service.ArrivalDeparture;

public class EnumTypes {

  public static final GraphQLEnumType ABSOLUTE_DIRECTION = GraphQLEnumType.newEnum()
    .name("AbsoluteDirection")
    .value("north", AbsoluteDirection.NORTH)
    .value("northeast", AbsoluteDirection.NORTHEAST)
    .value("east", AbsoluteDirection.EAST)
    .value("southeast", AbsoluteDirection.SOUTHEAST)
    .value("south", AbsoluteDirection.SOUTH)
    .value("southwest", AbsoluteDirection.SOUTHWEST)
    .value("west", AbsoluteDirection.WEST)
    .value("northwest", AbsoluteDirection.NORTHWEST)
    .build();

  public static final GraphQLEnumType ALTERNATIVE_LEGS_FILTER = GraphQLEnumType.newEnum()
    .name("AlternativeLegsFilter")
    .value("noFilter", AlternativeLegsFilter.NO_FILTER)
    .value("sameAuthority", AlternativeLegsFilter.SAME_AGENCY)
    .value("sameMode", AlternativeLegsFilter.SAME_MODE)
    .value("sameSubmode", AlternativeLegsFilter.SAME_SUBMODE, "Must match both subMode and mode")
    .value("sameLine", AlternativeLegsFilter.SAME_ROUTE)
    .build();

  public static final GraphQLEnumType ARRIVAL_DEPARTURE = GraphQLEnumType.newEnum()
    .name("ArrivalDeparture")
    .value("arrivals", ArrivalDeparture.ARRIVALS, "Only show arrivals")
    .value("departures", ArrivalDeparture.DEPARTURES, "Only show departures")
    .value("both", ArrivalDeparture.BOTH, "Show both arrivals and departures")
    .build();

  public static final GraphQLEnumType BICYCLE_OPTIMISATION_METHOD = GraphQLEnumType.newEnum()
    .name("BicycleOptimisationMethod")
    .value("quick", VehicleRoutingOptimizeType.SHORTEST_DURATION)
    .value("safe", VehicleRoutingOptimizeType.SAFE_STREETS)
    .value("flat", VehicleRoutingOptimizeType.FLAT_STREETS)
    .value("greenways", VehicleRoutingOptimizeType.SAFEST_STREETS)
    .value("triangle", VehicleRoutingOptimizeType.TRIANGLE)
    .build();

  public static final GraphQLEnumType BIKES_ALLOWED = GraphQLEnumType.newEnum()
    .name("BikesAllowed")
    .value("noInformation", BikeAccess.UNKNOWN, "There is no bike information for the trip.")
    .value(
      "allowed",
      BikeAccess.ALLOWED,
      "The vehicle being used on this particular trip can accommodate at least one bicycle."
    )
    .value("notAllowed", BikeAccess.NOT_ALLOWED, "No bicycles are allowed on this trip.")
    .build();

  public static final GraphQLEnumType BOOKING_METHOD = GraphQLEnumType.newEnum()
    .name("BookingMethod")
    .value("callDriver", BookingMethod.CALL_DRIVER)
    .value("callOffice", BookingMethod.CALL_OFFICE)
    .value("online", BookingMethod.ONLINE)
    .value("phoneAtStop", BookingMethod.PHONE_AT_STOP)
    .value("text", BookingMethod.TEXT_MESSAGE)
    .build();

  public static final GraphQLEnumType DIRECTION_TYPE = GraphQLEnumType.newEnum()
    .name("DirectionType")
    .value("unknown", Direction.UNKNOWN)
    .value("outbound", Direction.OUTBOUND)
    .value("inbound", Direction.INBOUND)
    .value("clockwise", Direction.CLOCKWISE)
    .value("anticlockwise", Direction.ANTICLOCKWISE)
    .build();

  public static final GraphQLEnumType FILTER_PLACE_TYPE_ENUM = GraphQLEnumType.newEnum()
    .name("FilterPlaceType")
    .value("quay", TransmodelPlaceType.QUAY, "Quay")
    .value("stopPlace", TransmodelPlaceType.STOP_PLACE, "StopPlace")
    .value("bicycleRent", TransmodelPlaceType.BICYCLE_RENT, "Bicycle rent stations")
    .value("bikePark", TransmodelPlaceType.BIKE_PARK, "Bike parks")
    .value("carPark", TransmodelPlaceType.CAR_PARK, "Car parks")
    .build();

  public static final GraphQLEnumType INPUT_FIELD = GraphQLEnumType.newEnum()
    .name("InputField")
    .value("dateTime", InputField.DATE_TIME)
    .value("from", InputField.FROM_PLACE)
    .value("to", InputField.TO_PLACE)
    .value("intermediatePlace", InputField.INTERMEDIATE_PLACE)
    .build();

  public static final GraphQLEnumType INTERCHANGE_PRIORITY = GraphQLEnumType.newEnum()
    .name("InterchangePriority")
    .value("preferred", TransferPriority.PREFERRED)
    .value("recommended", TransferPriority.RECOMMENDED)
    .value("allowed", TransferPriority.ALLOWED)
    .value("notAllowed", TransferPriority.NOT_ALLOWED)
    .build();

  public static final GraphQLEnumType INTERCHANGE_WEIGHTING = GraphQLEnumType.newEnum()
    .description("Deprecated. Use STOP_INTERCHANGE_PRIORITY")
    .name("InterchangeWeighting")
    .value("preferredInterchange", 2, "Highest priority interchange.")
    .value("recommendedInterchange", 1, "Second highest priority interchange.")
    .value("interchangeAllowed", 0, "Third highest priority interchange.")
    .value("noInterchange", -1, "Interchange not allowed.")
    .build();

  public static final GraphQLEnumType STOP_INTERCHANGE_PRIORITY = GraphQLEnumType.newEnum()
    .name("StopInterchangePriority")
    .value(
      "preferred",
      StopTransferPriority.PREFERRED,
      "Preferred place to transfer, strongly recommended. NeTEx equivalent is PREFERRED_INTERCHANGE."
    )
    .value(
      "recommended",
      StopTransferPriority.RECOMMENDED,
      "Recommended stop place. NeTEx equivalent is RECOMMENDED_INTERCHANGE."
    )
    .value(
      "allowed",
      StopTransferPriority.ALLOWED,
      "Allow transfers from/to this stop. This is the default. NeTEx equivalent is INTERCHANGE_ALLOWED."
    )
    .value(
      "discouraged",
      StopTransferPriority.DISCOURAGED,
      "Block transfers from/to this stop. In OTP this is not a definitive block," +
      " just a huge penalty is added to the cost function. NeTEx equivalent is NO_INTERCHANGE."
    )
    .build();

  public static final GraphQLEnumType ITINERARY_FILTER_DEBUG_PROFILE = createFromDocumentedEnum(
    "ItineraryFilterDebugProfile",
    List.of(
      map("off", ItineraryFilterDebugProfile.OFF),
      map("listAll", ItineraryFilterDebugProfile.LIST_ALL),
      map("limitToSearchWindow", ItineraryFilterDebugProfile.LIMIT_TO_SEARCH_WINDOW),
      map("limitToNumOfItineraries", ItineraryFilterDebugProfile.LIMIT_TO_NUM_OF_ITINERARIES)
    )
  );

  public static final GraphQLEnumType LEG_MODE = GraphQLEnumType.newEnum()
    .name("Mode")
    .value("air", TransitMode.AIRPLANE)
    .value("bicycle", TraverseMode.BICYCLE)
    .value("bus", TransitMode.BUS)
    .value("cableway", TransitMode.GONDOLA)
    .value("water", TransitMode.FERRY)
    .value("funicular", TransitMode.FUNICULAR)
    .value("lift", TransitMode.GONDOLA)
    .value("rail", TransitMode.RAIL)
    .value("metro", TransitMode.SUBWAY)
    .value("taxi", TransitMode.TAXI)
    .value("tram", TransitMode.TRAM)
    .value("trolleybus", TransitMode.TROLLEYBUS)
    .value("monorail", TransitMode.MONORAIL)
    .value("coach", TransitMode.COACH)
    .value("foot", TraverseMode.WALK)
    .value("car", TraverseMode.CAR)
    .value("scooter", TraverseMode.SCOOTER)
    .build();

  public static final GraphQLEnumType LOCALE = GraphQLEnumType.newEnum()
    .name("Locale")
    .value("no", "no")
    .value("us", "us")
    .build();

  public static final GraphQLEnumType MULTI_MODAL_MODE = GraphQLEnumType.newEnum()
    .name("MultiModalMode")
    .value("parent", "parent", "Multi modal parent stop places without their mono modal children.")
    .value(
      "child",
      "child",
      "Only mono modal children stop places, not their multi modal parent stop"
    )
    .value("all", "all", "Both multiModal parents and their mono modal child stop places.")
    .build();

  public static final GraphQLEnumType OCCUPANCY_STATUS = createFromDocumentedEnum(
    "OccupancyStatus",
    List.of(
      map("noData", OccupancyStatus.NO_DATA_AVAILABLE),
      map("empty", OccupancyStatus.EMPTY),
      map("manySeatsAvailable", OccupancyStatus.MANY_SEATS_AVAILABLE),
      map("fewSeatsAvailable", OccupancyStatus.FEW_SEATS_AVAILABLE),
      map("standingRoomOnly", OccupancyStatus.STANDING_ROOM_ONLY),
      map("crushedStandingRoomOnly", OccupancyStatus.CRUSHED_STANDING_ROOM_ONLY),
      map("full", OccupancyStatus.FULL),
      map("notAcceptingPassengers", OccupancyStatus.NOT_ACCEPTING_PASSENGERS)
    )
  );

  public static final GraphQLEnumType PURCHASE_WHEN = GraphQLEnumType.newEnum()
    .name("PurchaseWhen")
    .value("timeOfTravelOnly", "timeOfTravelOnly")
    .value("dayOfTravelOnly", "dayOfTravelOnly")
    .value("untilPreviousDay", "untilPreviousDay")
    .value("advanceAndDayOfTravel", "advanceAndDayOfTravel")
    .value("other", "other")
    .build();

  public static final GraphQLEnumType REALTIME_STATE = GraphQLEnumType.newEnum()
    .name("RealtimeState")
    .value(
      "scheduled",
      RealTimeState.SCHEDULED,
      "The service journey information comes from the regular time table, i.e. no real-time update has been applied."
    )
    .value(
      "updated",
      RealTimeState.UPDATED,
      "The service journey information has been updated, but the journey pattern stayed the same as the journey pattern of the scheduled service journey."
    )
    .value(
      "canceled",
      RealTimeState.CANCELED,
      "The service journey has been canceled by a real-time update."
    )
    .value(
      "Added",
      RealTimeState.ADDED,
      "The service journey has been added using a real-time update, i.e. the service journey was not present in the regular time table."
    )
    .value(
      "modified",
      RealTimeState.MODIFIED,
      "The service journey information has been updated and resulted in a different journey pattern compared to the journey pattern of the scheduled service journey."
    )
    .build();

  public static final GraphQLEnumType RELATIVE_DIRECTION = GraphQLEnumType.newEnum()
    .name("RelativeDirection")
    .value("depart", RelativeDirection.DEPART)
    .value("hardLeft", RelativeDirection.HARD_LEFT)
    .value("left", RelativeDirection.LEFT)
    .value("slightlyLeft", RelativeDirection.SLIGHTLY_LEFT)
    .value("continue", RelativeDirection.CONTINUE)
    .value("slightlyRight", RelativeDirection.SLIGHTLY_RIGHT)
    .value("right", RelativeDirection.RIGHT)
    .value("hardRight", RelativeDirection.HARD_RIGHT)
    .value("circleClockwise", RelativeDirection.CIRCLE_CLOCKWISE)
    .value("circleCounterclockwise", RelativeDirection.CIRCLE_COUNTERCLOCKWISE)
    .value("elevator", RelativeDirection.ELEVATOR)
    .value("uturnLeft", RelativeDirection.UTURN_LEFT)
    .value("uturnRight", RelativeDirection.UTURN_RIGHT)
    .value("enterStation", RelativeDirection.ENTER_STATION)
    .value("exitStation", RelativeDirection.EXIT_STATION)
    .value("followSigns", RelativeDirection.FOLLOW_SIGNS)
    .build();

  public static final GraphQLEnumType REPORT_TYPE = GraphQLEnumType.newEnum()
    .name("ReportType") //SIRI - ReportTypeEnumeration
    .value("general", "general", "Indicates a general info-message that should not affect trip.")
    .value("incident", "incident", "Indicates an incident that may affect trip.")
    .build();

  public static final GraphQLEnumType ROUTING_ERROR_CODE = GraphQLEnumType.newEnum()
    .name("RoutingErrorCode")
    .value(
      "locationNotFound",
      RoutingErrorCode.LOCATION_NOT_FOUND,
      "The specified location is not close to any streets or transit stops"
    )
    .value(
      "noStopsInRange",
      RoutingErrorCode.NO_STOPS_IN_RANGE,
      "No stops are reachable from the location specified. You can try searching using a different access or egress mode"
    )
    .value(
      "noTransitConnection",
      RoutingErrorCode.NO_TRANSIT_CONNECTION,
      "No transit connection was found between the origin and destination withing the operating day or the next day"
    )
    .value(
      "noTransitConnectionInSearchWindow",
      RoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW,
      "A transit connection was found, but it was outside the search window. Use paging to navigate to a result."
    )
    .value(
      "outsideBounds",
      RoutingErrorCode.OUTSIDE_BOUNDS,
      "The coordinates are outside the bounds of the data currently loaded into the system"
    )
    .value(
      "outsideServicePeriod",
      RoutingErrorCode.OUTSIDE_SERVICE_PERIOD,
      "The date specified is outside the range of data currently loaded into the system"
    )
    .value(
      "walkingBetterThanTransit",
      RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT,
      "The origin and destination are so close to each other, that walking is always better, but no direct mode was specified for the search"
    )
    .build();

  public static final GraphQLEnumType SERVICE_ALTERATION = GraphQLEnumType.newEnum()
    .name("ServiceAlteration")
    .value("cancellation", TripAlteration.CANCELLATION)
    .value("replaced", TripAlteration.REPLACED)
    .value("extraJourney", TripAlteration.EXTRA_JOURNEY)
    .value("planned", TripAlteration.PLANNED)
    .build();

  public static final GraphQLEnumType SEVERITY = GraphQLEnumType.newEnum()
    .name("Severity") //SIRI - SeverityEnumeration
    .value("unknown", "unknown", "Situation has unknown impact on trips.")
    .value("noImpact", "noImpact", "Situation has no impact on trips.")
    .value("verySlight", "verySlight", "Situation has a very slight impact on trips.")
    .value("slight", "slight", "Situation has a slight impact on trips.")
    .value("normal", "normal", "Situation has an impact on trips (default).")
    .value("severe", "severe", "Situation has a severe impact on trips.")
    .value("verySevere", "verySevere", "Situation has a very severe impact on trips.")
    .value("undefined", "undefined", "Severity is undefined.")
    .build();

  public static final GraphQLEnumType STOP_CONDITION_ENUM = GraphQLEnumType.newEnum()
    .name("StopCondition") //SIRI - RoutePointTypeEnumeration
    .value(
      "destination",
      StopCondition.DESTINATION,
      "Situation applies when stop is the destination of the leg."
    )
    .value(
      "startPoint",
      StopCondition.START_POINT,
      "Situation applies when stop is the startpoint of the leg."
    )
    .value(
      "exceptionalStop",
      StopCondition.EXCEPTIONAL_STOP,
      "Situation applies when transfering to another leg at the stop."
    )
    .value(
      "notStopping",
      StopCondition.NOT_STOPPING,
      "Situation applies when passing the stop, without stopping."
    )
    .value(
      "requestStop",
      StopCondition.REQUEST_STOP,
      "Situation applies when at the stop, and the stop requires a request to stop."
    )
    .build();

  public static final GraphQLEnumType STREET_MODE = GraphQLEnumType.newEnum()
    .name("StreetMode")
    .value("foot", StreetMode.WALK, "Walk only")
    .value(
      "bicycle",
      StreetMode.BIKE,
      "Bike only. This can be used as " + "access/egress, but transfers will still be walk only."
    )
    .value(
      "bike_park",
      StreetMode.BIKE_TO_PARK,
      "Bike to a bike parking area, " +
      "then walk the rest of the way. Direct mode and access mode only."
    )
    .value(
      "bike_rental",
      StreetMode.BIKE_RENTAL,
      "Walk to a bike rental point, " +
      "bike to a bike rental drop-off point, and walk the rest of the way. This can include " +
      "bike rental at fixed locations or free-floating services."
    )
    .value(
      "scooter_rental",
      StreetMode.SCOOTER_RENTAL,
      "Walk to a scooter " +
      "rental point, ride a scooter to a scooter rental drop-off point, and walk the " +
      "rest of the way. This can include scooter rental at fixed locations or " +
      "free-floating services."
    )
    .value("car", StreetMode.CAR, "Car only. Direct mode only.")
    .value(
      "car_park",
      StreetMode.CAR_TO_PARK,
      "Start in the car, drive to a " +
      "parking area, and walk the rest of the way. Direct mode and access mode only."
    )
    .value(
      "car_pickup",
      StreetMode.CAR_PICKUP,
      "Walk to a pickup point along " +
      "the road, drive to a drop-off point along the road, and walk the rest of the way. " +
      "This can include various taxi-services or kiss & ride."
    )
    .value(
      "car_rental",
      StreetMode.CAR_RENTAL,
      "Walk to a car rental point along " +
      "the road, drive to a drop-off point along the road, and walk the rest of the way. " +
      "This can include car rentals at fixed locations or free-floating services."
    )
    .value(
      "flexible",
      StreetMode.FLEXIBLE,
      "Walk to an eligible pickup area for " +
      "flexible transportation, ride to an eligible drop-off area and then walk the rest of " +
      "the way."
    )
    .build();

  public static final GraphQLEnumType TRANSPORT_MODE = GraphQLEnumType.newEnum()
    .name("TransportMode")
    .value("air", TransitMode.AIRPLANE)
    .value("bus", TransitMode.BUS)
    .value("cableway", TransitMode.GONDOLA)
    .value("water", TransitMode.FERRY)
    .value("funicular", TransitMode.FUNICULAR)
    .value("lift", TransitMode.GONDOLA)
    .value("rail", TransitMode.RAIL)
    .value("metro", TransitMode.SUBWAY)
    .value("taxi", TransitMode.TAXI)
    .value("tram", TransitMode.TRAM)
    .value("trolleybus", TransitMode.TROLLEYBUS)
    .value("monorail", TransitMode.MONORAIL)
    .value("coach", TransitMode.COACH)
    .value("unknown", "unknown")
    .build();

  public static final GraphQLEnumType TRANSPORT_SUBMODE = createEnum(
    "TransportSubmode",
    TransmodelTransportSubmode.values(),
    TransmodelTransportSubmode::getValue
  );

  public static final GraphQLEnumType VERTEX_TYPE = GraphQLEnumType.newEnum()
    .name("VertexType")
    .value("normal", VertexType.NORMAL)
    .value("transit", VertexType.TRANSIT)
    .value("bikePark", VertexType.VEHICLEPARKING)
    .value("bikeShare", VertexType.VEHICLERENTAL)
    //TODO QL: .value("parkAndRide", VertexType.PARKANDRIDE)
    .build();

  public static final GraphQLEnumType WHEELCHAIR_BOARDING = GraphQLEnumType.newEnum()
    .name("WheelchairBoarding")
    .value(
      "noInformation",
      Accessibility.NO_INFORMATION,
      "There is no accessibility information for the stopPlace/quay."
    )
    .value(
      "notPossible",
      Accessibility.NOT_POSSIBLE,
      "Wheelchair boarding/alighting is not possible at this stop."
    )
    .value(
      "possible",
      Accessibility.POSSIBLE,
      "Boarding wheelchair-accessible serviceJourneys is possible at this stopPlace/quay."
    )
    .build();

  static <T extends Enum<?>> GraphQLEnumType createEnum(
    String name,
    T[] values,
    Function<T, String> mapping
  ) {
    GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum().name(name);
    Arrays.stream(values).forEach(type -> enumBuilder.value(mapping.apply(type), type));
    return enumBuilder.build();
  }

  @SuppressWarnings("unchecked")
  static <E extends Enum<E>> GraphQLEnumType createFromDocumentedEnum(
    String typeName,
    List<DocumentedEnumMapping<E>> mappings
  ) {
    var builder = GraphQLEnumType.newEnum().name(typeName);
    var enObj = mappings.get(0).internal;
    var enumType = (Class<E>) enObj.castToEnum().getClass();
    var mappedValues = EnumSet.noneOf(enumType);

    builder.description(enObj.typeDescription());

    for (var it : mappings) {
      builder.value(it.apiName, it.internal, it.internal.enumValueDescription());
      E enumValue = it.internal.castToEnum();
      if (mappedValues.contains(enumValue)) {
        var existing = mappings
          .stream()
          .filter(e -> e.internal == enumValue)
          .findFirst()
          .orElse(null);
        throw new IllegalStateException("Enum value mapped twice: " + it + " and " + existing);
      }
      mappedValues.add(enumValue);
    }
    var missed = EnumSet.complementOf(mappedValues);

    if (!missed.isEmpty()) {
      throw new IllegalStateException("Mapping is missing for: " + missed);
    }
    return builder.build();
  }

  static <E extends Enum<E>> DocumentedEnumMapping<E> map(
    String apiName,
    DocumentedEnum<E> internal
  ) {
    return new DocumentedEnumMapping<>(apiName, internal);
  }

  private record DocumentedEnumMapping<E extends Enum<E>>(
    String apiName,
    DocumentedEnum<E> internal
  ) {}
}
