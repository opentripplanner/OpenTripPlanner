package org.opentripplanner.ext.transmodelapi;

import graphql.Scalars;
import graphql.relay.DefaultConnection;
import graphql.relay.DefaultPageInfo;
import graphql.relay.Relay;
import graphql.relay.SimpleListConnection;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.transmodelapi.mapping.TransmodelMappingUtil;
import org.opentripplanner.ext.transmodelapi.model.MonoOrMultiModalStation;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.model.TransmodelPlaceType;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.model.TripTimeShortHelper;
import org.opentripplanner.ext.transmodelapi.model.scalars.DateScalarFactory;
import org.opentripplanner.ext.transmodelapi.model.scalars.DateTimeScalarFactory;
import org.opentripplanner.ext.transmodelapi.model.scalars.GeoJSONCoordinatesScalar;
import org.opentripplanner.ext.transmodelapi.model.scalars.LocalTimeScalarFactory;
import org.opentripplanner.ext.transmodelapi.model.scalars.TimeScalarFactory;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopCollection;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.AbsoluteDirection;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.model.routing.TripSearchMetadata;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.ResourceBundleSingleton;
import org.opentripplanner.util.TranslatedString;
import org.opentripplanner.util.model.EncodedPolylineBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.opentripplanner.model.StopPattern.PICKDROP_COORDINATE_WITH_DRIVER;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

/**
 * Schema definition for the Transmodel GraphQL API.
 * <p>
 * Currently a simplified version of the IndexGraphQLSchema, with gtfs terminology replaced with corresponding terms from Transmodel.
 */
public class TransmodelIndexGraphQLSchema {
    private static final Logger LOG = LoggerFactory.getLogger(TransmodelIndexGraphQLSchema.class);

    private static GraphQLEnumType wheelchairBoardingEnum = GraphQLEnumType.newEnum()
            .name("WheelchairBoarding")
            .value("noInformation", 0, "There is no accessibility information for the stopPlace/quay.")
            .value("possible", 1, "Boarding wheelchair-accessible serviceJourneys is possible at this stopPlace/quay.")
            .value("notPossible", 2, "Wheelchair boarding/alighting is not possible at this stop.")
            .build();

    private static GraphQLEnumType interchangeWeightingEnum = GraphQLEnumType.newEnum()
            .name("InterchangeWeighting")
            .value("preferredInterchange", 2, "Highest priority interchange.")
            .value("recommendedInterchange", 1, "Second highest priority interchange.")
            .value("interchangeAllowed",0, "Third highest priority interchange.")
            .value("noInterchange", -1, "Interchange not allowed.")
            .build();

    private static GraphQLEnumType bikesAllowedEnum = GraphQLEnumType.newEnum()
            .name("BikesAllowed")
            .value("noInformation", 0, "There is no bike information for the trip.")
            .value("allowed", 1, "The vehicle being used on this particular trip can accommodate at least one bicycle.")
            .value("notAllowed", 2, "No bicycles are allowed on this trip.")
            .build();

    private static GraphQLEnumType reportTypeEnum = GraphQLEnumType.newEnum()
            .name("ReportType") //SIRI - ReportTypeEnumeration
            .value("general", "general", "Indicates a general info-message that should not affect trip.")
            .value("incident", "incident", "Indicates an incident that may affect trip.")
            .build();

    private static GraphQLEnumType severityEnum = GraphQLEnumType.newEnum()
            .name("Severity") //SIRI - SeverityEnumeration
            .value("noImpact", "noImpact", "Situation has no impact on trips.")
            .value("slight", "slight", "Situation has a small impact on trips.")
            .value("normal", "normal", "Situation has an impact on trips (default).")
            .value("severe", "severe", "Situation has a severe impact on trips.")
            .build();

    /* TODO OTP2 - StopCondition does not exist yet
    private static GraphQLEnumType stopConditionEnum = GraphQLEnumType.newEnum()
            .name("StopCondition") //SIRI - RoutePointTypeEnumeration
            .value("destination", StopCondition.DESTINATION, "Situation applies when stop is the destination of the leg.")
            .value("startPoint", StopCondition.START_POINT, "Situation applies when stop is the startpoint of the leg.")
            .value("exceptionalStop", StopCondition.EXCEPTIONAL_STOP, "Situation applies when transfering to another leg at the stop.")
            .value("notStopping", StopCondition.NOT_STOPPING, "Situation applies when passing the stop, without stopping.")
            .value("requestStop", StopCondition.REQUEST_STOP, "Situation applies when at the stop, and the stop requires a request to stop.")
            .build();
     */

    private static GraphQLEnumType realtimeStateEnum = GraphQLEnumType.newEnum()
            .name("RealtimeState")
            .value("scheduled", RealTimeState.SCHEDULED, "The service journey information comes from the regular time table, i.e. no real-time update has been applied.")
            .value("updated", RealTimeState.UPDATED, "The service journey information has been updated, but the journey pattern stayed the same as the journey pattern of the scheduled service journey.")
            .value("canceled", RealTimeState.CANCELED, "The service journey has been canceled by a real-time update.")
            .value("Added", RealTimeState.ADDED, "The service journey has been added using a real-time update, i.e. the service journey was not present in the regular time table.")
            .value("modified", RealTimeState.MODIFIED, "The service journey information has been updated and resulted in a different journey pattern compared to the journey pattern of the scheduled service journey.")
            .build();

    private static GraphQLEnumType vertexTypeEnum = GraphQLEnumType.newEnum()
            .name("VertexType")
            .value("normal", VertexType.NORMAL)
            .value("transit", VertexType.TRANSIT)
            .value("bikePark", VertexType.BIKEPARK)
            .value("bikeShare", VertexType.BIKESHARE)
            //TODO QL: .value("parkAndRide", VertexType.PARKANDRIDE)
            .build();

    /* TODO QL
    private static GraphQLEnumType serviceAlterationEnum = GraphQLEnumType.newEnum()
            .name("ServiceAlteration")
            .value("planned", Trip.ServiceAlteration.planned)
            .value("cancellation", Trip.ServiceAlteration.cancellation)
            .value("extraJourney", Trip.ServiceAlteration.extraJourney)
            .build();
    */

    private static GraphQLEnumType modeEnum = GraphQLEnumType.newEnum()
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

    private static GraphQLEnumType transportModeEnum = GraphQLEnumType.newEnum()
            .name("TransportMode")
            .value("air", TraverseMode.AIRPLANE)
            .value("bus", TraverseMode.BUS)
            .value("cableway", TraverseMode.CABLE_CAR)
            .value("water", TraverseMode.FERRY)
            .value("funicular", TraverseMode.FUNICULAR)
            .value("lift", TraverseMode.GONDOLA)
            .value("rail", TraverseMode.RAIL)
            .value("metro", TraverseMode.SUBWAY)
            .value("tram", TraverseMode.TRAM)
            .value("coach", TraverseMode.BUS).description("NOT IMPLEMENTED")
            .value("unknown", "unknown")
            .build();

    private static GraphQLEnumType relativeDirectionEnum = GraphQLEnumType.newEnum()
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

    private static GraphQLEnumType absoluteDirectionEnum = GraphQLEnumType.newEnum()
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

    private static GraphQLEnumType localeEnum = GraphQLEnumType.newEnum()
            .name("Locale")
            .value("no", "no")
            .value("us", "us")
            .build();

    private static GraphQLEnumType multiModalModeEnum = GraphQLEnumType.newEnum()
             .name("MultiModalMode")
             .value("parent", "parent", "Multi modal parent stop places without their mono modal children.")
             .value("child", "child", "Only mono modal children stop places, not their multi modal parent stop")
             .value("all", "all", "Both multiModal parents and their mono modal child stop places.")
             .build();

    /*
    private static GraphQLEnumType stopTypeEnum = GraphQLEnumType.newEnum()
            .name("StopType")
            .value("regular", Stop.stopTypeEnumeration.REGULAR)
            .value("flexible_area", Stop.stopTypeEnumeration.FLEXIBLE_AREA)
            .build();

*/
    private static GraphQLEnumType transportSubmode = TransmodelIndexGraphQLSchema.createEnum("TransportSubmode", TransmodelTransportSubmode.values(), (t -> t.getValue()));
    /*

    private static GraphQLEnumType flexibleLineTypeEnum = TransmodelIndexGraphQLSchema.createEnum("FlexibleLineType", Route.FlexibleRouteTypeEnum.values(), (t -> t.name()));

    private static GraphQLEnumType flexibleServiceTypeEnum = TransmodelIndexGraphQLSchema.createEnum("FlexibleServiceType", Trip.FlexibleTripTypeEnum.values(), (t -> t.name()));

    private static GraphQLEnumType purchaseMomentEnum = TransmodelIndexGraphQLSchema.createEnum("PurchaseMoment", BookingArrangement.PurchaseMomentEnum.values(), (t -> t.name()));

    private static GraphQLEnumType purchaseWhenEnum = TransmodelIndexGraphQLSchema.createEnum("PurchaseWhen", BookingArrangement.PurchaseWhenEnum.values(), (t -> t.name()));

    private static GraphQLEnumType bookingAccessEnum = TransmodelIndexGraphQLSchema.createEnum("BookingAccess", BookingArrangement.BookingAccessEnum.values(), (t -> t.name()));

    private static GraphQLEnumType bookingMethodEnum = TransmodelIndexGraphQLSchema.createEnum("BookingMethod", BookingArrangement.BookingMethodEnum.values(), (t -> t.name()));
*/

    private static <T extends Enum> GraphQLEnumType createEnum(String name, T[] values, Function<T, String> mapping) {
        GraphQLEnumType.Builder enumBuilder = GraphQLEnumType.newEnum().name(name);
        Arrays.stream(values).forEach(type -> enumBuilder.value(mapping.apply(type), type));
        return enumBuilder.build();
    }


    private static GraphQLEnumType filterPlaceTypeEnum = GraphQLEnumType.newEnum()
            .name("FilterPlaceType")
            .value("quay", TransmodelPlaceType.QUAY, "Quay")
            .value("stopPlace", TransmodelPlaceType.STOP_PLACE, "StopPlace")
            .value("bicycleRent", TransmodelPlaceType.BICYCLE_RENT, "Bicycle rent stations")
            .value("bikePark",TransmodelPlaceType.BIKE_PARK, "Bike parks")
            .value("carPark", TransmodelPlaceType.CAR_PARK, "Car parks")
            .build();

    private static GraphQLEnumType optimisationMethodEnum = GraphQLEnumType.newEnum()
            .name("OptimisationMethod")
            .value("quick", OptimizeType.QUICK)
            .value("safe", OptimizeType.SAFE)
            .value("flat", OptimizeType.FLAT)
            .value("greenways", OptimizeType.GREENWAYS)
            .value("triangle", OptimizeType.TRIANGLE)
            .value("transfers", OptimizeType.TRANSFERS)
            .build();

    private static GraphQLEnumType directionTypeEnum = GraphQLEnumType.newEnum()
            .name("DirectionType")
            .value("unknown",-1)
            .value("outbound", 0)
            .value("inbound", 1)
            .value("clockwise", 2)
            .value("anticlockwise", 3)
            .build();

    private GraphQLOutputType noticeType = new GraphQLTypeReference("Notice");

    private GraphQLOutputType organisationType = new GraphQLTypeReference("Organisation");

    private GraphQLOutputType authorityType = new GraphQLTypeReference("Authority");

    private GraphQLOutputType operatorType = new GraphQLTypeReference("Operator");

    private GraphQLOutputType ptSituationElementType = new GraphQLTypeReference("PtSituationElement");

    private GraphQLOutputType bikeRentalStationType = new GraphQLTypeReference("BikeRentalStation");

    private GraphQLOutputType bikeParkType = new GraphQLTypeReference("BikePark");

    // private GraphQLOutputType carParkType = new GraphQLTypeReference("CarPark");

    private GraphQLOutputType journeyPatternType = new GraphQLTypeReference("JourneyPattern");

    private GraphQLOutputType lineType = new GraphQLTypeReference("Line");

    private GraphQLOutputType tariffZoneType = new GraphQLTypeReference("TariffZone");

    private GraphQLOutputType timetabledPassingTimeType = new GraphQLTypeReference("TimetabledPassingTime");

    private GraphQLOutputType estimatedCallType = new GraphQLTypeReference("EstimatedCall");

    private GraphQLOutputType stopPlaceType = new GraphQLTypeReference("StopPlace");

    private GraphQLOutputType quayType = new GraphQLTypeReference("Quay");

    private GraphQLOutputType serviceJourneyType = new GraphQLTypeReference("ServiceJourney");

    private GraphQLOutputType quayAtDistance = new GraphQLTypeReference("QuayAtDistance");

    private GraphQLOutputType multilingualStringType = new GraphQLTypeReference("TranslatedString");

    private GraphQLOutputType placeAtDistanceType = new GraphQLTypeReference("PlaceAtDistance");

    private GraphQLOutputType bookingArrangementType = new GraphQLTypeReference("BookingArrangement");

    private GraphQLOutputType contactType = new GraphQLTypeReference("Contact");

    private GraphQLInputObjectType locationType;

    private GraphQLObjectType keyValueType;

    //private GraphQLObjectType brandingType;

    private GraphQLObjectType linkGeometryType;

    private GraphQLObjectType queryType;

    private GraphQLOutputType routingParametersType = new GraphQLTypeReference("RoutingParameters");

    private GraphQLOutputType tripType = new GraphQLTypeReference("Trip");

    private GraphQLOutputType tripMetadataType = new GraphQLTypeReference("TripMetadata");

    private GraphQLOutputType interchangeType = new GraphQLTypeReference("interchange");

    private TransmodelMappingUtil mappingUtil;

    private TripTimeShortHelper tripTimeShortHelper;
    private String fixedAgencyId;

    private GraphQLScalarType dateTimeScalar;
    private GraphQLObjectType timeType;
    private GraphQLScalarType dateScalar;
    private GraphQLObjectType destinationDisplayType;
    private GraphQLScalarType localTimeScalar;

    public GraphQLSchema indexSchema;

    private GraphIndex index;

    private Relay relay = new Relay();


    private GraphQLInterfaceType placeInterface = GraphQLInterfaceType.newInterface()
            .name("PlaceInterface")
            .description("Interface for places, i.e. quays, stop places, parks")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("id")
                    .type(new GraphQLNonNull(Scalars.GraphQLID))
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("latitude")
                    .type(Scalars.GraphQLFloat)
                    .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("longitude")
                    .type(Scalars.GraphQLFloat)
                    .build())
            .typeResolver(typeResolutionEnvironment -> {
                    Object o=typeResolutionEnvironment.getObject();

                    // TODO OTP2 - Add support for Station, osv

                    if (o instanceof Stop) {
                        return (GraphQLObjectType) quayType;
                    }
                    if (o instanceof BikeRentalStation) {
                        return (GraphQLObjectType) bikeRentalStationType;
                    }
                    if (o instanceof BikePark) {
                        return (GraphQLObjectType) bikeParkType;
                    }
                    //if (o instanceof CarPark) {
                    //    return (GraphQLObjectType) carParkType;
                    //}
                    return null;
            }
            ).build();

    private Agency getAgency(String agencyId) {
        //xxx what if there are duplicate agency ids?
        //now we return the first
        for (Map<String, Agency> feedAgencies : index.agenciesForFeedId.values()) {
            if (feedAgencies.get(agencyId) != null) {
                return feedAgencies.get(agencyId);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public TransmodelIndexGraphQLSchema(Graph graph) {
        index = graph.index;
        RoutingRequest defaultRoutingRequest = getDefaultRoutingRequest();
        String fixedAgencyIdPropValue = System.getProperty("transmodel.graphql.api.agency.id");
        if (!StringUtils.isEmpty(fixedAgencyIdPropValue)) {
            fixedAgencyId = fixedAgencyIdPropValue;
            LOG.info("Starting Transmodel GraphQL Schema with fixed AgencyID:'" + fixedAgencyId +
                    "'. All FeedScopedIds in API will be assumed to belong to this agency.");
        }

        mappingUtil = new TransmodelMappingUtil(fixedAgencyId, index.graph.getTimeZone());
        tripTimeShortHelper = new TripTimeShortHelper(index);
        dateTimeScalar = DateTimeScalarFactory.createMillisecondsSinceEpochAsDateTimeStringScalar(index.graph.getTimeZone());
        timeType = TimeScalarFactory.createSecondsSinceMidnightAsTimeObject();
        dateScalar = DateScalarFactory.createSecondsSinceEpochAsDateStringScalar(index.graph.getTimeZone());
        localTimeScalar = LocalTimeScalarFactory.createLocalTimeScalar();

        GraphQLInputObjectType coordinateInputType = GraphQLInputObjectType.newInputObject()
                .name("InputCoordinates")
                .description("Input type for coordinates in the WGS84 system")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("latitude")
                        .description("The latitude of the place.")
                        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("longitude")
                        .description("The longitude of the place.")
                        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                        .build())
                .build();

        interchangeType = GraphQLObjectType.newObject()
                .name("Interchange")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("staySeated")
                        .description("Time that the trip departs. NOT IMPLEMENTED")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> false)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("guaranteed")
                        .description("Time that the trip departs. NOT IMPLEMENTED")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> false)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("FromLine")
                        .type(lineType)
                        .dataFetcher(environment -> ((Transfer) environment.getSource()).getFromRoute())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("ToLine")
                        .type(lineType)
                        .dataFetcher(environment -> ((Transfer) environment.getSource()).getToRoute())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("FromServiceJourney")
                        .type(serviceJourneyType)
                        .dataFetcher(environment -> ((Transfer) environment.getSource()).getFromTrip())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("ToServiceJourney")
                        .type(serviceJourneyType)
                        .dataFetcher(environment -> ((Transfer) environment.getSource()).getToTrip())
                        .build())
                .build();

        keyValueType = GraphQLObjectType.newObject()
                .name("KeyValue")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("key")
                        .description("Identifier of value.")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> null)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("value")
                        .description("The actual value")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> null)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("typeOfKey")
                        .description("Identifier of type of key")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> null)
                        .build())
                .build();


        /*
        brandingType = GraphQLObjectType.newObject()
                .name("Branding")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(
                                environment -> ((Branding) environment.getSource()).getId().getId())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .description("Full name to be used for branding.")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Branding) environment.getSource()).getName())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("description")
                        .description("Description of branding.")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Branding) environment.getSource()).getDescription())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("url")
                        .description("URL to be used for branding")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Branding) environment.getSource()).getUrl())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("image")
                        .description("URL to an image be used for branding")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Branding) environment.getSource()).getImage())
                        .build())
                .build();
         */

        contactType = GraphQLObjectType.newObject()
                              .name("Contact")
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("contactPerson")
                                             .description("Name of person to contact")
                                             .type(Scalars.GraphQLString)//
                                             .build())
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("email")
                                             .description("Email adress for contact")
                                             .type(Scalars.GraphQLString)//
                                             .build())
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("url")
                                             .description("Url for contact")
                                             .type(Scalars.GraphQLString)//
                                             .build())
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("phone")
                                             .description("Phone number for contact")
                                             .type(Scalars.GraphQLString)//
                                             .build())
                              .field(GraphQLFieldDefinition.newFieldDefinition()
                                             .name("furtherDetails")
                                             .description("Textual description of how to get in contact")
                                             .type(Scalars.GraphQLString)//
                                             .build())
                              .build();


        bookingArrangementType = GraphQLObjectType.newObject()
                                         .name("BookingArrangement")
            /*
                                         .field(GraphQLFieldDefinition.newFieldDefinition()
                                                        .name("bookingAccess")
                                                        .description("Who has access to book service?")
                                                        .type(bookingAccessEnum)
                                                        .build())
             */
                                         .field(GraphQLFieldDefinition.newFieldDefinition()
                                                        .name("bookingMethods")
                                                        .description("How should service be booked?")
                                                        .type(Scalars.GraphQLString)
                                                        .build())
            /*
                                         .field(GraphQLFieldDefinition.newFieldDefinition()
                                                        .name("bookWhen")
                                                        .description("When should service be booked?")
                                                        .type(purchaseWhenEnum)
                                                        .build())
             */
                                         .field(GraphQLFieldDefinition.newFieldDefinition()
                                                        .name("latestBookingTime")
                                                        .description("Latest time service can be booked. ISO 8601 timestamp")
                                                        .type(localTimeScalar)
                                                        .build())
                                         .field(GraphQLFieldDefinition.newFieldDefinition()
                                                        .name("minimumBookingPeriod")
                                                        .description("Minimum period in advance service can be booked as a ISO 8601 duration")
                                                        .type(Scalars.GraphQLString)
                                                        .build())
                                         .field(GraphQLFieldDefinition.newFieldDefinition()
                                                        .name("bookingNote")
                                                        .description("Textual description of booking arrangement for service")
                                                        .type(Scalars.GraphQLString)
                                                        .build())
                /*
                                         .field(GraphQLFieldDefinition.newFieldDefinition()
                                                        .name("buyWhen")
                                                        .description("When should ticket be purchased?")
                                                        .type(new GraphQLList(purchaseMomentEnum))
                                                        .build())

                 */
                                         .field(GraphQLFieldDefinition.newFieldDefinition()
                                                        .name("bookingContact")
                                                        .description("Who should ticket be contacted for booking")
                                                        .type(contactType)
                                                        .build())
                                         .build();

        destinationDisplayType = GraphQLObjectType.newObject()
                .name("DestinationDisplay")
                .description("An advertised destination of a specific journey pattern, usually displayed on a head sign or at other on-board locations.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("frontText")
                        .description("Name of destination to show on front of vehicle.")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> environment.getSource())
                        .build())
                .build();


        locationType = GraphQLInputObjectType.newInputObject()
                .name("Location")
                .description("Input format for specifying a location through either a place reference (id), coordinates or both. If both place and coordinates are provided the place ref will be used if found, coordinates will only be used if place is not known.")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("name")
                        .description("The name of the location. This is pass-through information"
                                + "and is not used in routing.")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("place")
                        .description("The id of an element in the OTP model. Currently supports"
                                + " Quay, StopPlace, multimodal StopPlace, and GroupOfStopPlaces.")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("coordinates")
                        .description("Coordinates for the location. This can be used alone or as"
                                + " fallback if the place id is not found.")
                        .type(coordinateInputType)
                        .build())
                .build();

        linkGeometryType = GraphQLObjectType.newObject()
                .name("PointsOnLink")
                .description("A list of coordinates encoded as a polyline string (see http://code.google.com/apis/maps/documentation/polylinealgorithm.html)")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("length")
                        .description("The number of points in the string")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((EncodedPolylineBean) environment.getSource()).getLength())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("points")
                        .description("The encoded points of the polyline. Be aware that the string could contain escape characters that need to be accounted for. " +
                                "(https://www.freeformatter.com/javascript-escape.html)")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((EncodedPolylineBean) environment.getSource()).getPoints())
                        .build())
                .build();


        createPlanType();


        GraphQLInputObjectType preferredInputType = GraphQLInputObjectType.newInputObject()
                .name("InputPreferred")
                .description("Preferences for trip search.")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("lines")
                        .description("Set of ids of lines preferred by user.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .defaultValue(new ArrayList<>())
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("authorities")
                        .description("Set of ids of authorities preferred by user.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .defaultValue(new ArrayList<>())
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("organisations")
                        .description("Deprecated! Use 'authorities' instead.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .defaultValue(new ArrayList<>())
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("otherThanPreferredLinesPenalty")
                        .description("Penalty added for using a line that is not preferred if user has set any line as preferred. In number of seconds that user is willing to wait for preferred line.")
                        .type(Scalars.GraphQLInt)
                        .defaultValue(defaultRoutingRequest.otherThanPreferredRoutesPenalty)
                        .build())
                .build();

        GraphQLInputObjectType unpreferredInputType = GraphQLInputObjectType.newInputObject()
                .name("InputUnpreferred")
                .description("Negative preferences for trip search. Unpreferred elements may still be used in suggested trips if alternatives are not desirable, see InputBanned for hard limitations.")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("lines")
                        .description("Set of ids of lines user prefers not to use.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .defaultValue(new ArrayList<>())
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("authorities")
                        .description("Set of ids of authorities user prefers not to use.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .defaultValue(new ArrayList<>())
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("organisations")
                        .description("Deprecated! Use 'authorities' instead.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .defaultValue(new ArrayList<>())
                        .build())
                .build();

        GraphQLInputObjectType bannedInputType = GraphQLInputObjectType.newInputObject()
                .name("InputBanned")
                .description("Filter trips by disallowing trip patterns involving certain elements")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("lines")
                        .description("Set of ids for lines that should not be used")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .defaultValue(new ArrayList<>())
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("authorities")
                        .description("Set of ids for authorities that should not be used")
                        .defaultValue(new ArrayList<>())
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("organisations")
                        .description("Deprecated! Use 'authorities' instead.")
                        .defaultValue(new ArrayList<>())
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                // TODO trip ids (serviceJourneys) are expected on format AgencyId:trip-id[:stop ordinal:stop ordinal..] and thus will not work with serviceJourney ids containing ':'.
                // Need to subclass GraphQLPlanner if this field is to be supported
//                                                         .field(GraphQLInputObjectField.newInputObjectField()
//                                                                        .name("serviceJourneys")
//                                                                        .description("Do not use certain named serviceJourneys")
//                                                                        .type(new GraphQLList(Scalars.GraphQLString))
//                                                                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("quays")
                        .description("Set of ids of quays that should not be allowed for boarding or alighting. Trip patterns that travel through the quay will still be permitted.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .defaultValue(new ArrayList<>())
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("quaysHard")
                        .description("Set of ids of quays that should not be allowed for boarding, alighting or traveling thorugh.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .defaultValue(new ArrayList<>())
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("serviceJourneys")
                        .description("Set of ids of service journeys that should not be used.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .defaultValue(new ArrayList<>())
                        .build())
                .build();

        GraphQLInputObjectType whiteListedInputType = GraphQLInputObjectType.newInputObject()
                .name("InputWhiteListed")
                .description("Filter trips by only allowing trip patterns involving certain elements. If both lines and authorities are specificed, only one must be valid for each trip to be used.")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("lines")
                        .description("Set of ids for lines that should be used")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("authorities")
                        .description("Set of ids for authorities that should be used")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("organisations")
                        .description("Deprecated! Use 'authorities' instead.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                .build();

        GraphQLInputObjectType transportSubmodeFilterInputType = GraphQLInputObjectType.newInputObject()
                .name("TransportSubmodeFilter")
                .description("Filter trips by allowing only certain transport submodes per mode.")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("transportMode")
                        .description("Set of ids for lines that should be used")
                        .type(new GraphQLNonNull(transportModeEnum))
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("transportSubmodes")
                        .description("Set of transport submodes allowed for transport mode.")
                        .type(new GraphQLNonNull(new GraphQLList(transportSubmode)))
                        .build())
                 .build();


        GraphQLFieldDefinition tripFieldType = GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .description("Input type for executing a travel search for a trip between two locations. Returns trip patterns describing suggested alternatives for the trip.")
                .type(tripType)
                .argument(GraphQLArgument.newArgument()
                        .name("dateTime")
                        .description("Date and time for the earliest time the user is willing to start the journey (if arriveBy=false/not set) or the latest acceptable time of arriving (arriveBy=true). Defaults to now")
                        .type(dateTimeScalar)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("searchWindow")
                        .description("The length of the search-window in seconds. This is normally dynamically calculated by the server, but you may override this by setting it. The search-window used in a request is returned in the response metadata. To get the \"next page\" of trips use the metadata(searchWindowUsed and nextWindowDateTime) to create a new request. If not provided the value is resolved depending on the other input parameters, available transit options and realtime changes.")
                        .type(Scalars.GraphQLInt)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("from")
                        .description("The start location")
                        .type(new GraphQLNonNull(locationType))
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("to")
                        .description("The end location")
                        .type(new GraphQLNonNull(locationType))
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("wheelchair")
                        .description("Whether the trip must be wheelchair accessible.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.wheelchairAccessible)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("numTripPatterns")
                        .description("The maximum number of trip patterns to return.")
                        .defaultValue(defaultRoutingRequest.numItineraries)
                        .type(Scalars.GraphQLInt)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("maximumWalkDistance")
                        .description("DEPRECATED - Use maxPreTransitWalkDistance/maxTransferWalkDistance instead. " +
                                "The maximum distance (in meters) the user is willing to walk. Note that trip patterns with " +
                                "longer walking distances will be penalized, but not altogether disallowed. Maximum allowed value is 15000 m")
                        .defaultValue(defaultRoutingRequest.maxWalkDistance)
                        .type(Scalars.GraphQLFloat)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("maxTransferWalkDistance")
                        .description("The maximum walk distance allowed for transfers.")
                        .defaultValue(defaultRoutingRequest.maxTransferWalkDistance)
                        .type(Scalars.GraphQLFloat)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("walkSpeed")
                        .description("The maximum walk speed along streets, in meters per second")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(defaultRoutingRequest.walkSpeed)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("bikeSpeed")
                        .description("The maximum bike speed along streets, in meters per second")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(defaultRoutingRequest.bikeSpeed)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("optimisationMethod")
                        .description("The set of characteristics that the user wants to optimise for -- defaults to " + reverseMapEnumVal(optimisationMethodEnum, defaultRoutingRequest.optimize))
                        .type(optimisationMethodEnum)
                        .defaultValue(defaultRoutingRequest.optimize)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("arriveBy")
                        .description("Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.arriveBy)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("vias")
                        .description("An ordered list of intermediate locations to be visited.")
                        .type(new GraphQLList(locationType))
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("preferred")
                        .description("Parameters for indicating authorities or lines that preferably should be used in trip patters. A cost is applied to boarding nonpreferred authorities or lines (otherThanPreferredRoutesPenalty).")
                        .type(preferredInputType)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("unpreferred")
                        .description("Parameters for indicating authorities or lines that preferably should not be used in trip patters. A cost is applied to boarding nonpreferred authorities or lines (otherThanPreferredRoutesPenalty).")
                        .type(unpreferredInputType)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("banned")
                        .description("Banned")
                        .description("Parameters for indicating authorities, lines or quays not be used in the trip patterns")
                        .type(bannedInputType)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("whiteListed")
                        .description("Whitelisted")
                        .description("Parameters for indicating the only authorities, lines or quays to be used in the trip patterns")
                        .type(whiteListedInputType)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("transferPenalty")
                        .description("An extra penalty added on transfers (i.e. all boardings except the first one). The transferPenalty is used when a user requests even less transfers. In the latter case, we don't actually optimise for fewest transfers, as this can lead to absurd results. Consider a trip in New York from Grand Army Plaza (the one in Brooklyn) to Kalustyan's at noon. The true lowest transfers trip pattern is to wait until midnight, when the 4 train runs local the whole way. The actual fastest trip pattern is the 2/3 to the 4/5 at Nevins to the 6 at Union Square, which takes half an hour. Even someone optimise for fewest transfers doesn't want to wait until midnight. Maybe they would be willing to walk to 7th Ave and take the Q to Union Square, then transfer to the 6. If this takes less than transferPenalty seconds, then that's what we'll return.")
                        .type(Scalars.GraphQLInt)
                        .defaultValue(defaultRoutingRequest.transferPenalty)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("modes")
                        .description("The set of modes that a user is willing to use. Defaults to " + reverseMapEnumVals(modeEnum, defaultRoutingRequest.modes.getModes()))
                        .type(new GraphQLList(modeEnum))
                        .defaultValue(defaultRoutingRequest.modes.getModes())
                        .build())
                .argument(GraphQLArgument.newArgument()
                         .name("transportSubmodes")
                         .description("Optional set of allowed submodes per transport mode provided in 'modes'. If at least one submode is set for a transport mode all submodes not set will be disregarded. Note that transportMode must also be included in 'modes' for the submodes to be allowed")
                         .type(new GraphQLList(transportSubmodeFilterInputType))
                         .defaultValue(new ArrayList<>())
                         .build())
                .argument(GraphQLArgument.newArgument()
                        .name("allowBikeRental")
                        .description("Is bike rental allowed?")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.allowBikeRental)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("minimumTransferTime")
                        .description("A global minimum transfer time (in seconds) that specifies the minimum amount of time that must pass between exiting one public transport vehicle and boarding another. This time is in addition to time it might take to walk between stops.")
                        .type(Scalars.GraphQLInt)
                        .defaultValue(defaultRoutingRequest.transferSlack)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("maximumTransfers")
                        .description("Maximum number of transfers")
                        .type(Scalars.GraphQLInt)
                        .defaultValue(defaultRoutingRequest.maxTransfers)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("ignoreRealtimeUpdates")
                        .description("When true, realtime updates are ignored during this search.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.ignoreRealtimeUpdates)
                        .build())
                /*
                .argument(GraphQLArgument.newArgument()
                        .name("includePlannedCancellations")
                        .description("When true, service journeys cancelled in scheduled route data will be included during this search.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.includePlannedCancellations)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("ignoreInterchanges")
                        .description("DEPRECATED - For debugging only. Ignores interchanges defined in timetable data.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.ignoreInterchanges)
                        .build())
                 */
                .argument(GraphQLArgument.newArgument()
                        .name("locale")
                        .type(localeEnum)
                        .defaultValue("no")
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("compactLegsByReversedSearch")
                        .description("DEPRECATED - NO EFFECT IN OTP2")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(false)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("reverseOptimizeOnTheFly")
                        .description("DEPRECATED - NO EFFECT IN OTP2.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(false)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("maxPreTransitTime")
                        .description("Maximum time for the ride part of \"kiss and ride\" and \"ride and kiss\".")
                        .type(Scalars.GraphQLInt)
                        .defaultValue(defaultRoutingRequest.maxPreTransitTime)
                        .build())
                /*
                .argument(GraphQLArgument.newArgument()
                        .name("preTransitReluctance")
                        .description("How much worse driving before and after transit is than riding on transit. Applies to ride and kiss, kiss and ride and park and ride.")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(defaultRoutingRequest.preTransitReluctance)
                        .build())
                */
                .argument(GraphQLArgument.newArgument()
                        .name("maxPreTransitWalkDistance")
                        .description("Max walk distance for access/egress legs. NOT IMPLEMENTED")
                        .type(Scalars.GraphQLFloat)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("useFlex")
                        .type(Scalars.GraphQLBoolean)
                        .description("NOT IMPLEMENTED")
                        .build())
                /*
                .argument(GraphQLArgument.newArgument()
                        .name("banFirstServiceJourneysFromReuseNo")
                        .description("How many service journeys used in a tripPatterns should be banned from inclusion in successive tripPatterns. Counting from start of tripPattern.")
                        .type(Scalars.GraphQLInt)
                        .defaultValue(defaultRoutingRequest.banFirstTripsFromReuseNo)
                        .build())
                 */
                .argument(GraphQLArgument.newArgument()
                        .name("walkReluctance")
                        .description("Walk cost is multiplied by this value. This is the main parameter to use for limiting walking.")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(defaultRoutingRequest.walkReluctance)
                        .build())
                /*
                .argument(GraphQLArgument.newArgument()
                        .name("ignoreMinimumBookingPeriod")
                        .description("Ignore the MinimumBookingPeriod defined on the ServiceJourney and allow itineraries to start immediately after the current time.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.ignoreDrtAdvanceBookMin)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("transitDistanceReluctance")
                        .description("The extra cost per meter that is travelled by transit. This is a cost point peter meter, so it should in most\n" +
                                "cases be a very small fraction. The purpose of assigning a cost to distance is often because it correlates with\n" +
                                "fare prices and you want to avoid situations where you take detours or travel back again even if it is\n" +
                                "technically faster. Setting this value to 0 turns off the feature altogether.")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(defaultRoutingRequest.transitDistanceReluctance)
                        .build())
                */
                .argument(GraphQLArgument.newArgument()
                        .name("debugItineraryFilter")
                        .description("Debug the itinerary-filter-chain. The filters will mark itineraries as deleted, but NOT delete them when this is enabled.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.debugItineraryFilter)
                        .build())

                .dataFetcher(environment -> new TransmodelGraphQLPlanner(mappingUtil).plan(environment))
                .build();

        noticeType = GraphQLObjectType.newObject()
                .name("Notice")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(
                                environment -> ((Notice) environment.getSource()).getId())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("text")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(
                                environment -> ((Notice) environment.getSource()).getText())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("publicCode")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(
                                environment -> ((Notice) environment.getSource()).getPublicCode())
                        .build())
                .build();

        tariffZoneType = GraphQLObjectType.newObject()
                .name("TariffZone")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(e -> "NOT IMPLEMENTED")
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(e -> "NOT IMPLEMENTED")
                        .build())
                .build();

        multilingualStringType = GraphQLObjectType.newObject()
                .name("MultilingualString")
                .description("Text with language")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("value")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Map.Entry<String, String>) environment.getSource()).getValue())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("language")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Map.Entry<String, String>) environment.getSource()).getKey())
                        .build())
                .build();

        GraphQLObjectType validityPeriodType = GraphQLObjectType.newObject()
                .name("ValidityPeriod")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("startTime")
                        .type(dateTimeScalar)
                        .description("Start of validity period")
                        .dataFetcher(environment -> {
                            Pair<Long, Long> period = environment.getSource();
                            return period != null ? period.getLeft() : null;
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("endTime")
                        .type(dateTimeScalar)
                        .description("End of validity period")
                        .dataFetcher(environment -> {
                            Pair<Long, Long> period = environment.getSource();
                            return period != null ? period.getRight() : null;
                        })
                        .build())
                .build();


        GraphQLObjectType infoLinkType = GraphQLObjectType.newObject()
                .name("infoLink")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("uri")
                        .type(Scalars.GraphQLString)
                        .description("URI")
                        .dataFetcher(environment -> {
                            AlertUrl source = environment.getSource();
                            return source.uri;
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("label")
                        .type(Scalars.GraphQLString)
                        .description("Label")
                        .dataFetcher(environment -> {
                            AlertUrl source = environment.getSource();
                            return source.label;
                        })
                        .build())
                .build();

        ptSituationElementType = GraphQLObjectType.newObject()
                .name("PtSituationElement")
                .description("Simple public transport situation element")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment -> relay.toGlobalId(
                                ptSituationElementType.getName(), ((AlertPatch) environment.getSource()).getId()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("authority")
                        .type(authorityType)
                        .description("Get affected authority for this situation element")
                        .dataFetcher(environment -> getAgency(((AlertPatch) environment.getSource()).getAgency()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("organisation")
                        .deprecate("Use 'authority' instead.")
                        .type(organisationType)
                        .dataFetcher(environment -> getAgency(((AlertPatch) environment.getSource()).getAgency()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lines")
                        .type(new GraphQLNonNull(new GraphQLList(lineType)))
                        .dataFetcher(environment -> wrapInListUnlessNull(index.routeForId.get(((AlertPatch) environment.getSource()).getRoute())))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourneys")
                        .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
                        .dataFetcher(environment -> wrapInListUnlessNull(index.tripForId.get(((AlertPatch) environment.getSource()).getTrip())))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quays")
                        .type(new GraphQLNonNull(new GraphQLList(quayType)))
                        .dataFetcher(environment ->
                                wrapInListUnlessNull(index.stopForId.get(((AlertPatch) environment.getSource()).getStop()))
                        )
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("stopPlaces")
                        .type(new GraphQLNonNull(new GraphQLList(stopPlaceType)))
                        .dataFetcher(environment ->
                                wrapInListUnlessNull(index.stationForId.get(((AlertPatch) environment.getSource()).getStop()))
                        )
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("journeyPatterns")
                        .description("Get all journey patterns for this situation element")
                        .type(new GraphQLNonNull(new GraphQLList(journeyPatternType)))
                        .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getTripPatterns())
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("summary")
                        .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(multilingualStringType))))
                        .description("Summary of situation in all different translations available")
                        .dataFetcher(environment -> {
                            AlertPatch alertPatch = environment.getSource();
                            Alert alert = alertPatch.getAlert();
                            if (alert.alertHeaderText instanceof TranslatedString) {
                                return ((TranslatedString) alert.alertHeaderText).getTranslations();
                            } else if (alert.alertHeaderText != null) {
                                return Arrays.asList(new AbstractMap.SimpleEntry<>(null, alert.alertHeaderText.toString()));
                            } else {
                                return emptyList();
                            }
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("description")
                        .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(multilingualStringType))))
                        .description("Description of situation in all different translations available")
                        .dataFetcher(environment -> {
                            AlertPatch alertPatch = environment.getSource();
                            Alert alert = alertPatch.getAlert();
                            if (alert.alertDescriptionText instanceof TranslatedString) {
                                return ((TranslatedString) alert.alertDescriptionText).getTranslations();
                            } else if (alert.alertDescriptionText != null) {
                                return Arrays.asList(new AbstractMap.SimpleEntry<>(null, alert.alertDescriptionText.toString()));
                            } else {
                                return emptyList();
                            }
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("detail")
                        .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(multilingualStringType))))
                        .description("Details of situation in all different translations available")
                        .deprecate("Not allowed according to profile. Use ´advice´ instead.")
                        .dataFetcher(environment -> {
                            AlertPatch alertPatch = environment.getSource();
                            Alert alert = alertPatch.getAlert();
                            if (alert.alertDetailText instanceof TranslatedString) {
                                return ((TranslatedString) alert.alertDetailText).getTranslations();
                            } else if (alert.alertDetailText != null) {
                                return Arrays.asList(new AbstractMap.SimpleEntry<>(null, alert.alertDetailText.toString()));
                            } else {
                                return emptyList();
                            }
                        })
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("advice")
                        .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(multilingualStringType))))
                        .description("Advice of situation in all different translations available")
                        .dataFetcher(environment -> {
                            AlertPatch alertPatch = environment.getSource();
                            Alert alert = alertPatch.getAlert();
                            if (alert.alertAdviceText instanceof TranslatedString) {
                                return ((TranslatedString) alert.alertAdviceText).getTranslations();
                            } else if (alert.alertAdviceText != null) {
                                return Arrays.asList(new AbstractMap.SimpleEntry<>(null, alert.alertAdviceText.toString()));
                            } else {
                                return emptyList();
                            }
                        })
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("infoLink")
                        .type(Scalars.GraphQLString)
                        .deprecate("Use the attribute infoLinks instead.")
                        .description("Url with more information")
                        .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertUrl)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("infoLinks")
                        .type(new GraphQLList(infoLinkType))
                        .description("Optional links to more information.")
                        .dataFetcher(environment -> null)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("validityPeriod")
                        .type(validityPeriodType)
                        .description("Period this situation is in effect")
                        .dataFetcher(environment -> {
                            Alert alert = ((AlertPatch) environment.getSource()).getAlert();
                            Long startTime = alert.effectiveStartDate != null ? alert.effectiveStartDate.getTime() : null;
                            Long endTime = alert.effectiveEndDate != null ? alert.effectiveEndDate.getTime() : null;
                            return Pair.of(startTime, endTime);
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("reportType")
                    .type(reportTypeEnum)
                    .description("ReportType of this situation")
                    .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertType)
                    .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("stopConditions")
                        .type(new GraphQLNonNull(new GraphQLList(stopConditionEnum)))
                        .deprecate("Temporary attribute used for data-verification.")
                        .description("StopConditions of this situation")
                        .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getStopConditions())
                        .build())
                */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situationNumber")
                        .type(Scalars.GraphQLString)
                        .description("Operator's internal id for this situation")
                        .dataFetcher(environment -> null)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("severity")
                        .type(severityEnum)
                        .description("Severity of this situation ")
                        .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().severity)
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("reportAuthority")
                        .type(authorityType)
                        .description("Authority that reported this situation")
                        .deprecate("Not yet officially supported. May be removed or renamed.")
                        .dataFetcher(environment -> getAgency(((AlertPatch) environment.getSource()).getFeedId()))
                        .build())
                .build();


        quayAtDistance = GraphQLObjectType.newObject()
                .name("QuayAtDistance")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment -> relay.toGlobalId(quayAtDistance.getName(),
                                Integer.toString(((GraphIndex.StopAndDistance) environment.getSource()).distance) + ";" +
                                        mappingUtil.toIdString(((GraphIndex.StopAndDistance) environment.getSource()).stop.getId())))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quay")
                        .type(quayType)
                        .dataFetcher(environment -> ((GraphIndex.StopAndDistance) environment.getSource()).stop)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("distance")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((GraphIndex.StopAndDistance) environment.getSource()).distance)
                        .build())
                .build();

        placeAtDistanceType = GraphQLObjectType.newObject()
                .name("PlaceAtDistance")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .deprecate("Id is not referable or meaningful and will be removed")
                        .dataFetcher(environment -> relay.toGlobalId(placeAtDistanceType.getName(), "N/A"))
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("place")
                        .type(placeInterface)
                        .dataFetcher(environment -> ((GraphIndex.PlaceAndDistance) environment.getSource()).place)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("distance")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((GraphIndex.PlaceAndDistance) environment.getSource()).distance)
                        .build())
                 */
                .build();

        stopPlaceType = GraphQLObjectType.newObject()
                .name("StopPlace")
                .description("Named place where public transport may be accessed. May be a building complex (e.g. a station) or an on-street location.")
                .withInterface(placeInterface)
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment ->
                                mappingUtil.toIdString(((MonoOrMultiModalStation) environment.getSource()).getId()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("latitude")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> (((MonoOrMultiModalStation) environment.getSource()).getLat()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("longitude")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> (((MonoOrMultiModalStation) environment.getSource()).getLon()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("description")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> (((MonoOrMultiModalStation) environment.getSource()).getDescription()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("wheelchairBoarding")
                        .description("Whether this stop place is suitable for wheelchair boarding.")
                        .type(wheelchairBoardingEnum)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("weighting")
                        .description("Relative weighting of this stop with regards to interchanges. NOT IMPLEMENTED")
                        .type(interchangeWeightingEnum)
                        .dataFetcher(environment -> 0)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("tariffZones")
                        .type(new GraphQLNonNull(new GraphQLList(tariffZoneType)))
                        .description("NOT IMPLEMENTED")
                        .dataFetcher(environment -> new ArrayList<>())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transportMode")
                        .description("The transport mode serviced by this stop place.  NOT IMPLEMENTED")
                        .type(transportModeEnum)
                        .dataFetcher(environment -> "unknown")
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transportSubmode")
                        .description("The transport submode serviced by this stop place. NOT IMPLEMENTED")
                        .type(transportSubmode)
                        .dataFetcher(environment -> TransmodelTransportSubmode.UNDEFINED)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("adjacentSites")
                        .description("This stop place's adjacent sites")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .dataFetcher(environment -> ((MonoOrMultiModalStation) environment.getSource()).getAdjacentSites())
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("timezone")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                // TODO stopPlaceType?

                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quays")
                        .description("Returns all quays that are children of this stop place")
                        .type(new GraphQLList(quayType))
                        .argument(GraphQLArgument.newArgument()
                                .name("filterByInUse")
                                .description("If true only quays with at least one visiting line are included.")
                                .type(Scalars.GraphQLBoolean)
                                .defaultValue(Boolean.FALSE)
                                .build())
                        .dataFetcher(environment -> {
                            Collection<Stop> quays = ((MonoOrMultiModalStation) environment.getSource()).getChildStops();
                            if (Boolean.TRUE.equals(environment.getArgument("filterByInUse"))) {
                                quays=quays.stream().filter(stop ->  !index.getPatternsForStop(stop,true).isEmpty()).collect(Collectors.toList());
                            }
                            return quays;
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("parent")
                        .description("Returns parent stop for this stop")
                        .type(stopPlaceType)
                        .dataFetcher(
                            environment -> (
                                ((MonoOrMultiModalStation) environment.getSource())
                                    .getParentStation()
                            ))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("estimatedCalls")
                        .description("List of visits to this stop place as part of vehicle journeys.")
                        .type(new GraphQLNonNull(new GraphQLList(estimatedCallType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("startTime")
                                .type(dateTimeScalar)
                                .description("DateTime for when to fetch estimated calls from. Default value is current time")
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("timeRange")
                                .type(Scalars.GraphQLInt)
                                .defaultValue(24 * 60 * 60)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("numberOfDepartures")
                                .description("Limit the total number of departures returned.")
                                .type(Scalars.GraphQLInt)
                                .defaultValue(5)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("numberOfDeparturesPerLineAndDestinationDisplay")
                                .description("Limit the number of departures per line and destination display returned. The parameter is only applied " +
                                        "when the value is between 1 and 'numberOfDepartures'.")
                                .type(Scalars.GraphQLInt)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("omitNonBoarding")
                                .type(Scalars.GraphQLBoolean)
                                .defaultValue(false)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("whiteListed")
                                .description("Whitelisted")
                                .description("Parameters for indicating the only authorities and/or lines or quays to list estimatedCalls for")
                                .type(whiteListedInputType)
                                .build())
                        .dataFetcher(environment -> {
                            boolean omitNonBoarding = environment.getArgument("omitNonBoarding");
                            int numberOfDepartures = environment.getArgument("numberOfDepartures");
                            Integer departuresPerLineAndDestinationDisplay = environment.getArgument("numberOfDeparturesPerLineAndDestinationDisplay");
                            int timeRage = environment.getArgument("timeRange");

                            MonoOrMultiModalStation monoOrMultiModalStation = environment.getSource();

                            Set<String> authorityIds = new HashSet();
                            Set<FeedScopedId> lineIds = new HashSet();
                            Map<String, List<String>> whiteList = environment.getArgument("whiteListed");
                            if (whiteList != null) {
                                List<String> authorityIdList = whiteList.get("authorities");
                                if (authorityIdList != null) {
                                    authorityIds.addAll(authorityIdList);
                                }

                                List<String> lineIdList = whiteList.get("lines");
                                if (lineIdList != null) {
                                    lineIds.addAll(lineIdList.stream().map(id -> mappingUtil.fromIdString(id)).collect(Collectors.toSet()));
                                }
                            }

                            Long startTimeMs = environment.getArgument("startTime") == null ? 0l : environment.getArgument("startTime");
                            Long startTimeSeconds = startTimeMs / 1000;

                            return monoOrMultiModalStation.getChildStops()
                                    .stream()
                                    .flatMap(singleStop ->
                                            getTripTimesForStop(
                                                    singleStop,
                                                    startTimeSeconds,
                                                    timeRage,
                                                    omitNonBoarding,
                                                    numberOfDepartures,
                                                    departuresPerLineAndDestinationDisplay,
                                                    authorityIds,
                                                    lineIds
                                            )
                                    )
                                    .sorted(TripTimeShort.compareByDeparture())
                                    .distinct()
                                    .limit((long) numberOfDepartures)
                                    .collect(Collectors.toList());
                        })
                        .build())
                .build();

        quayType = GraphQLObjectType.newObject()
                .name("Quay")
                .description("A place such as platform, stance, or quayside where passengers have access to PT vehicles.")
                .withInterface(placeInterface)
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment ->
                                mappingUtil.toIdString(((Stop) environment.getSource()).getId()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("latitude")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> (((Stop) environment.getSource()).getLat()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("longitude")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> (((Stop) environment.getSource()).getLon()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("description")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> (((Stop) environment.getSource()).getDescription()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("stopPlace")
                    .description("The stop place to which this quay belongs to.")
                    .type(stopPlaceType)
                    .dataFetcher(environment ->
                        {
                            Station station = ((Stop) environment.getSource()).getParentStation();
                            if (station != null) {
                                return new MonoOrMultiModalStation(
                                    station,
                                    graph.index.multiModalStationForStations.get(station));
                            } else {
                                return null;
                            }
                        }
                    )
                    .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("wheelchairAccessible")
                        .type(wheelchairBoardingEnum)
                        .description("Whether this quay is suitable for wheelchair boarding.")
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("timezone")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("publicCode")
                        .type(Scalars.GraphQLString)
                        .description("Public code used to identify this quay within the stop place. For instance a platform code.")
                        .dataFetcher(environment -> (((Stop) environment.getSource()).getCode()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lines")
                        .description("List of lines servicing this quay")
                        .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(lineType))))
                        .dataFetcher(environment -> index.getPatternsForStop(environment.getSource(),true)
                                .stream()
                                .map(pattern -> pattern.route)
                                .distinct()
                                .collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("journeyPatterns")
                        .description("List of journey patterns servicing this quay")
                        .type(new GraphQLNonNull(new GraphQLList(journeyPatternType)))
                        .dataFetcher(environment -> index.getPatternsForStop(environment.getSource(), true))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("estimatedCalls")
                        .description("List of visits to this quay as part of vehicle journeys.")
                        .type(new GraphQLNonNull(new GraphQLList(estimatedCallType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("startTime")
                                .type(dateTimeScalar)
                                .description("DateTime for when to fetch estimated calls from. Default value is current time")
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("timeRange")
                                .type(Scalars.GraphQLInt)
                                .defaultValue(24 * 60 * 60)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("numberOfDepartures")
                                .description("Limit the total number of departures returned.")
                                .type(Scalars.GraphQLInt)
                                .defaultValue(5)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("numberOfDeparturesPerLineAndDestinationDisplay")
                                .description("Limit the number of departures per line and destination display returned. The parameter is only applied " +
                                        "when the value is between 1 and 'numberOfDepartures'.")
                                .type(Scalars.GraphQLInt)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("omitNonBoarding")
                                .type(Scalars.GraphQLBoolean)
                                .defaultValue(false)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("whiteListed")
                                .description("Whitelisted")
                                .description("Parameters for indicating the only authorities and/or lines or quays to list estimatedCalls for")
                                .type(whiteListedInputType)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                            .name("includeCancelledTrips")
                            .description("Indicates that realtime-cancelled trips should also be included. NOT IMPLEMENTED")
                            .type(Scalars.GraphQLBoolean)
                            .defaultValue(false)
                            .build())
                        .dataFetcher(environment -> {
                            boolean omitNonBoarding = environment.getArgument("omitNonBoarding");
                            int numberOfDepartures = environment.getArgument("numberOfDepartures");
                            Integer departuresPerLineAndDestinationDisplay = environment.getArgument("numberOfDeparturesPerLineAndDestinationDisplay");
                            int timeRange = environment.getArgument("timeRange");
                            Stop stop = environment.getSource();

                            Set<String> authorityIds = new HashSet();
                            Set<FeedScopedId> lineIds = new HashSet();
                            Map<String, List<String>> whiteList = environment.getArgument("whiteListed");

                            if (whiteList != null) {
                                List<String> authorityIdList = whiteList.get("authorities");
                                if (authorityIdList != null) {
                                    authorityIds.addAll(authorityIdList);
                                }

                                List<String> lineIdList = whiteList.get("lines");
                                if (lineIdList != null) {
                                    lineIds.addAll(lineIdList.stream().map(id -> mappingUtil.fromIdString(id)).collect(Collectors.toSet()));
                                }
                            }

                            Long startTimeMs = environment.getArgument("startTime") == null ? 0l : environment.getArgument("startTime");
                            Long startTimeSeconds = startTimeMs / 1000;

                            return getTripTimesForStop(
                                    stop,
                                    startTimeSeconds,
                                    timeRange,
                                    omitNonBoarding,
                                    numberOfDepartures,
                                    departuresPerLineAndDestinationDisplay,
                                    authorityIds,
                                    lineIds
                            )
                                .sorted(TripTimeShort.compareByDeparture())
                                .distinct()
                                .limit((long)numberOfDepartures)
                                .collect(Collectors.toList());
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("Get all situations active for the quay.")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                    .dataFetcher(dataFetchingEnvironment -> graph.getSiriAlertPatchService()
                        .getStopPatches(((Stop)dataFetchingEnvironment.getSource()).getId()))
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("stopType")
                        .type(stopTypeEnum)
                        .dataFetcher(environment -> (((Stop) environment.getSource()).getStopType()))
                        .build())
                */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("flexibleArea")
                        .description("Geometry for flexible area.")
                        .type(GeoJSONCoordinatesScalar.getGraphQGeoJSONCoordinatesScalar())
                        .dataFetcher(environment -> (null))
                        .build())
                .build();

        timetabledPassingTimeType = GraphQLObjectType.newObject()
                .name("TimetabledPassingTime")
                .description("Scheduled passing times. These are not affected by real time updates.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quay")
                        .type(quayType)
                        .dataFetcher(environment -> index.stopForId
                                .get(((TripTimeShort) environment.getSource()).stopId))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("arrival")
                        .type(timeType)
                        .description("Scheduled time of arrival at quay")
                        .dataFetcher(
                                environment -> ((TripTimeShort) environment.getSource()).scheduledArrival)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("departure")
                        .type(timeType)
                        .description("Scheduled time of departure from quay")
                        .dataFetcher(
                                environment -> ((TripTimeShort) environment.getSource()).scheduledDeparture)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("timingPoint")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether this is a timing point or not. Boarding and alighting is not allowed at timing points.")
                        .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).timepoint)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("forBoarding")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether vehicle may be boarded at quay.")
                        .dataFetcher(environment -> index.patternForTrip
                                .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                                .getBoardType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("forAlighting")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether vehicle may be alighted at quay.")
                        .dataFetcher(environment -> index.patternForTrip
                                .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                                .getAlightType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("requestStop")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether vehicle will only stop on request.")
                        .dataFetcher(environment -> index.patternForTrip
                                .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                                .getAlightType(((TripTimeShort) environment.getSource()).stopIndex) == PICKDROP_COORDINATE_WITH_DRIVER)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourney")
                        .type(serviceJourneyType)
                        .dataFetcher(environment -> index.tripForId
                                .get(((TripTimeShort) environment.getSource()).tripId))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("destinationDisplay")
                        .type(destinationDisplayType)
                        .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).headsign)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("notices")
                        .type(new GraphQLNonNull(new GraphQLList(noticeType)))
                        .dataFetcher(environment -> {
                            TripTimeShort tripTimeShort = environment.getSource();
                            // TODO OTP2 - fix this
                            return null; //index.getNoticesByEntity(tripTimeShort.);
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bookingArrangements")
                        .description("Booking arrangements for flexible service. NOT IMPLEMENTED")
                        .dataFetcher(environment ->  null)
                        .type(bookingArrangementType)
                        .build())
                .build();

        estimatedCallType = GraphQLObjectType.newObject()
                .name("EstimatedCall")
                .description("List of visits to quays as part of vehicle journeys. Updated with real time information where available")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quay")
                        .type(quayType)
                        .dataFetcher(environment -> index.stopForId
                                .get(((TripTimeShort) environment.getSource()).stopId))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                               .name("aimedArrivalTime")
                               .description("Scheduled time of arrival at quay. Not affected by read time updated")
                               .type(dateTimeScalar)
                               .dataFetcher(
                                       environment -> 1000 * (((TripTimeShort) environment.getSource()).serviceDay +
                                                                      ((TripTimeShort) environment.getSource()).scheduledArrival))
                               .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                               .name("expectedArrivalTime")
                               .type(dateTimeScalar)
                               .description("Expected time of arrival at quay. Updated with real time information if available. Will be null if an actualArrivalTime exists")
                               .dataFetcher(
                                       environment -> {
                                           TripTimeShort tripTimeShort = environment.getSource();
                                           return 1000 * (tripTimeShort.serviceDay +
                                                                  tripTimeShort.realtimeArrival);
                                       })
                               .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                               .name("actualArrivalTime")
                               .type(dateTimeScalar)
                               .description("Actual time of arrival at quay. Updated from real time information if available. NOT IMPLEMENTED")
                               .dataFetcher(
                                       environment -> null)
                               .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                               .name("aimedDepartureTime")
                               .description("Scheduled time of departure from quay. Not affected by read time updated")
                               .type(dateTimeScalar)
                               .dataFetcher(
                                       environment -> 1000 * (((TripTimeShort) environment.getSource()).serviceDay +
                                                                      ((TripTimeShort) environment.getSource()).scheduledDeparture))
                               .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                               .name("expectedDepartureTime")
                               .type(dateTimeScalar)
                               .description("Expected time of departure from quay. Updated with real time information if available. Will be null if an actualDepartureTime exists")
                               .dataFetcher(
                                       environment -> {
                                           TripTimeShort tripTimeShort = environment.getSource();
                                           return 1000 * (tripTimeShort.serviceDay +
                                                                  tripTimeShort.realtimeDeparture);
                                       })
                               .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                               .name("actualDepartureTime")
                               .type(dateTimeScalar)
                               .description("Actual time of departure from quay. Updated with real time information if available. NOT IMPLEMENTED")
                               .dataFetcher(
                                       environment -> null)
                               .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("aimedArrival")
                        .deprecate("Use aimedArrivalTime")
                        .description("Scheduled time of arrival at quay. Not affected by read time updated")
                        .type(timeType)
                        .dataFetcher(
                                environment -> ((TripTimeShort) environment.getSource()).scheduledArrival)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("expectedArrival")
                        .deprecate("Use expectedArrivalTime")
                        .type(timeType)
                        .description("Expected time of arrival at quay. Updated with real time information if available")
                        .dataFetcher(
                                environment -> ((TripTimeShort) environment.getSource()).realtimeArrival)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("aimedDeparture")
                        .deprecate("Use aimedDepartureTime")
                        .description("Scheduled time of departure from quay. Not affected by read time updated")
                        .type(timeType)
                        .dataFetcher(
                                environment -> ((TripTimeShort) environment.getSource()).scheduledDeparture)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("expectedDeparture")
                        .deprecate("Use expectedDepartureTime")
                        .type(timeType)
                        .description("Expected time of departure from quay. Updated with real time information if available")
                        .dataFetcher(
                                environment -> ((TripTimeShort) environment.getSource()).realtimeDeparture)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("timingPoint")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether this is a timing point or not. Boarding and alighting is not allowed at timing points.")
                        .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).timepoint)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("realtime")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether this call has been updated with real time information.")
                        .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).realtime)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("predictionInaccurate")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether the updated estimates are expected to be inaccurate.")
                        .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).predictionInaccurate)
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("realtimeState")
                        .type(realtimeStateEnum)
                        .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).realtimeState)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("forBoarding")
                    .type(Scalars.GraphQLBoolean)
                    .description("Whether vehicle may be boarded at quay.")
                    .dataFetcher(environment -> {
                        if (((TripTimeShort) environment.getSource()).pickupType >= 0) {
                            //Realtime-updated
                            return ((TripTimeShort) environment.getSource()).pickupType != PICKDROP_NONE;
                        }
                        return index.patternForTrip
                            .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                            .getBoardType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE;
                    })
                    .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("forAlighting")
                    .type(Scalars.GraphQLBoolean)
                    .description("Whether vehicle may be alighted at quay.")
                    .dataFetcher(environment -> {
                        if (((TripTimeShort) environment.getSource()).dropoffType >= 0) {
                            //Realtime-updated
                            return ((TripTimeShort) environment.getSource()).dropoffType != PICKDROP_NONE;
                        }
                        return index.patternForTrip
                            .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                            .getAlightType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE;

                    })
                    .build())

                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("requestStop")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether vehicle will only stop on request.")
                        .dataFetcher(environment -> index.patternForTrip
                                .get(index.tripForId.get(((TripTimeShort) environment.getSource()).tripId))
                                .getAlightType(((TripTimeShort) environment.getSource()).stopIndex) == PICKDROP_COORDINATE_WITH_DRIVER)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("cancellation")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether stop is cancellation. NOT IMPLEMENTED")
                        .dataFetcher(environment -> false)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("date")
                        .type(dateScalar)
                        .description("The date the estimated call is valid for.")
                        .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).serviceDay)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourney")
                        .type(serviceJourneyType)
                        .dataFetcher(environment -> index.tripForId
                                .get(((TripTimeShort) environment.getSource()).tripId))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("destinationDisplay")
                        .type(destinationDisplayType)
                        .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).headsign)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("notices")
                        .type(new GraphQLNonNull(new GraphQLList(noticeType)))
                        .dataFetcher(environment -> {
                            // TODO OTP2 - Fix it!
                            //TripTimeShort tripTimeShort = environment.getSource();
                            return Collections.emptyList(); //index.getNoticesByEntity(tripTimeShort.stopTimeId);
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                        .description("Get all relevant situations for this EstimatedCall.")
                        .dataFetcher(environment -> getAllRelevantAlerts(environment.getSource()))
                        .build())
                 .field(GraphQLFieldDefinition.newFieldDefinition()
                         .name("bookingArrangements")
                         .description("Booking arrangements for flexible service. NOT IMPLEMENTED")
                         .dataFetcher(environment ->  null)
                         .type(bookingArrangementType)
                         .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("flexible")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether this call is part of a flexible trip. This means that arrival or departure " +
                                "times are not scheduled but estimated within specified operating hours.")
                        .dataFetcher(environment -> ((TripTimeShort) environment.getSource()).isFlexible())
                        .build())
                 */
                .build();

        serviceJourneyType = GraphQLObjectType.newObject()
                .name("ServiceJourney")
                .description("A planned vehicle journey with passengers.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment ->
                                mappingUtil.toIdString(((Trip) environment.getSource()).getId()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("line")
                        .type(new GraphQLNonNull(lineType))
                        .dataFetcher(environment -> ((Trip) environment.getSource()).getRoute())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("activeDates")
                        .type(new GraphQLNonNull(new GraphQLList(dateScalar)))
                        .dataFetcher(environment -> index.graph.getCalendarService()
                                .getServiceDatesForServiceId((((Trip) environment.getSource()).getServiceId()))
                                .stream().map(serviceDate -> mappingUtil.serviceDateToSecondsSinceEpoch(serviceDate)).sorted().collect(Collectors.toList())
                        )
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceAlteration")
                        .type(serviceAlterationEnum)
                        .description("Whether journey is as planned, a cancellation or an extra journey. Default is as planned")
                        .dataFetcher(environment -> (((Trip) environment.getSource()).getServiceAlteration()))
                        .build())
                        */

                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transportSubmode")
                        .type(transportSubmode)
                        .description("The transport submode of the journey, if different from lines transport submode. NOT IMPLEMENTED")
                        .dataFetcher(environment -> TransmodelTransportSubmode.UNDEFINED)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("publicCode")
                        .type(Scalars.GraphQLString)
                        .description("Publicly announced code for service journey, differentiating it from other service journeys for the same line. NOT IMPLEMENTED")
                        .dataFetcher(environment -> "")
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("privateCode")
                        .type(Scalars.GraphQLString)
                        .description("For internal use by operators.")
                        .dataFetcher(environment -> (((Trip) environment.getSource()).getTripPrivateCode()))
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("linePublicCode")
                        .type(Scalars.GraphQLString)
                        .deprecate("Use line.publicCode instead.")
                        .description("Publicly announced code for line, differentiating it from other lines for the same operator.")
                        .dataFetcher(environment -> (((Trip) environment.getSource()).getRoute().getShortName()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("operator")
                        .type(operatorType)
                        .dataFetcher(
                                environment -> (((Trip) environment.getSource()).getOperator()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("directionType")
                        .type(directionTypeEnum)
                        .dataFetcher(environment -> directIdStringToInt(((Trip) environment.getSource()).getDirectionId()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("wheelchairAccessible")
                        .type(wheelchairBoardingEnum)
                        .description("Whether service journey is accessible with wheelchair.")
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikesAllowed")
                        .type(bikesAllowedEnum)
                        .description("Whether bikes are allowed on service journey.")
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("journeyPattern")
                        .type(journeyPatternType)
                        .dataFetcher(
                                environment -> index.patternForTrip.get(environment.getSource()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quays")
                        .description("Quays visited by service journey")
                        .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
                        .dataFetcher(environment -> index.patternForTrip
                                .get(environment.getSource()).getStops())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("passingTimes")
                        .type(new GraphQLNonNull(new GraphQLList(timetabledPassingTimeType)))
                        .description("Returns scheduled passing times only - without realtime-updates, for realtime-data use 'estimatedCalls'")
                        .dataFetcher(environment -> TripTimeShort.fromTripTimes(
                                index.patternForTrip.get((Trip) environment.getSource()).scheduledTimetable,
                                environment.getSource()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("estimatedCalls")
                        .type(new GraphQLList(estimatedCallType))
                        .description("Returns scheduled passingTimes for this ServiceJourney for a given date, updated with realtime-updates (if available). " +
                                             "NB! This takes a date as argument (default=today) and returns estimatedCalls for that date and should only be used if the date is " +
                                             "known when creating the request. For fetching estimatedCalls for a given trip.leg, use leg.serviceJourneyEstimatedCalls instead.")
                        .argument(GraphQLArgument.newArgument()
                                .name("date")
                                .type(dateScalar)
                                .description("Date to get estimated calls for. Defaults to today.")
                                .defaultValue(null)
                                .build())
                        .dataFetcher(environment -> {
                            final Trip trip = environment.getSource();

                            final ServiceDate serviceDate = mappingUtil.secondsSinceEpochToServiceDate(environment.getArgument("date"));
                            return tripTimeShortHelper.getTripTimesShort(trip, serviceDate);
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("pointsOnLink")
                        .type(linkGeometryType)
                        .description("Detailed path travelled by service journey.")
                        .dataFetcher(environment -> {
                                    LineString geometry = index.patternForTrip
                                            .get(environment.getSource())
                                            .getGeometry();
                                    if (geometry == null) {
                                        return null;
                                    }
                                    return PolylineEncoder.createEncodings(geometry);
                                }
                        )
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("notices")
                        .type(new GraphQLNonNull(new GraphQLList(noticeType)))
                        .dataFetcher(environment -> {
                                Trip trip = environment.getSource();
                                return index.getNoticesByEntity(trip);
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("Get all situations active for the service journey.")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                        .dataFetcher(dataFetchingEnvironment ->
                            graph.getSiriAlertPatchService().getTripPatches(
                            dataFetchingEnvironment.getSource()))
                    .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("keyValues")
                        .description("List of keyValue pairs for the service journey.")
                        .type(new GraphQLList(keyValueType))
                        .dataFetcher(environment -> ((Trip) environment.getSource()).getKeyValues())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("flexibleServiceType")
                        .description("Type of flexible service, or null if service is not flexible.")
                        .type(flexibleServiceTypeEnum)
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bookingArrangements")
                        .description("Booking arrangements for flexible services.")
                        .type(bookingArrangementType)
                        .build())
                .build();

        journeyPatternType = GraphQLObjectType.newObject()
                .name("JourneyPattern")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment ->   mappingUtil.toIdString(((TripPattern) environment.getSource()).getId()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("line")
                        .type(new GraphQLNonNull(lineType))
                        .dataFetcher(environment -> ((TripPattern) environment.getSource()).route)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("directionType")
                        .type(directionTypeEnum)
                        .dataFetcher(environment -> ((TripPattern) environment.getSource()).directionId)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((TripPattern) environment.getSource()).name)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("destinationDisplay")
                        .type(destinationDisplayType)
                        .deprecate("Get destinationDisplay from estimatedCall or timetabledPassingTime instead. DestinationDisplay from JourneyPattern is not correct according to model, will give misleading results in some cases and will be removed!")
                        .dataFetcher(environment -> ((TripPattern) environment.getSource()).getDirection())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourneys")
                        .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(serviceJourneyType))))
                        .dataFetcher(environment -> ((TripPattern) environment.getSource()).getTrips())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourneysForDate")
                        .description("List of service journeys for the journey pattern for a given date")
                        .argument(GraphQLArgument.newArgument()
                                .name("date")
                                .type(dateScalar)
                                .build())
                        .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(serviceJourneyType))))
                        .dataFetcher(environment -> {

                            BitSet services = index.servicesRunning(mappingUtil.secondsSinceEpochToServiceDate(environment.getArgument("date")));
                            return ((TripPattern) environment.getSource()).scheduledTimetable.tripTimes
                                    .stream()
                                    .filter(times -> services.get(times.serviceCode))
                                    .map(times -> times.trip)
                                    .collect(Collectors.toList());
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quays")
                        .description("Quays visited by service journeys for this journey patterns")
                        .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
                        .dataFetcher(environment -> ((TripPattern) environment.getSource()).getStops())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("pointsOnLink")
                        .type(linkGeometryType)
                        .dataFetcher(environment -> {
                            LineString geometry = ((TripPattern) environment.getSource()).getGeometry();
                            if (geometry == null) {
                                return null;
                            } else {
                                return PolylineEncoder.createEncodings(geometry);
                            }
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("Get all situations active for the journey pattern.")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                    .dataFetcher(dataFetchingEnvironment -> graph.getSiriAlertPatchService().getTripPatternPatches(
                        dataFetchingEnvironment.getSource()))
                    .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("notices")
                        .type(new GraphQLNonNull(new GraphQLList(noticeType)))
                        .dataFetcher(environment -> {
                            TripPattern tripPattern = environment.getSource();
                            return index.getNoticesByEntity(tripPattern);
                        })
                        .build())
                .build();

        GraphQLObjectType presentationType = GraphQLObjectType.newObject()
                .name("Presentation")
                .description("Types describing common presentation properties")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("colour")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Route) environment.getSource()).getColor())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("textColour")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Route) environment.getSource()).getTextColor())
                        .build())
                .build();

        lineType = GraphQLObjectType.newObject()
                .name("Line")
                .description("A group of routes which is generally known to the public by a similar name or number")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment ->
                                mappingUtil.toIdString(((Route) environment.getSource()).getId()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("authority")
                        .type(authorityType)
                        .dataFetcher(environment -> (((Route) environment.getSource()).getAgency()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("operator")
                        .type(operatorType)
                        .dataFetcher(environment -> (((Route) environment.getSource()).getOperator()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("organisation")
                        .deprecate("Use 'authority' instead.")
                        .type(organisationType)
                        .dataFetcher(environment -> (((Route) environment.getSource()).getAgency()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("publicCode")
                        .type(Scalars.GraphQLString)
                        .description("Publicly announced code for line, differentiating it from other lines for the same operator.")
                        .dataFetcher(environment -> (((Route) environment.getSource()).getShortName()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> (((Route) environment.getSource()).getLongName()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transportMode")
                        .type(transportModeEnum)
                        .dataFetcher(environment -> GtfsLibrary.getTraverseMode(
                                environment.getSource()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transportSubmode")
                        .type(transportSubmode)
                        .description("NOT IMPLEMENTED")
                        .dataFetcher(environment -> TransmodelTransportSubmode.UNDEFINED)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("description")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Route) environment.getSource()).getDesc())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("url")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("presentation")
                        .type(presentationType)
                        .dataFetcher(environment -> environment.getSource())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikesAllowed")
                        .type(bikesAllowedEnum)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("journeyPatterns")
                        .type(new GraphQLList(journeyPatternType))
                        .dataFetcher(environment -> index.patternsForRoute
                                .get(environment.getSource()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quays")
                        .type(new GraphQLNonNull(new GraphQLList(quayType)))
                        .dataFetcher(environment -> index.patternsForRoute
                                .get(environment.getSource())
                                .stream()
                                .map(TripPattern::getStops)
                                .flatMap(Collection::stream)
                                .distinct()
                                .collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourneys")
                        .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
                        .dataFetcher(environment -> index.patternsForRoute
                                .get(environment.getSource())
                                .stream()
                                .map(TripPattern::getTrips)
                                .flatMap(Collection::stream)
                                .distinct()
                                .collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("notices")
                        .type(new GraphQLNonNull(new GraphQLList(noticeType)))
                        .dataFetcher(environment -> {
                            Route route = environment.getSource();
                            return index.getNoticesByEntity(route);
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("Get all situations active for the line.")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                    .dataFetcher(dataFetchingEnvironment -> graph.getSiriAlertPatchService().getRoutePatches(
                        dataFetchingEnvironment.getSource()))
                    .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("flexibleLineType")
                    .description("Type of flexible line, or null if line is not flexible.")
                    .type(Scalars.GraphQLString)
                    .dataFetcher(environment -> null)
                    .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("bookingArrangements")
                    .description("Booking arrangements for flexible line.")
                    .type(bookingArrangementType)
                    .dataFetcher(environment -> null)
                    .build())
                .build();

        organisationType = GraphQLObjectType.newObject()
                .name("Organisation")
                .description("Deprecated! Replaced by authority and operator.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .description("Organisation id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment -> ((Agency) environment.getSource()).getId())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("url")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("timezone")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lang")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("phone")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("fareUrl")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lines")
                        .type(new GraphQLNonNull(new GraphQLList(lineType)))
                        .dataFetcher(environment -> index.routeForId.values()
                                .stream()
                                .filter(route -> route.getAgency() == environment.getSource())
                                .collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("Get all situations active for the organisation.")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                    .dataFetcher(dataFetchingEnvironment -> graph.getSiriAlertPatchService() .getAgencyPatches(
                        ((Agency)dataFetchingEnvironment.getSource()).getId()))
                        .build())
                .build();

        authorityType = GraphQLObjectType.newObject()
                .name("Authority")
                .description("Authority involved in public transportation. An organisation under which the responsibility of organising the transport service in a certain area is placed.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .description("Authority id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment -> ((Agency) environment.getSource()).getId())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("url")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("timezone")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lang")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("phone")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("fareUrl")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lines")
                        .type(new GraphQLNonNull(new GraphQLList(lineType)))
                        .dataFetcher(environment -> index.routeForId.values()
                                .stream()
                                .filter(route -> Objects.equals(route.getAgency(), environment.getSource()))
                                .collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("Get all situations active for the authority.")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                        .dataFetcher(dataFetchingEnvironment -> graph.getSiriAlertPatchService() .getAgencyPatches(
                            ((Agency)dataFetchingEnvironment.getSource()).getId()))
                        .build())
                .build();

        operatorType = GraphQLObjectType.newObject()
                .name("Operator")
                .description("Organisation providing public transport services.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .description("Operator id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment -> mappingUtil.toIdString(((Operator) environment.getSource()).getId()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("url")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("phone")
                        .type(Scalars.GraphQLString)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("branding")
                        .description("Branding for operator.")
                        .type(brandingType)
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lines")
                        .type(new GraphQLNonNull(new GraphQLList(lineType)))
                        .dataFetcher(environment -> index.routeForId.values()
                                .stream()
                                .filter(route -> Objects.equals(route.getOperator(), environment.getSource()))
                                .collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourney")
                        .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
                        .dataFetcher(environment -> index.tripForId.values()
                                .stream()
                                .filter(trip -> Objects.equals(trip.getOperator(), environment.getSource()))
                                .collect(Collectors.toList()))
                        .build())
                .build();

        bikeRentalStationType = GraphQLObjectType.newObject()
                .name("BikeRentalStation")
                .withInterface(placeInterface)
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).id)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).getName())
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("description")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).description)
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikesAvailable")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).bikesAvailable)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("spacesAvailable")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).spacesAvailable)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("realtimeOccupancyAvailable")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).realTimeData)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("allowDropoff")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).allowDropoff)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("networks")
                        .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
                        .dataFetcher(environment -> new ArrayList<>(((BikeRentalStation) environment.getSource()).networks))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("longitude")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).x)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("latitude")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((BikeRentalStation) environment.getSource()).y)
                        .build())
                .build();

        bikeParkType = GraphQLObjectType.newObject()
                .name("BikePark")
                .withInterface(placeInterface)
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment -> ((BikePark) environment.getSource()).id)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .dataFetcher(environment -> ((BikePark) environment.getSource()).name)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("spacesAvailable")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((BikePark) environment.getSource()).spacesAvailable)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("realtime")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((BikePark) environment.getSource()).realTimeData)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("longitude")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((BikePark) environment.getSource()).x)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("latitude")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((BikePark) environment.getSource()).y)
                        .build())
                .build();

        /*
        carParkType = GraphQLObjectType.newObject()
                .name("CarPark")
                .withInterface(placeInterface)
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment -> ((CarPark) environment.getSource()).id)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .dataFetcher(environment -> ((CarPark) environment.getSource()).name)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("capacity")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((CarPark) environment.getSource()).maxCapacity)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("spacesAvailable")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((CarPark) environment.getSource()).spacesAvailable)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("realtimeOccupancyAvailable")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((CarPark) environment.getSource()).realTimeData)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("longitude")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((CarPark) environment.getSource()).x)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("latitude")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((CarPark) environment.getSource()).y)
                        .build())
                .build();
         */

        GraphQLInputObjectType filterInputType = GraphQLInputObjectType.newInputObject()
                .name("InputFilters")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("quays")
                        .description("Quays to include by id.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("lines")
                        .description("Lines to include by id.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("bikeRentalStations")
                        .description("Bike rentals to include by id.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("bikeParks")
                        .description("Bike parks to include by id.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("carParks")
                        .description("Car parks to include by id.")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                .build();


        queryType = GraphQLObjectType.newObject()
                .name("QueryType")
                .field(tripFieldType)
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("stopPlace")
                        .description("Get a single stopPlace based on its id)")
                        .type(stopPlaceType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment ->
                            mappingUtil.getMonoOrMultiModalStation(
                                environment.getArgument("id"),
                                index.graph.stationById,
                                index.graph.multiModalStationById,
                                index.multiModalStationForStations
                            )
                        )
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("stopPlaces")
                        .description("Get all stopPlaces")
                        .type(new GraphQLNonNull(new GraphQLList(stopPlaceType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("ids")
                                .type(new GraphQLList(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> {
                            if ((environment.getArgument("ids") instanceof List)) {
                                return ((List<String>) environment.getArgument("ids"))
                                        .stream()
                                        .map(id -> mappingUtil.getMonoOrMultiModalStation(
                                            id,
                                            index.graph.stationById,
                                            index.graph.multiModalStationById,
                                            index.multiModalStationForStations
                                        ))
                                        .collect(Collectors.toList());
                            }
                            return new ArrayList<>(index.graph.stationById.values());
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("stopPlacesByBbox")
                        .description("Get all stop places within the specified bounding box")
                        .type(new GraphQLNonNull(new GraphQLList(stopPlaceType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("minimumLatitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("minimumLongitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("maximumLatitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("maximumLongitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("authority")
                                .type(Scalars.GraphQLString)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("multiModalMode")
                                .type(multiModalModeEnum)
                                .description("MultiModalMode for query. To control whether multi modal parent stop places, their mono modal children or both are included in the response." +
                                                     " Does not affect mono modal stop places that do not belong to a multi modal stop place.")
                                .defaultValue("parent")
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("filterByInUse")
                                .description("If true only stop places with at least one visiting line are included.")
                                .type(Scalars.GraphQLBoolean)
                                .defaultValue(Boolean.FALSE)
                                .build())
                        .dataFetcher(env -> {
                            Stream<Station> stations = index.graph.streetIndex.getTransitStopForEnvelope(
                                    new Envelope(
                                            new Coordinate(
                                                    env.getArgument("minimumLongitude"),
                                                    env.getArgument("minimumLatitude")
                                            ),
                                            new Coordinate(
                                                    env.getArgument("maximumLongitude"),
                                                    env.getArgument("maximumLatitude")
                                            )
                                    )
                            )
                                .stream()
                                .map(TransitStopVertex::getStop)
                                .map(quay -> quay.getParentStation())
                                .filter(Objects::nonNull)
                                .distinct()
                                .filter(station -> {
                                    String authority = env.getArgument("authority");
                                    return authority == null || station.getId().getFeedId().equalsIgnoreCase(authority);
                                });

                                if (Boolean.TRUE.equals(env.getArgument("filterByInUse"))){
                                    stations = stations.filter(this::isStopPlaceInUse);
                                }
                                return stations.distinct().collect(Collectors.toList());

                                /*
                                String multiModalMode=environment.getArgument("multiModalMode");
                                if ("parent".equals(multiModalMode)){
                                    stops = stops.map(s -> getParentStopPlace(s).orElse(s));
                                }
                                List<Stop> stopList=stops.distinct().collect(Collectors.toList());
                                if ("all".equals(multiModalMode)) {
                                    stopList.addAll(stopList.stream().map(s -> getParentStopPlace(s).orElse(null)).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
                                }
                                return stopList;
                                */
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quay")
                        .description("Get a single quay based on its id)")
                        .type(quayType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> index.stopForId
                                .get(mappingUtil.fromIdString(environment.getArgument("id"))))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quays")
                        .description("Get all quays")
                        .type(new GraphQLNonNull(new GraphQLList(quayType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("ids")
                                .type(new GraphQLList(Scalars.GraphQLString))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("name")
                                .type(Scalars.GraphQLString)
                                .build())
                        .dataFetcher(environment -> {
                            if ((environment.getArgument("ids") instanceof List)) {
                                if (environment.getArguments().entrySet()
                                        .stream()
                                        .filter(stringObjectEntry -> stringObjectEntry.getValue() != null)
                                        .collect(Collectors.toList())
                                        .size() != 1) {
                                    throw new IllegalArgumentException("Unable to combine other filters with ids");
                                }
                                return ((List<String>) environment.getArgument("ids"))
                                        .stream()
                                        .map(id -> index.stopForId.get(mappingUtil.fromIdString(id)))
                                        .collect(Collectors.toList());
                            }
                            if (environment.getArgument("name") == null) {
                                return index.stopForId.values();
                            }
                            /*
                            else {
                                return index.getLuceneIndex().query(environment.getArgument("name"), true, true, false)
                                        .stream()
                                        .map(result -> index.stopForId.get(mappingUtil.fromIdString(result.id)));
                            }
                             */
                            return emptyList();
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quaysByBbox")
                        .description("Get all quays within the specified bounding box")
                        .type(new GraphQLNonNull(new GraphQLList(quayType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("minimumLatitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("minimumLongitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("maximumLatitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("maximumLongitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("authority")
                                .type(Scalars.GraphQLString)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("filterByInUse")
                                .description("If true only quays with at least one visiting line are included.")
                                .type(Scalars.GraphQLBoolean)
                                .defaultValue(Boolean.FALSE)
                                .build())
                        .dataFetcher(environment -> index.graph.streetIndex
                                .getTransitStopForEnvelope(new Envelope(
                                        new Coordinate(environment.getArgument("minimumLongitude"),
                                                environment.getArgument("minimumLatitude")),
                                        new Coordinate(environment.getArgument("maximumLongitude"),
                                                environment.getArgument("maximumLatitude"))))
                                .stream()
                                .map(TransitStopVertex::getStop)
                                .filter(stop -> environment.getArgument("authority") == null ||
                                        stop.getId().getFeedId().equalsIgnoreCase(environment.getArgument("authority")))
                                .filter(stop -> !Boolean.TRUE.equals(environment.getArgument("filterByInUse"))
                                                        || !index.getPatternsForStop(stop,true).isEmpty())
                                .collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quaysByRadius")
                        .description(
                                "Get all quays within the specified radius from a location. The returned type has two fields quay and distance")
                        .type(relay.connectionType("quayAtDistance",
                                relay.edgeType("quayAtDistance", quayAtDistance, null, new ArrayList<>()),
                                new ArrayList<>()))
                        .argument(GraphQLArgument.newArgument()
                                .name("latitude")
                                .description("Latitude of the location")
                                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("longitude")
                                .description("Longitude of the location")
                                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("radius")
                                .description("Radius (in meters) to search for from the specified location")
                                .type(new GraphQLNonNull(Scalars.GraphQLInt))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("authority")
                                .type(Scalars.GraphQLString)
                                .build())
                        .argument(relay.getConnectionFieldArguments())
                        .dataFetcher(environment -> {
                            List<GraphIndex.StopAndDistance> stops;
                            try {
                                stops = index.findClosestStopsByWalking(
                                        environment.getArgument("latitude"),
                                        environment.getArgument("longitude"),
                                        environment.getArgument("radius"))
                                        .stream()
                                        .filter(stopAndDistance -> environment.getArgument("authority") == null ||
                                                stopAndDistance.stop.getId().getFeedId()
                                                        .equalsIgnoreCase(environment.getArgument("authority")))
                                        .sorted(Comparator.comparing(s -> s.distance))
                                        .collect(Collectors.toList());
                            } catch (VertexNotFoundException e) {
                                LOG.warn("findClosestPlacesByWalking failed with exception, returning empty list of places. " , e);
                                stops = Collections.emptyList();
                            }

                            if (CollectionUtils.isEmpty(stops)) {
                                return new DefaultConnection<>(Collections.emptyList(), new DefaultPageInfo(null, null, false, false));
                            }
                            return new SimpleListConnection(stops).get(environment);
                        })
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("nearest")
                        .description(
                                "Get all places (quays, stop places, car parks etc. with coordinates) within the specified radius from a location. The returned type has two fields place and distance. The search is done by walking so the distance is according to the network of walkables.")
                        .type(relay.connectionType("placeAtDistance",
                                relay.edgeType("placeAtDistance", placeAtDistanceType, null, new ArrayList<>()),
                                new ArrayList<>()))
                        .argument(GraphQLArgument.newArgument()
                                .name("latitude")
                                .description("Latitude of the location")
                                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("longitude")
                                .description("Longitude of the location")
                                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("maximumDistance")
                                .description("Maximum distance (in meters) to search for from the specified location. Default is 2000m.")
                                .defaultValue(2000)
                                .type(Scalars.GraphQLInt)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("maximumResults")
                                .description("Maximum number of results. Search is stopped when this limit is reached. Default is 20.")
                                .defaultValue(20)
                                .type(Scalars.GraphQLInt)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("filterByPlaceTypes")
                                .description("Only include places of given types if set. Default accepts all types")
                                .defaultValue(Arrays.asList(TransmodelPlaceType.values()))
                                .type(new GraphQLList(filterPlaceTypeEnum))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("filterByModes")
                                .description("Only include places that include this mode. Only checked for places with mode i.e. quays, departures.")
                                .type(new GraphQLList(modeEnum))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("filterByInUse")
                                .description("Only affects queries for quays and stop places. If true only quays and stop places with at least one visiting line are included.")
                                .type(Scalars.GraphQLBoolean)
                                .defaultValue(Boolean.FALSE)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("filterByIds")
                                .description("Only include places that match one of the given ids.")
                                .type(filterInputType)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("multiModalMode")
                                .type(multiModalModeEnum)
                                .description("MultiModalMode for query. To control whether multi modal parent stop places, their mono modal children or both are included in the response." +
                                                     " Does not affect mono modal stop places that do not belong to a multi modal stop place. Only applicable for placeType StopPlace")
                                .defaultValue("parent")
                                .build())
                        .argument(relay.getConnectionFieldArguments())
                        .dataFetcher(environment -> {
                            List<FeedScopedId> filterByStops = null;
                            List<FeedScopedId> filterByRoutes = null;
                            List<String> filterByBikeRentalStations = null;
                            List<String> filterByBikeParks = null;
                            List<String> filterByCarParks = null;
                            @SuppressWarnings("rawtypes")
                            Map filterByIds = environment.getArgument("filterByIds");
                            if (filterByIds != null) {
                                filterByStops = toIdList(((List<String>) filterByIds.get("quays")));
                                filterByRoutes = toIdList(((List<String>) filterByIds.get("lines")));
                                filterByBikeRentalStations = filterByIds.get("bikeRentalStations") != null ? (List<String>) filterByIds.get("bikeRentalStations") : Collections.emptyList();
                                filterByBikeParks = filterByIds.get("bikeParks") != null ? (List<String>) filterByIds.get("bikeParks") : Collections.emptyList();
                                filterByCarParks = filterByIds.get("carParks") != null ? (List<String>) filterByIds.get("carParks") : Collections.emptyList();
                            }

                            List<TraverseMode> filterByTransportModes = environment.getArgument("filterByModes");
                            List<TransmodelPlaceType> placeTypes = environment.getArgument("filterByPlaceTypes");
                            if (CollectionUtils.isEmpty(placeTypes)) {
                                placeTypes = Arrays.asList(TransmodelPlaceType.values());
                            }
                            List<GraphIndex.PlaceType> filterByPlaceTypes = mappingUtil.mapPlaceTypes(placeTypes);

                            // Need to fetch more than requested no of places if stopPlaces are allowed, as this requires fetching potentially multiple quays for the same stop place and mapping them to unique stop places.
                            int orgMaxResults = environment.getArgument("maximumResults");
                            int maxResults = orgMaxResults;
                            if (placeTypes != null && placeTypes.contains(TransmodelPlaceType.STOP_PLACE)) {
                                maxResults *= 5;
                            }

                            List<GraphIndex.PlaceAndDistance> places;
                            try {
                                places = index.findClosestPlacesByWalking(
                                        environment.getArgument("latitude"),
                                        environment.getArgument("longitude"),
                                        environment.getArgument("maximumDistance"),
                                        maxResults,
                                        filterByTransportModes,
                                        filterByPlaceTypes,
                                        filterByStops,
                                        filterByRoutes,
                                        filterByBikeRentalStations,
                                        filterByBikeParks,
                                        filterByCarParks,
                                        environment.getArgument("filterByInUse")
                                );
                            } catch (VertexNotFoundException e) {
                                LOG.warn("findClosestPlacesByWalking failed with exception, returning empty list of places. " , e);
                                places = Collections.emptyList();
                            }

                            places = convertQuaysToStopPlaces(placeTypes, places,  environment.getArgument("multiModalMode")).stream().limit(orgMaxResults).collect(Collectors.toList());
                            if (CollectionUtils.isEmpty(places)) {
                                return new DefaultConnection<>(Collections.emptyList(), new DefaultPageInfo(null, null, false, false));
                            }
                            return new SimpleListConnection(places).get(environment);
                        })
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("authority")
                        .description("Get an authority by ID")
                        .type(authorityType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment ->
                                index.getAgencyWithoutFeedId(environment.getArgument("id")))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("authorities")
                        .description("Get all authorities")
                        .type(new GraphQLNonNull(new GraphQLList(authorityType)))
                        .dataFetcher(environment -> new ArrayList<>(index.getAllAgencies()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("operator")
                        .description("Get a operator by ID")
                        .type(operatorType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment ->
                                index.operatorForId.get(
                                        mappingUtil.fromIdString(environment.getArgument("id"))
                                ))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("operators")
                        .description("Get all operators")
                        .type(new GraphQLNonNull(new GraphQLList(operatorType)))
                        .dataFetcher(environment -> new ArrayList<>(index.getAllOperators()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("organisation")
                        .deprecate("Use 'authority' instead.")
                        .type(organisationType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment ->
                                index.getAgencyWithoutFeedId(environment.getArgument("id")))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("organisations")
                        .deprecate("Use 'authorities' instead.")
                        .type(new GraphQLNonNull(new GraphQLList(organisationType)))
                        .dataFetcher(environment -> new ArrayList<>(index.getAllAgencies()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("line")
                        .description("Get a single line based on its id")
                        .type(lineType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> index.routeForId
                                .get(mappingUtil.fromIdString(environment.getArgument("id"))))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lines")
                        .description("Get all lines")
                        .type(new GraphQLNonNull(new GraphQLList(lineType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("ids")
                                .type(new GraphQLList(Scalars.GraphQLString))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("name")
                                .type(Scalars.GraphQLString)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("publicCode")
                                .type(Scalars.GraphQLString)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("publicCodes")
                                .type(new GraphQLList(Scalars.GraphQLString))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("transportModes")
                                .type(new GraphQLList(transportModeEnum))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("authorities")
                                .description("Set of ids of authorities to fetch lines for.")
                                .type(new GraphQLList(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> {
                            if ((environment.getArgument("ids") instanceof List)) {
                                if (environment.getArguments().entrySet()
                                        .stream()
                                        .filter(stringObjectEntry -> stringObjectEntry.getValue() != null)
                                        .collect(Collectors.toList())
                                        .size() != 1) {
                                    throw new IllegalArgumentException("Unable to combine other filters with ids");
                                }
                                return ((List<String>) environment.getArgument("ids"))
                                        .stream()
                                        .map(id -> index.routeForId.get(mappingUtil.fromIdString(id)))
                                        .collect(Collectors.toList());
                            }
                            Stream<Route> stream = index.routeForId.values().stream();
                            if (environment.getArgument("name") != null) {
                                stream = stream
                                        .filter(route -> route.getLongName() != null)
                                        .filter(route -> route.getLongName().toLowerCase().startsWith(
                                                ((String) environment.getArgument("name")).toLowerCase())
                                        );
                            }
                            if (environment.getArgument("publicCode") != null) {
                                stream = stream
                                        .filter(route -> route.getShortName() != null)
                                        .filter(route -> route.getShortName().equals(environment.getArgument("publicCode")));
                            }
                            if (environment.getArgument("publicCodes") instanceof List) {
                                Set<String> publicCodes = new HashSet<>((List)environment.getArgument("publicCodes"));
                                stream = stream
                                                 .filter(route -> route.getShortName() != null)
                                                 .filter(route -> publicCodes.contains(route.getShortName()));
                            }
                            if (environment.getArgument("transportModes") != null) {

                                Set<TraverseMode> modes = ((List<TraverseMode>) environment.getArgument("transportModes")).stream()
                                        .filter(TraverseMode::isTransit)
                                        .collect(Collectors.toSet());
                                stream = stream
                                        .filter(route ->
                                                modes.contains(GtfsLibrary.getTraverseMode(route)));
                            }
                            if ((environment.getArgument("authorities") instanceof Collection)) {
                                Collection<String> authorityIds = environment.getArgument("authorities");
                                stream = stream
                                        .filter(route ->
                                                route.getAgency() != null && authorityIds.contains(route.getAgency().getId()));
                            }
                            return stream.collect(Collectors.toList());
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourney")
                        .description("Get a single service journey based on its id")
                        .type(serviceJourneyType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> index.tripForId
                                .get(mappingUtil.fromIdString(environment.getArgument("id"))))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourneys")
                        .description("Get all service journeys")
                        .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("lines")
                                .description("Set of ids of lines to fetch serviceJourneys for.")
                                .type(new GraphQLList(Scalars.GraphQLString))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("privateCodes")
                                .description("Set of ids of private codes to fetch serviceJourneys for.")
                                .type(new GraphQLList(Scalars.GraphQLString))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("activeDates")
                                .description("Set of ids of active dates to fetch serviceJourneys for.")
                                .type(new GraphQLList(dateScalar))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("authorities")
                                .description("Set of ids of authorities to fetch serviceJourneys for.")
                                .type(new GraphQLList(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> {
                                List<String> lineIds=environment.getArgument("lines");
                                //List<String> privateCodes=environment.getArgument("privateCodes");
                                List<Long> activeDates=environment.getArgument("activeDates");
                                List<String> authorities=environment.getArgument("authorities");
                                return index.tripForId.values().stream()
                                        .filter(t -> CollectionUtils.isEmpty(lineIds) || lineIds.contains(t.getRoute().getId().getId()))
                                        //.filter(t -> CollectionUtils.isEmpty(privateCodes) || privateCodes.contains(t.getTripPrivateCode()))
                                        .filter(t -> CollectionUtils.isEmpty(authorities) || authorities.contains(t.getRoute().getAgency().getId()))
                                        .filter(t -> CollectionUtils.isEmpty(activeDates) || index.graph.getCalendarService().getServiceDatesForServiceId(t.getServiceId()).stream().anyMatch(sd -> activeDates.contains(mappingUtil.serviceDateToSecondsSinceEpoch(sd))))
                                        .collect(Collectors.toList());
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalStations")
                        .description("Get a single bike rental station based on its id")
                        .type(new GraphQLNonNull(new GraphQLList(bikeRentalStationType)))
                        .dataFetcher(dataFetchingEnvironment -> new ArrayList<>(index.graph.getService(BikeRentalStationService.class).getBikeRentalStations()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalStation")
                        .description("Get all bike rental stations")
                        .type(bikeRentalStationType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> index.graph.getService(BikeRentalStationService.class)
                                .getBikeRentalStations()
                                .stream()
                                .filter(bikeRentalStation -> bikeRentalStation.id.equals(environment.getArgument("id")))
                                .findFirst()
                                .orElse(null))
                        .build())

                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalStationsByBbox")
                        .description("Get all bike rental stations within the specified bounding box. NOT IMPLEMENTED")
                        .type(new GraphQLNonNull(new GraphQLList(bikeRentalStationType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("minimumLatitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("minimumLongitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("maximumLatitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("maximumLongitude")
                                .type(Scalars.GraphQLFloat)
                                .build())
                        .dataFetcher(environment -> Collections.emptyList())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikePark")
                        .description("Get a single bike park based on its id")
                        .type(bikeParkType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> index.graph.getService(BikeRentalStationService.class)
                                .getBikeParks()
                                .stream()
                                .filter(bikePark -> bikePark.id.equals(environment.getArgument("id")))
                                .findFirst()
                                .orElse(null))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeParks")
                        .description("Get all bike parks")
                        .type(new GraphQLNonNull(new GraphQLList(bikeParkType)))
                        .dataFetcher(dataFetchingEnvironment -> new ArrayList<>(index.graph.getService(BikeRentalStationService.class).getBikeParks()))
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("carPark")
                        .description("Get a single car park based on its id")
                        .type(carParkType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> index.graph.getService(CarParkService.class)
                                .getCarParks()
                                .stream()
                                .filter(carPark -> carPark.id.equals(environment.getArgument("id")))
                                .findFirst()
                                .orElse(null))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("carParks")
                        .description("Get all car parks")
                        .type(new GraphQLNonNull(new GraphQLList(carParkType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("ids")
                                .type(new GraphQLList(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> {
                            if ((environment.getArgument("ids") instanceof List)) {
                                Map<String, CarPark> carParks = index.graph.getService(CarParkService.class).getCarParkById();
                                return ((List<String>) environment.getArgument("ids"))
                                        .stream()
                                        .map(carParks::get)
                                        .collect(Collectors.toList());
                            }
                            return new ArrayList<>(index.graph.getService(CarParkService.class).getCarParks());
                        })
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("routingParameters")
                        .description("Get default routing parameters.")
                        .type(routingParametersType)
                        .dataFetcher(environment -> getDefaultRoutingRequest())
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("notices")
                        .description("Get all notices")
                        .type(new GraphQLNonNull(new GraphQLList(noticeType)))
                        .dataFetcher(environment -> index.getNoticesByEntity().values())
                        .build())
                */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("Get all active situations.")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                        .argument(GraphQLArgument.newArgument()
                                .name("authorities")
                                .description("Filter by reporting authorities.")
                                .type(new GraphQLList(Scalars.GraphQLString))
                                .build())
                        .argument(GraphQLArgument.newArgument()
                                .name("severities")
                                .description("Filter by severity.")
                                .type(new GraphQLList(severityEnum))
                                .build())
                        .dataFetcher(environment -> {
                            Collection<AlertPatch> alerts = graph.getSiriAlertPatchService().getAllAlertPatches();
                            if ((environment.getArgument("authorities") instanceof List)) {
                                List<String> authorities = environment.getArgument("authorities");
                                alerts = alerts.stream().filter(alertPatch -> authorities.contains(alertPatch.getFeedId())).collect(Collectors.toSet());
                            }
                            if ((environment.getArgument("severities") instanceof List)) {
                                List<String> severities = environment.getArgument("severities");
                                alerts = alerts.stream().filter(alertPatch -> severities.contains(alertPatch.getAlert().severity)).collect(Collectors.toSet());
                            }
                            return alerts;
                        })
                        .build())
                .build();

        Set<GraphQLType> dictionary = new HashSet<>();
        dictionary.add(placeInterface);

        indexSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build(dictionary);
    }

    /*
    private BookingArrangement getBookingArrangementForTripTimeShort(TripTimeShort tripTimeShort) {
        Trip trip = index.tripForId.get(tripTimeShort.tripId);
        if (trip == null) {
            return null;
        }
        TripPattern tripPattern = index.patternForTrip.get(trip);
        if (tripPattern == null || tripPattern.stopPattern == null) {
            return null;
        }
        return tripPattern.stopPattern.bookingArrangements[tripTimeShort.stopIndex];
    }
    */

    /**
     * Resolves all AlertPatches that are relevant for the supplied TripTimeShort.
     *
     * @param tripTimeShort
     * @return
     */

    private Collection<AlertPatch> getAllRelevantAlerts(TripTimeShort tripTimeShort) {
        FeedScopedId tripId = tripTimeShort.tripId;
        Trip trip = index.tripForId.get(tripId);
        FeedScopedId routeId = trip.getRoute().getId();

        FeedScopedId stopId = tripTimeShort.stopId;

        Stop stop = index.stopForId.get(stopId);
        FeedScopedId parentStopId = stop.getParentStation().getId();

        Collection<AlertPatch> allAlerts = new HashSet<>();

        AlertPatchService alertPatchService = index.graph.getSiriAlertPatchService();

        // Quay
        allAlerts.addAll(alertPatchService.getStopPatches(stopId));
        allAlerts.addAll(alertPatchService.getStopAndTripPatches(stopId, tripId));
        allAlerts.addAll(alertPatchService.getStopAndRoutePatches(stopId, routeId));
        // StopPlace
        allAlerts.addAll(alertPatchService.getStopPatches(parentStopId));
        allAlerts.addAll(alertPatchService.getStopAndTripPatches(parentStopId, tripId));
        allAlerts.addAll(alertPatchService.getStopAndRoutePatches(parentStopId, routeId));
        // Trip
        allAlerts.addAll(alertPatchService.getTripPatches(tripId));
        // Route
        allAlerts.addAll(alertPatchService.getRoutePatches(routeId));
        // Agency
        // TODO OTP2 This should probably have a FeedScopeId argument instead of string
        allAlerts.addAll(alertPatchService.getAgencyPatches(trip.getRoute().getAgency().getId()));
        // TripPattern
        allAlerts.addAll(alertPatchService.getTripPatternPatches(index.patternForTrip.get(trip)));

        long serviceDayMillis = 1000 * tripTimeShort.serviceDay;
        long arrivalMillis = 1000 * tripTimeShort.realtimeArrival;
        long departureMillis = 1000 * tripTimeShort.realtimeDeparture;

        filterSituationsByDateAndStopConditions(allAlerts,
                new Date(serviceDayMillis + arrivalMillis),
                new Date(serviceDayMillis + departureMillis),
                Arrays.asList(StopCondition.STOP, StopCondition.START_POINT, StopCondition.EXCEPTIONAL_STOP));

        return allAlerts;
    }



    private static void filterSituationsByDateAndStopConditions(Collection<AlertPatch> alertPatches, Date fromTime, Date toTime, List<StopCondition> stopConditions) {
        if (alertPatches != null) {

            // First and last period
            alertPatches.removeIf(alert -> alert.getAlert().effectiveStartDate.after(toTime) ||
                    (alert.getAlert().effectiveEndDate != null && alert.getAlert().effectiveEndDate.before(fromTime)));

            // Handle repeating validityPeriods
            alertPatches.removeIf(alertPatch -> !alertPatch.displayDuring(fromTime.getTime()/1000, toTime.getTime()/1000));

            alertPatches.removeIf(alert -> {
                boolean removeByStopCondition = false;

                if (!alert.getStopConditions().isEmpty()) {
                    removeByStopCondition = true;
                    for (StopCondition stopCondition : stopConditions) {
                        if (alert.getStopConditions().contains(stopCondition)) {
                            removeByStopCondition = false;
                        }
                    }
                }
                return removeByStopCondition;
            });
        }
    }


    /**
     * Create PlaceAndDistance objects for all unique stopPlaces according to specified multiModalMode if client has requested stopPlace type.
     *
     * Necessary because nearest does not support StopPlace (stations), so we need to fetch quays instead and map the response.
     *
     * Remove PlaceAndDistance objects for quays if client has not requested these.
     */
    /*
    private List<GraphIndex.PlaceAndDistance> convertQuaysToStopPlaces(List<TransmodelPlaceType> placeTypes, List<GraphIndex.PlaceAndDistance> places, String multiModalMode) {
        if (placeTypes==null || placeTypes.contains(TransmodelPlaceType.STOP_PLACE)) {
            // Convert quays to stop places
            List<GraphIndex.PlaceAndDistance> stations = places.stream().filter(p -> p.place instanceof Stop)
                                                                 .map(p -> new GraphIndex.PlaceAndDistance(index.stationForId.get(((Stop) p.place).getParentStationId()), p.distance))
                                                                 .filter(Objects::nonNull).collect(Collectors.toList());

            if ("parent".equals(multiModalMode)) {
                // Replace monomodal children with their multimodal parents
                stations = stations.stream().map(p -> new GraphIndex.PlaceAndDistance(getParentStopPlace((Stop) p.place).orElse((Stop) p.place), p.distance)).collect(Collectors.toList());
            }
            if ("all".equals(multiModalMode)) {
                // Add multimodal parents in addition to their monomodal children
                places.addAll(stations.stream().map(p -> new GraphIndex.PlaceAndDistance(getParentStopPlace((Stop) p.place).orElse(null), p.distance)).filter(p -> p.place != null).collect(Collectors.toList()));
            }

            places.addAll(stations);

            if (placeTypes != null && !placeTypes.contains(TransmodelPlaceType.QUAY)) {
                // Remove quays if only stop places are requested
                places = places.stream().filter(p -> !(p.place instanceof Stop && ((Stop) p.place).getLocationType() == 0)).collect(Collectors.toList());
            }

        }
        Collections.sort(places, Comparator.comparing(GraphIndex.PlaceAndDistance::getDistance));

        Set<Object> uniquePlaces= new HashSet<>();
        return places.stream().filter(s -> uniquePlaces.add(s.place)).collect(Collectors.toList());
    }
     */


    private RoutingRequest getDefaultRoutingRequest() {
        return new RoutingRequest();
    }

    private List<FeedScopedId> toIdList(List<String> ids) {
        if (ids == null) return Collections.emptyList();
        return ids.stream().map(id -> mappingUtil.fromIdString(id)).collect(Collectors.toList());
    }

    private void createPlanType() {
        final GraphQLObjectType placeType = GraphQLObjectType.newObject()
                .name("Place")
                .description("Common super class for all places (stop places, quays, car parks, bike parks and bike rental stations )")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .description("For transit quays, the name of the quay. For points of interest, the name of the POI.")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Place) environment.getSource()).name)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("vertexType")
                        .description("Type of vertex. (Normal, Bike sharing station, Bike P+R, Transit quay) Mostly used for better localization of bike sharing and P+R station names")
                        .type(vertexTypeEnum)
                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("latitude")
                        .description("The latitude of the place.")
                        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                        .dataFetcher(environment -> ((Place) environment.getSource()).lat)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("longitude")
                        .description("The longitude of the place.")
                        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                        .dataFetcher(environment -> ((Place) environment.getSource()).lon)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quay")
                        .description("The quay related to the place.")
                        .type(quayType)
                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.TRANSIT) ? index.stopForId.get(((Place) environment.getSource()).stopId) : null)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalStation")
                        .type(bikeRentalStationType)
                        .description("The bike rental station related to the place")
                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.BIKESHARE) ?
                                index.graph.getService(BikeRentalStationService.class)
                                        .getBikeRentalStations()
                                        .stream()
                                        .filter(bikeRentalStation -> bikeRentalStation.id.equals(((Place) environment.getSource()).bikeShareId))
                                        .findFirst()
                                        .orElse(null)
                                : null)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikePark")
                        .type(bikeParkType)
                        .description("The bike parking related to the place")
                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.BIKEPARK) ?
                                index.graph.getService(BikeRentalStationService.class)
                                        .getBikeParks()
                                        .stream()
                                        .filter(bikePark -> bikePark.id.equals(((Place) environment.getSource()).bikeParkId))
                                        .findFirst()
                                        .orElse(null)
                                : null)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("carPark")
                        .type(carParkType)
                        .description("The car parking related to the place")
                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.PARKANDRIDE) ?
                                index.graph.getService(CarParkService.class)
                                        .getCarParks()
                                        .stream()
                                        .filter(carPark -> carPark.id.equals(((Place) environment.getSource()).carParkId))
                                        .findFirst()
                                        .orElse(null)
                                : null)
                        .build())
                 */
                .build();


        final GraphQLObjectType pathGuidanceType = GraphQLObjectType.newObject()
                .name("PathGuidance")
                .description("A series of turn by turn instructions used for walking, biking and driving.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("distance")
                        .description("The distance in meters that this step takes.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).distance)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("relativeDirection")
                        .description("The relative direction of this step.")
                        .type(relativeDirectionEnum)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).relativeDirection)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("streetName")
                        .description("The name of the street.")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).streetName)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("heading")
                        .description("The absolute direction of this step.")
                        .type(absoluteDirectionEnum)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).absoluteDirection)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("exit")
                        .description("When exiting a highway or traffic circle, the exit name/number.")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).exit)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("stayOn")
                        .description("Indicates whether or not a street changes direction at an intersection.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).stayOn)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("area")
                        .description("This step is on an open area, such as a plaza or train platform, and thus the directions should say something like \"cross\"")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).area)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bogusName")
                        .description("The name of this street was generated by the system, so we should only display it once, and generally just display right/left directions")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).bogusName)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("latitude")
                        .description("The latitude of the step.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).lat)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("longitude")
                        .description("The longitude of the step.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).lon)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("legStepText")
                        .description("Direction information as readable text.")
                        .type(Scalars.GraphQLString)
                        .argument(GraphQLArgument.newArgument()
                                .name("locale")
                                .type(localeEnum)
                                .defaultValue("no")
                                .build())
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).getLegStepText(environment))
                        .build())
                 */
                .build();

        final GraphQLObjectType legType = GraphQLObjectType.newObject()
                .name("Leg")
                .description("Part of a trip pattern. Either a ride on a public transport vehicle or access or path link to/from/between places")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("startTime")
                        .description("The date and time this leg begins.")
                        .deprecate("Replaced with expectedStartTime")
                        .type(dateTimeScalar)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).startTime.getTime().getTime())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("endTime")
                        .description("The date and time this leg ends.")
                        .deprecate("Replaced with expectedEndTime")
                        .type(dateTimeScalar)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).endTime.getTime().getTime())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("aimedStartTime")
                        .description("The aimed date and time this leg starts.")
                        .type(dateTimeScalar)
                        .dataFetcher(// startTime is already adjusted for realtime - need to subtract delay to get aimed time
                                environment -> ((Leg) environment.getSource()).startTime.getTimeInMillis() -
                                        (1000 * ((Leg) environment.getSource()).departureDelay))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("expectedStartTime")
                        .description("The expected, realtime adjusted date and time this leg starts.")
                        .type(dateTimeScalar)
                        .dataFetcher(
                                environment -> ((Leg) environment.getSource()).startTime.getTimeInMillis())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("aimedEndTime")
                        .description("The aimed date and time this leg ends.")
                        .type(dateTimeScalar)
                        .dataFetcher(// endTime is already adjusted for realtime - need to subtract delay to get aimed time
                                environment -> ((Leg) environment.getSource()).endTime.getTimeInMillis() -
                                        (1000 * ((Leg) environment.getSource()).arrivalDelay))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("expectedEndTime")
                        .description("The expected, realtime adjusted date and time this leg ends.")
                        .type(dateTimeScalar)
                        .dataFetcher(
                                environment -> ((Leg) environment.getSource()).endTime.getTimeInMillis())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("mode")
                        .description("The mode of transport or access (e.g., foot) used when traversing this leg.")
                        .type(modeEnum)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).mode)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transportSubmode")
                        .description("The transport sub mode (e.g., localBus or expressBus) used when traversing this leg. Null if leg is not a ride")
                        .type(transportSubmode)
                        .dataFetcher(environment -> TransmodelTransportSubmode.UNDEFINED)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("duration")
                        .description("The legs's duration in seconds")
                        .type(Scalars.GraphQLLong)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).getDuration())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("directDuration")
                    .type(Scalars.GraphQLLong)
                    .description("NOT IMPLEMENTED")
                    .dataFetcher(environment -> ((Leg) environment.getSource()).getDuration())
                    .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("pointsOnLink")
                        .description("The legs's geometry.")
                        .type(linkGeometryType)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).legGeometry)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("authority")
                        .description("For ride legs, the service authority used for this legs. For non-ride legs, null.")
                        .type(authorityType)
                        .dataFetcher(environment -> getAgency(((Leg) environment.getSource()).agencyId))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("operator")
                        .description("For ride legs, the operator used for this legs. For non-ride legs, null.")
                        .type(operatorType)
                        .dataFetcher(
                                environment -> {
                                    FeedScopedId tripId = ((Leg) environment.getSource()).tripId;
                                    return tripId == null ? null : index.tripForId.get(tripId).getOperator();
                                }
                        )
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("organisation")
                        .description("For ride legs, the transit organisation that operates the service used for this legs. For non-ride legs, null.")
                        .deprecate("Use 'authority' instead.")
                        .type(organisationType)
                        .dataFetcher(environment -> getAgency(((Leg) environment.getSource()).agencyId))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("realTime")
                        .description("Whether there is real-time data about this leg")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).realTime)
                        .deprecate("Should not be camelCase. Use realtime instead.")
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("realtime")
                        .description("Whether there is real-time data about this leg")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).realTime)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("distance")
                        .description("The distance traveled while traversing the leg in meters.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).distanceMeters)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("ride")
                        .description("Whether this leg is a ride leg or not.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).isTransitLeg())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("rentedBike")
                        .description("Whether this leg is with a rented bike.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).rentedBike)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("fromPlace")
                        .description("The Place where the leg originates.")
                        .type(new GraphQLNonNull(placeType))
                        .dataFetcher(environment -> ((Leg) environment.getSource()).from)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("toPlace")
                        .description("The Place where the leg ends.")
                        .type(new GraphQLNonNull(placeType))
                        .dataFetcher(environment -> ((Leg) environment.getSource()).to)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("fromEstimatedCall")
                        .description("EstimatedCall for the quay where the leg originates. NOT IMPLEMENTED")
                        .type(estimatedCallType)
                        .dataFetcher(environment -> null)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("toEstimatedCall")
                        .description("EstimatedCall for the quay where the leg ends. NOT IMPLEMENTED")
                        .type(estimatedCallType)
                        .dataFetcher(environment -> null)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("line")
                        .description("For ride legs, the line. For non-ride legs, null.")
                        .type(lineType)
                        .dataFetcher(environment -> index.routeForId.get(((Leg) environment.getSource()).routeId))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourney")
                        .description("For ride legs, the service journey. For non-ride legs, null.")
                        .type(serviceJourneyType)
                        .dataFetcher(environment -> index.tripForId.get(((Leg) environment.getSource()).tripId))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("intermediateQuays")
                        .description("For ride legs, intermediate quays between the Place where the leg originates and the Place where the leg ends. For non-ride legs, empty list.")
                        .type(new GraphQLNonNull(new GraphQLList(quayType)))
                        .dataFetcher(environment -> {
                                List<Place> stops = ((Leg) environment.getSource()).intermediateStops;
                                if (stops == null) {
                                    return new ArrayList<>();
                                }
                                else {
                                    return (stops.stream()
                                            .filter(place -> place.stopId != null)
                                            .map(placeWithStop -> index.stopForId.get(placeWithStop.stopId))
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList()));
                                }
                            })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("intermediateEstimatedCalls")
                        .description("For ride legs, estimated calls for quays between the Place where the leg originates and the Place where the leg ends. For non-ride legs, empty list.")
                        .type(new GraphQLNonNull(new GraphQLList(estimatedCallType)))
                        .dataFetcher(environment -> new ArrayList<>())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourneyEstimatedCalls")
                        .description("For ride legs, all estimated calls for the service journey. For non-ride legs, empty list.")
                        .type(new GraphQLNonNull(new GraphQLList(estimatedCallType)))
                        .dataFetcher(environment -> tripTimeShortHelper.getAllTripTimeShortsForLegsTrip(environment.getSource()))
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("via")
                        .description("Do we continue from a specified via place")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).intermediatePlace)
                        .build())
                */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("All relevant situations for this leg")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                        .dataFetcher(environment -> ((Leg) environment.getSource()).alertPatches)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("steps")
                        .description("Do we continue from a specified via place")
                        .type(new GraphQLNonNull(new GraphQLList(pathGuidanceType)))
                        .dataFetcher(environment -> ((Leg) environment.getSource()).walkSteps)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("interchangeFrom")
                        .description("NOT IMPLEMENTED")
                        .type(interchangeType)
                        .dataFetcher(environment -> null)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("interchangeTo")
                        .description("NOT IMPLEMENTED")
                        .type(interchangeType)
                        .dataFetcher(environment -> null)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bookingArrangements")
                        .type(bookingArrangementType)
                        .build())
                .build();


        final GraphQLObjectType tripPatternType = GraphQLObjectType.newObject()
                .name("TripPattern")
                .description("List of legs constituting a suggested sequence of rides and links for a specific trip.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("startTime")
                        .description("Time that the trip departs.")
                        .type(dateTimeScalar)
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).startTime().getTime().getTime())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("endTime")
                        .description("Time that the trip arrives.")
                        .type(dateTimeScalar)
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).endTime().getTime().getTime())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("duration")
                        .description("Duration of the trip, in seconds.")
                        .type(Scalars.GraphQLLong)
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).durationSeconds)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("directDuration")
                        .description("NOT IMPLEMENTED.")
                        .type(Scalars.GraphQLLong)
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).durationSeconds)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("waitingTime")
                        .description("How much time is spent waiting for transit to arrive, in seconds.")
                        .type(Scalars.GraphQLLong)
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).waitingTimeSeconds)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("distance")
                        .description("Total distance for the trip, in meters. NOT IMPLEMENTED")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> 0)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("walkTime")
                        .description("How much time is spent walking, in seconds.")
                        .type(Scalars.GraphQLLong)
                        // TODO This unfortunately include BIKE and CAR
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).nonTransitTimeSeconds)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("walkDistance")
                        // TODO This unfortunately include BIKE and CAR
                        .description("How far the user has to walk, in meters.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).nonTransitDistanceMeters)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("debugMarkedAsDeleted")
                        .description("This itinerary is marked as deleted by at least one itinerary filter.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).debugMarkedAsDeleted)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("legs")
                        .description("A list of legs. Each leg is either a walking (cycling, car) portion of the trip, or a ride leg on a particular vehicle. So a trip where the use walks to the Q train, transfers to the 6, then walks to their destination, has four legs.")
                        .type(new GraphQLNonNull(new GraphQLList(legType)))
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).legs)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("weight")
                        .description("Weight of the itinerary. Used for debugging. NOT IMPLEMENTED")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> 0.0)
                        .build())

                .build();

        routingParametersType = GraphQLObjectType.newObject()
                .name("RoutingParameters")
                .description("The default parameters used in travel searches.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("walkSpeed")
                        .description("Max walk speed along streets, in meters per second")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).walkSpeed)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeSpeed")
                        .description("Max bike speed along streets, in meters per second")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).bikeSpeed)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("carSpeed")
                        .description("Max car speed along streets, in meters per second")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).carSpeed)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("maxWalkDistance")
                        .description("The maximum distance (in meters) the user is willing to walk for access/egress legs.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).maxWalkDistance)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("maxTransferWalkDistance")
                        .description("The maximum distance (in meters) the user is willing to walk for transfer legs.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).maxTransferWalkDistance)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("maxPreTransitTime")
                        .description("The maximum time (in seconds) of pre-transit travel when using drive-to-transit (park and ride or kiss and ride).")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).maxPreTransitTime)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("wheelChairAccessible")
                        .description("Whether the trip must be wheelchair accessible.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).wheelchairAccessible)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("numItineraries")
                        .description("The maximum number of itineraries to return.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).numItineraries)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("maxSlope")
                        .description("The maximum slope of streets for wheelchair trips.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).maxWheelchairSlope)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("showIntermediateStops")
                        .description("Whether the planner should return intermediate stops lists for transit legs.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).showIntermediateStops)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transferPenalty")
                        .description("An extra penalty added on transfers (i.e. all boardings except the first one).")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).transferPenalty)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("walkReluctance")
                        .description("A multiplier for how bad walking is, compared to being in transit for equal lengths of time.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).walkReluctance)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("stairsReluctance")
                        .description("Used instead of walkReluctance for stairs.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).stairsReluctance)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("turnReluctance")
                        .description("Multiplicative factor on expected turning time.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).turnReluctance)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("walkOnStreetReluctance")
                        .description("How much more reluctant is the user to walk on streets with car traffic allowed.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).walkOnStreetReluctance)
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("elevatorBoardTime")
                        .description("How long does it take to get on an elevator, on average.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).elevatorBoardTime)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("elevatorBoardCost")
                        .description("What is the cost of boarding a elevator?")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).elevatorBoardCost)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("elevatorHopTime")
                        .description("How long does it take to advance one floor on an elevator?")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).elevatorHopTime)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("elevatorHopCost")
                        .description("What is the cost of travelling one floor on an elevator?")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).elevatorHopCost)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalPickupTime")
                        .description("Time to rent a bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).bikeRentalPickupTime)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalPickupCost")
                        .description("Cost to rent a bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).bikeRentalPickupCost)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalDropOffTime")
                        .description("Time to drop-off a rented bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).bikeRentalDropoffTime)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalDropOffCost")
                        .description("Cost to drop-off a rented bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).bikeRentalDropoffCost)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeParkTime")
                        .description("Time to park a bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).bikeParkTime)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeParkCost")
                        .description("Cost to park a bike.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).bikeParkCost)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("carDropOffTime")
                        .description("Time to park a car in a park and ride, w/o taking into account driving and walking cost.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).carDropoffTime)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("waitReluctance")
                        .description("How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).waitReluctance)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("waitAtBeginningFactor")
                        .description("How much less bad is waiting at the beginning of the trip (replaces waitReluctance on the first boarding).")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).waitAtBeginningFactor)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("walkBoardCost")
                        .description("This prevents unnecessary transfers by adding a cost for boarding a vehicle.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).walkBoardCost)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeBoardCost")
                        .description("Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).bikeBoardCost)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("otherThanPreferredRoutesPenalty")
                        .description("Penalty added for using every route that is not preferred if user set any route as preferred. We return number of seconds that we are willing to wait for preferred route.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).otherThanPreferredRoutesPenalty)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transferSlack")
                        .description("A global minimum transfer time (in seconds) that specifies the minimum amount of time that must pass between exiting one transit vehicle and boarding another.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).transferSlack)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("boardSlack")
                        .description("Invariant: boardSlack + alightSlack <= transferSlack.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).boardSlack)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("alightSlack")
                        .description("Invariant: boardSlack + alightSlack <= transferSlack.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).alightSlack)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("maxTransfers")
                        .description("Maximum number of transfers returned in a trip plan.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).maxTransfers)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("reverseOptimizeOnTheFly")
                        .description("DEPRECATED - NOT IN USE IN OTP2.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(e -> false)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("compactLegsByReversedSearch")
                        .description("DEPRECATED - NOT IN USE IN OTP2.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(e -> false)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("carDecelerationSpeed")
                        .description("The deceleration speed of an automobile, in meters per second per second.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).carDecelerationSpeed)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("carAccelerationSpeed")
                        .description("The acceleration speed of an automobile, in meters per second per second.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).carAccelerationSpeed)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("ignoreRealTimeUpdates")
                        .description("When true, realtime updates are ignored during this search.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).ignoreRealtimeUpdates)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("includedPlannedCancellations")
                        .description("When true, service journeys cancelled in scheduled route data will be included during this search.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).includePlannedCancellations)
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("disableRemainingWeightHeuristic")
                        .description("If true, the remaining weight heuristic is disabled.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).disableRemainingWeightHeuristic)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("allowBikeRental")
                        .description("")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).allowBikeRental)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeParkAndRide")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).bikeParkAndRide)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("parkAndRide")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).parkAndRide)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("kissAndRide")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).kissAndRide)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("debugItineraryFilter")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).debugItineraryFilter)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("rideAndKiss")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).rideAndKiss)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("useTraffic")
                        .description("Should traffic congestion be considered when driving?")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).useTraffic)
                        .build())
                 */
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("onlyTransitTrips")
                        .description("Accept only paths that use transit (no street-only paths).")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).onlyTransitTrips)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("disableAlertFiltering")
                        .description("Option to disable the default filtering of GTFS-RT alerts by time.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).disableAlertFiltering)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("geoIdElevation")
                        .description("Whether to apply the ellipsoid->geoid offset to all elevations in the response.")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).geoidElevation)
                        .build())
                /*
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("preferredInterchangePenalty")
                        .description("Whether to apply the ellipsoid->geoid offset to all elevations in the response.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).preferredInterchangePenalty)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("recommendedInterchangePenalty")
                        .description("Whether to apply the ellipsoid->geoid offset to all elevations in the response.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).recommendedInterchangePenalty)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("interchangeAllowedPenalty")
                        .description("Whether to apply the ellipsoid->geoid offset to all elevations in the response.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).interchangeAllowedPenalty)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("noInterchangePenalty")
                        .description("Whether to apply the ellipsoid->geoid offset to all elevations in the response.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).noInterchangePenalty)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("preTransitReluctance")
                        .description("How much worse driving before and after transit is than riding on transit. Applies to ride and kiss, kiss and ride and park and ride.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((RoutingRequest) environment.getSource()).preTransitReluctance)
                        .build())
                 */
                .build();

        tripMetadataType = GraphQLObjectType.newObject()
                .name("TripSearchData")
                .description("Trips search metadata.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("searchWindowUsed")
                        .description("The search-window used in the current trip request. Unit: seconds.")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(e -> ((TripSearchMetadata) e.getSource()).searchWindowUsed)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("nextDateTime")
                        .description(
                                "This is the suggested search time for the \"next page\" or time "
                                + "window. Insert it together with the 'searchWindowUsed' in the "
                                + "request to get a new set of trips following in the time-window "
                                + "AFTER the current search. No duplicate trips should be "
                                + "returned, unless a trip is delayed and new realtime-data is "
                                + "available."
                        )
                        .type(dateTimeScalar)
                        .dataFetcher(e -> ((TripSearchMetadata) e.getSource()).nextDateTime.toEpochMilli())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("prevDateTime")
                        .description(
                                "This is the suggested search time for the \"previous page\" or "
                                + "time-window. Insert it together with the 'searchWindowUsed' in "
                                + "the request to get a new set of trips preceding in the "
                                + "time-window BEFORE the current search. No duplicate trips "
                                + "should be returned, unless a trip is delayed and new "
                                + "realtime-data is available."
                        )
                        .type(dateTimeScalar)
                        .dataFetcher(e -> ((TripSearchMetadata) e.getSource()).prevDateTime.toEpochMilli())
                        .build())
                .build();

        tripType = GraphQLObjectType.newObject()
                .name("Trip")
                .description("Description of a travel between two places.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("dateTime")
                        .description("The time and date of travel")
                        .type(dateTimeScalar)
                        .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.date.getTime())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("metadata")
                        .description("The trip request metadata.")
                        .type(tripMetadataType)
                        .dataFetcher(env -> ((PlanResponse) env.getSource()).metadata)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("fromPlace")
                        .description("The origin")
                        .type(new GraphQLNonNull(placeType))
                        .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.from)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("toPlace")
                        .description("The destination")
                        .type(new GraphQLNonNull(placeType))
                        .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.to)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("tripPatterns")
                        .description("A list of possible trip patterns")
                        .type(new GraphQLNonNull(new GraphQLList(tripPatternType)))
                        .dataFetcher(env -> ((PlanResponse) env.getSource()).plan.itineraries)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("messageEnums")
                        .description("A list of possible error messages as enum")
                        .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
                        .dataFetcher(env -> ((PlanResponse) env.getSource()).messages
                                .stream().map(Enum::name).collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("messageStrings")
                        .description("A list of possible error messages in cleartext")
                        .type(new GraphQLNonNull(new GraphQLList(Scalars.GraphQLString)))
                        .dataFetcher(env -> ((PlanResponse) env.getSource()).messages
                                .stream()
                                .map(message -> message.get(ResourceBundleSingleton.INSTANCE.getLocale(
                                        env.getArgument("locale"))))
                                .collect(Collectors.toList())
                        )
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("debugOutput")
                        .description("Information about the timings for the trip generation")
                        .type(new GraphQLNonNull(GraphQLObjectType.newObject()
                                .name("debugOutput")
                                .field(GraphQLFieldDefinition.newFieldDefinition()
                                        .name("totalTime")
                                        .type(Scalars.GraphQLLong)
                                        .build())
                                .build()))
                        .dataFetcher(env -> ((PlanResponse) env.getSource()).debugOutput)
                        .build())
                .build();
    }

    private Stream<TripTimeShort> getTripTimesForStop(
            Stop stop,
            Long startTimeSeconds,
            int timeRage,
            boolean omitNonBoarding,
            int numberOfDepartures,
            Integer departuresPerLineAndDestinationDisplay,
            Set<String> authorityIdsWhiteListed,
            Set<FeedScopedId> lineIdsWhiteListed
    ) {

        boolean limitOnDestinationDisplay = departuresPerLineAndDestinationDisplay != null &&
                departuresPerLineAndDestinationDisplay > 0 &&
                departuresPerLineAndDestinationDisplay < numberOfDepartures;

        int departuresPerTripPattern = limitOnDestinationDisplay ? departuresPerLineAndDestinationDisplay : numberOfDepartures;

        List<StopTimesInPattern> stopTimesInPatterns = index.stopTimesForStop(
                stop, startTimeSeconds, timeRage, departuresPerTripPattern, omitNonBoarding
        );

        Stream<TripTimeShort> tripTimesStream = stopTimesInPatterns.stream().flatMap(p -> p.times.stream());

        tripTimesStream = whiteListAuthoritiesAndOrLines(tripTimesStream,  authorityIdsWhiteListed, lineIdsWhiteListed);

        if(!limitOnDestinationDisplay) {
            return tripTimesStream;
        }
        // Group by line and destination display, limit departures per group and merge
        return tripTimesStream
                .collect(Collectors.groupingBy(this::destinationDisplayPerLine))
                .values()
                .stream()
                .flatMap(tripTimes ->
                        tripTimes.stream()
                                .sorted(TripTimeShort.compareByDeparture())
                                .distinct()
                                .limit(departuresPerLineAndDestinationDisplay)
                );
    }

    private String destinationDisplayPerLine(TripTimeShort t) {
        Trip trip = index.tripForId.get(t.tripId);
        return trip == null ?  t.headsign :  trip.getRoute().getId() + "|" + t.headsign;
    }

    private <T> List<T> wrapInListUnlessNull(T element) {
        if (element == null) {
            return emptyList();
        }
        return Arrays.asList(element);
    }

    private int directIdStringToInt(String directionId) {
        try {
            return Integer.parseInt(directionId);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    private <T extends Object> List<String> reverseMapEnumVals(GraphQLEnumType enumType, Collection<T> otpVals) {
        return enumType.getValues().stream().filter(e -> otpVals.contains(e.getValue())).map(e -> e.getName()).collect(Collectors.toList());
    }

    private String reverseMapEnumVal(GraphQLEnumType enumType, Object otpVal) {
        return enumType.getValues().stream().filter(e -> e.getValue().equals(otpVal)).findFirst().get().getName();
    }


    private Stream<TripTimeShort> whiteListAuthoritiesAndOrLines(Stream<TripTimeShort> stream, Set<String> authorityIds, Set<FeedScopedId> lineIds) {
        if (CollectionUtils.isEmpty(authorityIds) && CollectionUtils.isEmpty(lineIds)) {
            return stream;
        }
        return stream.filter(it -> isTripTimeShortAcceptable(it, authorityIds, lineIds));
    }


    private boolean isTripTimeShortAcceptable(TripTimeShort tts, Set<String> authorityIds, Set<FeedScopedId> lineIds) {
        Trip trip = index.tripForId.get(tts.tripId);

        if (trip == null || trip.getRoute() == null) {
            return true;
        }

        Route route = trip.getRoute();
        boolean okForAuthority = authorityIds.contains(route.getAgency().getId());
        boolean okForLine = lineIds.contains(route.getId());

        return okForAuthority || okForLine;
    }

    private boolean isStopPlaceInUse(StopCollection station) {
        for (Stop quay: station.getChildStops()) {
            if (!index.getPatternsForStop(quay,true).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
