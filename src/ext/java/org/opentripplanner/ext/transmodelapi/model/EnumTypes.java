package org.opentripplanner.ext.transmodelapi.model;

import graphql.schema.GraphQLEnumType;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.plan.AbsoluteDirection;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.trippattern.RealTimeState;

import java.util.Arrays;
import java.util.function.Function;

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
            .value("noInformation", 0, "There is no bike information for the trip.")
            .value("allowed", 1, "The vehicle being used on this particular trip can accommodate at least one bicycle.")
            .value("notAllowed", 2, "No bicycles are allowed on this trip.")
            .build();

    public static GraphQLEnumType REPORT_TYPE = GraphQLEnumType.newEnum()
            .name("ReportType") //SIRI - ReportTypeEnumeration
            .value("general", "general", "Indicates a general info-message that should not affect trip.")
            .value("incident", "incident", "Indicates an incident that may affect trip.")
            .build();

    public static GraphQLEnumType SEVERITY = GraphQLEnumType.newEnum()
            .name("Severity") //SIRI - SeverityEnumeration
            .value("noImpact", "noImpact", "Situation has no impact on trips.")
            .value("slight", "slight", "Situation has a small impact on trips.")
            .value("normal", "normal", "Situation has an impact on trips (default).")
            .value("severe", "severe", "Situation has a severe impact on trips.")
            .build();

    /* TODO OTP2 - StopCondition does not exist yet
    public static GraphQLEnumType stopConditionEnum = GraphQLEnumType.newEnum()
            .name("StopCondition") //SIRI - RoutePointTypeEnumeration
            .value("destination", StopCondition.DESTINATION, "Situation applies when stop is the destination of the leg.")
            .value("startPoint", StopCondition.START_POINT, "Situation applies when stop is the startpoint of the leg.")
            .value("exceptionalStop", StopCondition.EXCEPTIONAL_STOP, "Situation applies when transfering to another leg at the stop.")
            .value("notStopping", StopCondition.NOT_STOPPING, "Situation applies when passing the stop, without stopping.")
            .value("requestStop", StopCondition.REQUEST_STOP, "Situation applies when at the stop, and the stop requires a request to stop.")
            .build();
     */

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
            .value("bikePark", VertexType.BIKEPARK)
            .value("bikeShare", VertexType.BIKESHARE)
            //TODO QL: .value("parkAndRide", VertexType.PARKANDRIDE)
            .build();

    /* TODO QL
    public static GraphQLEnumType serviceAlterationEnum = GraphQLEnumType.newEnum()
            .name("ServiceAlteration")
            .value("planned", Trip.ServiceAlteration.planned)
            .value("cancellation", Trip.ServiceAlteration.cancellation)
            .value("extraJourney", Trip.ServiceAlteration.extraJourney)
            .build();
    */

    public static GraphQLEnumType STREET_MODE = GraphQLEnumType.newEnum()
        .name("StreetMode")
        .value("foot", StreetMode.WALK)
        .value("bicycle", StreetMode.BIKE)
        .value("bike_park", StreetMode.BIKE_TO_PARK)
        .value("bike_rental", StreetMode.BIKE_RENTAL)
        .value("car", StreetMode.CAR)
        .value("car_park", StreetMode.CAR_TO_PARK)
        .value("car_pickup", StreetMode.CAR_PICKUP)
        .value("car_rental", StreetMode.CAR_RENTAL)
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
            .value("coach", TransitMode.BUS).description("NOT IMPLEMENTED")
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
/*
    public static GraphQLEnumType filterPlaceTypeEnum = GraphQLEnumType.newEnum()
            .name("FilterPlaceType")
            .value("quay", TransmodelPlaceType.QUAY, "Quay")
            .value("stopPlace", TransmodelPlaceType.STOP_PLACE, "StopPlace")
            .value("bicycleRent", TransmodelPlaceType.BICYCLE_RENT, "Bicycle rent stations")
            .value("bikePark",TransmodelPlaceType.BIKE_PARK, "Bike parks")
            .value("carPark", TransmodelPlaceType.CAR_PARK, "Car parks")
            .build();
*/

    public static GraphQLEnumType OPTIMISATION_METHOD = GraphQLEnumType.newEnum()
            .name("OptimisationMethod")
            .value("quick", OptimizeType.QUICK)
            .value("safe", OptimizeType.SAFE)
            .value("flat", OptimizeType.FLAT)
            .value("greenways", OptimizeType.GREENWAYS)
            .value("triangle", OptimizeType.TRIANGLE)
            .value("transfers", OptimizeType.TRANSFERS)
            .build();

    public static GraphQLEnumType DIRECTION_TYPE = GraphQLEnumType.newEnum()
            .name("DirectionType")
            .value("unknown",-1)
            .value("outbound", 0)
            .value("inbound", 1)
            .value("clockwise", 2)
            .value("anticlockwise", 3)
            .build();

    public static Object enumToString(GraphQLEnumType type, Enum<?> value) {
        return type.getCoercing().serialize(value);
    }



    private static <T extends Enum> GraphQLEnumType createEnum(String name, T[] values, Function<T, String> mapping) {
        GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum().name(name);
        Arrays.stream(values).forEach(type -> enumBuilder.value(mapping.apply(type), type));
        return enumBuilder.build();
    }
}
