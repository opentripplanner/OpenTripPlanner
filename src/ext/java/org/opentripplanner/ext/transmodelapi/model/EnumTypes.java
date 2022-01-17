package org.opentripplanner.ext.transmodelapi.model;

import graphql.schema.GraphQLEnumType;
import java.util.Arrays;
import java.util.function.Function;
import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.BookingMethod;
import org.opentripplanner.model.Direction;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripAlteration;
import org.opentripplanner.model.plan.AbsoluteDirection;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.routing.trippattern.RealTimeState;

public class EnumTypes {
    public static GraphQLEnumType WHEELCHAIR_BOARDING = GraphQLEnumType.newEnum()
            .name("WheelchairBoarding")
            .value("noInformation", 0, "There is no accessibility information for the stopPlace/quay.")
            .value("possible", 1, "Boarding wheelchair-accessible serviceJourneys is possible at this stopPlace/quay.")
            .value("notPossible", 2, "Wheelchair boarding/alighting is not possible at this stop.")
            .build();

    public static GraphQLEnumType INTERCHANGE_WEIGHTING = GraphQLEnumType.newEnum()
            .name("InterchangeWeighting")
            .value("preferredInterchange", 2, "Highest priority interchange.")
            .value("recommendedInterchange", 1, "Second highest priority interchange.")
            .value("interchangeAllowed",0, "Third highest priority interchange.")
            .value("noInterchange", -1, "Interchange not allowed.")
            .build();

    public static GraphQLEnumType BIKES_ALLOWED = GraphQLEnumType.newEnum()
            .name("BikesAllowed")
            .value("noInformation", BikeAccess.UNKNOWN, "There is no bike information for the trip.")
            .value("allowed", BikeAccess.ALLOWED, "The vehicle being used on this particular trip can accommodate at least one bicycle.")
            .value("notAllowed", BikeAccess.NOT_ALLOWED, "No bicycles are allowed on this trip.")
            .build();

    public static GraphQLEnumType REPORT_TYPE = GraphQLEnumType.newEnum()
            .name("ReportType") //SIRI - ReportTypeEnumeration
            .value("general", "general", "Indicates a general info-message that should not affect trip.")
            .value("incident", "incident", "Indicates an incident that may affect trip.")
            .build();

    public static GraphQLEnumType SEVERITY = GraphQLEnumType.newEnum()
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

    public static GraphQLEnumType stopConditionEnum = GraphQLEnumType.newEnum()
            .name("StopCondition") //SIRI - RoutePointTypeEnumeration
            .value("destination", StopCondition.DESTINATION, "Situation applies when stop is the destination of the leg.")
            .value("startPoint", StopCondition.START_POINT, "Situation applies when stop is the startpoint of the leg.")
            .value("exceptionalStop", StopCondition.EXCEPTIONAL_STOP, "Situation applies when transfering to another leg at the stop.")
            .value("notStopping", StopCondition.NOT_STOPPING, "Situation applies when passing the stop, without stopping.")
            .value("requestStop", StopCondition.REQUEST_STOP, "Situation applies when at the stop, and the stop requires a request to stop.")
            .build();


    public static GraphQLEnumType REALTIME_STATE = GraphQLEnumType.newEnum()
            .name("RealtimeState")
            .value("scheduled", RealTimeState.SCHEDULED, "The service journey information comes from the regular time table, i.e. no real-time update has been applied.")
            .value("updated", RealTimeState.UPDATED, "The service journey information has been updated, but the journey pattern stayed the same as the journey pattern of the scheduled service journey.")
            .value("canceled", RealTimeState.CANCELED, "The service journey has been canceled by a real-time update.")
            .value("Added", RealTimeState.ADDED, "The service journey has been added using a real-time update, i.e. the service journey was not present in the regular time table.")
            .value("modified", RealTimeState.MODIFIED, "The service journey information has been updated and resulted in a different journey pattern compared to the journey pattern of the scheduled service journey.")
            .build();

    public static GraphQLEnumType VERTEX_TYPE = GraphQLEnumType.newEnum()
            .name("VertexType")
            .value("normal", VertexType.NORMAL)
            .value("transit", VertexType.TRANSIT)
            .value("bikePark", VertexType.VEHICLEPARKING)
            .value("bikeShare", VertexType.VEHICLERENTAL)
            //TODO QL: .value("parkAndRide", VertexType.PARKANDRIDE)
            .build();

    public static GraphQLEnumType INTERCHANGE_PRIORITY = GraphQLEnumType.newEnum()
            .name("InterchangePriority")
            .value("preferred", TransferPriority.PREFERRED)
            .value("recommended", TransferPriority.RECOMMENDED)
            .value("allowed", TransferPriority.ALLOWED)
            .value("notAllowed", TransferPriority.NOT_ALLOWED)
            .build();

    public static GraphQLEnumType STREET_MODE = GraphQLEnumType.newEnum()
        .name("StreetMode")
        .value("foot", StreetMode.WALK, "Walk only")
        .value("bicycle", StreetMode.BIKE, "Bike only. This can be used as "
            + "access/egress, but transfers will still be walk only.")
        .value("bike_park", StreetMode.BIKE_TO_PARK, "Bike to a bike parking area, "
            + "then walk the rest of the way. Direct mode and access mode only.")
        .value("bike_rental", StreetMode.BIKE_RENTAL, "Walk to a bike rental point, "
            + "bike to a bike rental drop-off point, and walk the rest of the way. This can include "
            + "bike rental at fixed locations or free-floating services.")
        .value("scooter_rental", StreetMode.SCOOTER_RENTAL, "Walk to a scooter "
                + "rental point, ride a scooter to a scooter rental drop-off point, and walk the "
                + "rest of the way. This can include scooter rental at fixed locations or "
                + "free-floating services.")
        .value("car", StreetMode.CAR, "Car only. Direct mode only.")
        .value("car_park", StreetMode.CAR_TO_PARK, "Start in the car, drive to a "
            + "parking area, and walk the rest of the way. Direct mode and access mode only.")
        .value("car_pickup", StreetMode.CAR_PICKUP, "Walk to a pickup point along "
            + "the road, drive to a drop-off point along the road, and walk the rest of the way. "
            + "This can include various taxi-services or kiss & ride.")
        .value("flexible", StreetMode.FLEXIBLE, "Walk to an eligible pickup area for "
            + "flexible transportation, ride to an eligible drop-off area and then walk the rest of "
            + "the way.")
        .build();

    public static GraphQLEnumType MODE = GraphQLEnumType.newEnum()
            .name("Mode")
            .value("air", TraverseMode.AIRPLANE)
            .value("bicycle", TraverseMode.BICYCLE)
            .value("bus", TraverseMode.BUS)
            .value("cableway", TraverseMode.CABLE_CAR)
            .value("water", TraverseMode.FERRY)
            .value("funicular", TraverseMode.FUNICULAR)
            .value("lift", TraverseMode.GONDOLA)
            .value("rail", TraverseMode.RAIL)
            .value("metro", TraverseMode.SUBWAY)
            .value("tram", TraverseMode.TRAM)
            .value("coach", TraverseMode.BUS).description("NOT IMPLEMENTED")
            .value("transit", TraverseMode.TRANSIT, "Any for of public transportation")
            .value("foot", TraverseMode.WALK)
            .value("car", TraverseMode.CAR)
            .value("scooter", TraverseMode.SCOOTER)
            // TODO OTP2 - Car park no added
            // .value("car_park", TraverseMode.CAR_PARK, "Combine with foot and transit for park and ride.")
            // .value("car_dropoff", TraverseMode.CAR_DROPOFF, "Combine with foot and transit for kiss and ride.")
            // .value("car_pickup", TraverseMode.CAR_PICKUP, "Combine with foot and transit for ride and kiss.")
            .build();

    public static GraphQLEnumType TRANSPORT_MODE = GraphQLEnumType.newEnum()
            .name("TransportMode")
            .value("air", TransitMode.AIRPLANE)
            .value("bus", TransitMode.BUS)
            .value("cableway", TransitMode.CABLE_CAR)
            .value("water", TransitMode.FERRY)
            .value("funicular", TransitMode.FUNICULAR)
            .value("lift", TransitMode.GONDOLA)
            .value("rail", TransitMode.RAIL)
            .value("metro", TransitMode.SUBWAY)
            .value("tram", TransitMode.TRAM)
            .value("trolleybus", TransitMode.TROLLEYBUS)
            .value("monorail", TransitMode.MONORAIL)
            .value("coach", TransitMode.COACH)
            .value("unknown", "unknown")
            .build();

    public static GraphQLEnumType RELATIVE_DIRECTION = GraphQLEnumType.newEnum()
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
            .build();

    public static GraphQLEnumType ABSOLUTE_DIRECTION = GraphQLEnumType.newEnum()
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

    public static GraphQLEnumType LOCALE = GraphQLEnumType.newEnum()
            .name("Locale")
            .value("no", "no")
            .value("us", "us")
            .build();

    public static GraphQLEnumType MULTI_MODAL_MODE = GraphQLEnumType.newEnum()
            .name("MultiModalMode")
            .value("parent", "parent", "Multi modal parent stop places without their mono modal children.")
            .value("child", "child", "Only mono modal children stop places, not their multi modal parent stop")
            .value("all", "all", "Both multiModal parents and their mono modal child stop places.")
            .build();

    /*
    public static GraphQLEnumType stopTypeEnum = GraphQLEnumType.newEnum()
            .name("StopType")
            .value("regular", Stop.stopTypeEnumeration.REGULAR)
            .value("flexible_area", Stop.stopTypeEnumeration.FLEXIBLE_AREA)
            .build();

*/
    public static GraphQLEnumType TRANSPORT_SUBMODE = createEnum("TransportSubmode", TransmodelTransportSubmode.values(), (t -> t.getValue()));
    /*

    public static GraphQLEnumType flexibleLineTypeEnum = TransmodelIndexGraphQLSchema.createEnum("FlexibleLineType", Route.FlexibleRouteTypeEnum.values(), (t -> t.name()));

    public static GraphQLEnumType flexibleServiceTypeEnum = TransmodelIndexGraphQLSchema.createEnum("FlexibleServiceType", Trip.FlexibleTripTypeEnum.values(), (t -> t.name()));

    public static GraphQLEnumType purchaseMomentEnum = TransmodelIndexGraphQLSchema.createEnum("PurchaseMoment", BookingArrangement.PurchaseMomentEnum.values(), (t -> t.name()));

    public static GraphQLEnumType purchaseWhenEnum = TransmodelIndexGraphQLSchema.createEnum("PurchaseWhen", BookingArrangement.PurchaseWhenEnum.values(), (t -> t.name()));

    public static GraphQLEnumType bookingAccessEnum = TransmodelIndexGraphQLSchema.createEnum("BookingAccess", BookingArrangement.BookingAccessEnum.values(), (t -> t.name()));

    public static GraphQLEnumType bookingMethodEnum = TransmodelIndexGraphQLSchema.createEnum("BookingMethod", BookingArrangement.BookingMethodEnum.values(), (t -> t.name()));
*/

    public static GraphQLEnumType filterPlaceTypeEnum = GraphQLEnumType.newEnum()
            .name("FilterPlaceType")
            .value("quay", TransmodelPlaceType.QUAY, "Quay")
            .value("stopPlace", TransmodelPlaceType.STOP_PLACE, "StopPlace")
            .value("bicycleRent", TransmodelPlaceType.BICYCLE_RENT, "Bicycle rent stations")
            .value("bikePark",TransmodelPlaceType.BIKE_PARK, "Bike parks")
            .value("carPark", TransmodelPlaceType.CAR_PARK, "Car parks")
            .build();


    public static GraphQLEnumType BICYCLE_OPTIMISATION_METHOD = GraphQLEnumType.newEnum()
            .name("BicycleOptimisationMethod")
            .value("quick", BicycleOptimizeType.QUICK)
            .value("safe", BicycleOptimizeType.SAFE)
            .value("flat", BicycleOptimizeType.FLAT)
            .value("greenways", BicycleOptimizeType.GREENWAYS)
            .value("triangle", BicycleOptimizeType.TRIANGLE)
            .value("transfers", BicycleOptimizeType.TRANSFERS)
            .build();

    public static GraphQLEnumType DIRECTION_TYPE = GraphQLEnumType.newEnum()
            .name("DirectionType")
            .value("unknown", Direction.UNKNOWN)
            .value("outbound", Direction.OUTBOUND)
            .value("inbound", Direction.INBOUND)
            .value("clockwise", Direction.CLOCKWISE)
            .value("anticlockwise", Direction.ANTICLOCKWISE)
            .build();

    public static GraphQLEnumType BOOKING_METHOD = GraphQLEnumType.newEnum()
        .name("BookingMethod")
        .value("callDriver", BookingMethod.CALL_DRIVER)
        .value("callOffice", BookingMethod.CALL_OFFICE)
        .value("online", BookingMethod.ONLINE)
        .value("phoneAtStop", BookingMethod.PHONE_AT_STOP)
        .value("text", BookingMethod.TEXT_MESSAGE)
        .build();


    public static GraphQLEnumType SERVICE_ALTERATION = GraphQLEnumType.newEnum()
        .name("ServiceAlteration")
        .value("cancellation", TripAlteration.CANCELLATION)
        .value("replaced", TripAlteration.REPLACED)
        .value("extraJourney", TripAlteration.EXTRA_JOURNEY)
        .value("planned", TripAlteration.PLANNED)
        .build();

    public static GraphQLEnumType ARRIVAL_DEPARTURE = GraphQLEnumType.newEnum()
        .name("ArrivalDeparture")
        .value("arrivals", ArrivalDeparture.ARRIVALS, "Only show arrivals")
        .value("departures", ArrivalDeparture.DEPARTURES, "Only show departures")
        .value("both", ArrivalDeparture.BOTH, "Show both arrivals and departures")
        .build();

    public static GraphQLEnumType ROUTING_ERROR_CODE = GraphQLEnumType.newEnum()
        .name("RoutingErrorCode")
        .value("noTransitConnection", RoutingErrorCode.NO_TRANSIT_CONNECTION, "No transit connection was found between the origin and destination withing the operating day or the next day")
        .value("noTransitConnectionInSearchWindow", RoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW, "Transit connection was found, but it was outside the search window, see metadata for the next search window")
        .value("outsideServicePeriod", RoutingErrorCode.OUTSIDE_SERVICE_PERIOD, "The date specified is outside the range of data currently loaded into the system")
        .value("outsideBounds", RoutingErrorCode.OUTSIDE_BOUNDS, "The coordinates are outside the bounds of the data currently loaded into the system")
        .value("locationNotFound", RoutingErrorCode.LOCATION_NOT_FOUND, "The specified location is not close to any streets or transit stops")
        .value("noStopsInRange", RoutingErrorCode.NO_STOPS_IN_RANGE, "No stops are reachable from the location specified. You can try searching using a different access or egress mode")
        .value("walkingBetterThanTransit", RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT, "The origin and destination are so close to each other, that walking is always better, but no direct mode was specified for the search")
        .value("systemError", RoutingErrorCode.SYSTEM_ERROR, "An unknown error happened during the search. The details have been logged to the server logs")
        .build();

    public static GraphQLEnumType INPUT_FIELD = GraphQLEnumType.newEnum()
        .name("InputField")
        .value("dateTime", InputField.DATE_TIME)
        .value("from", InputField.FROM_PLACE)
        .value("to", InputField.TO_PLACE)
        .build();

    public static Object enumToString(GraphQLEnumType type, Enum<?> value) {
        return type.serialize(value);
    }



    private static <T extends Enum> GraphQLEnumType createEnum(String name, T[] values, Function<T, String> mapping) {
        GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum().name(name);
        Arrays.stream(values).forEach(type -> enumBuilder.value(mapping.apply(type), type));
        return enumBuilder.build();
    }
}
