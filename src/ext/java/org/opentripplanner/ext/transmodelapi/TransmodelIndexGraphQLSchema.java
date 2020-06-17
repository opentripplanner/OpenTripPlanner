package org.opentripplanner.ext.transmodelapi;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.relay.SimpleListConnection;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.ext.transmodelapi.mapping.TransmodelMappingUtil;
import org.opentripplanner.ext.transmodelapi.model.DefaultRoutingRequest;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.model.MonoOrMultiModalStation;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.model.TransportModeSlack;
import org.opentripplanner.ext.transmodelapi.model.TripTimeShortHelper;
import org.opentripplanner.ext.transmodelapi.model.scalars.DateScalarFactory;
import org.opentripplanner.ext.transmodelapi.model.scalars.DateTimeScalarFactory;
import org.opentripplanner.ext.transmodelapi.model.scalars.GeoJSONCoordinatesScalar;
import org.opentripplanner.ext.transmodelapi.model.scalars.LocalTimeScalarFactory;
import org.opentripplanner.ext.transmodelapi.model.scalars.TimeScalarFactory;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopCollection;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.alertpatch.AlertUrl;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.TripSearchMetadata;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.StopAtDistance;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.util.PolylineEncoder;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.STREET_MODE;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_MODE;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_SUBMODE;
import static org.opentripplanner.model.StopPattern.PICKDROP_COORDINATE_WITH_DRIVER;
import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

/**
 * Schema definition for the Transmodel GraphQL API.
 * <p>
 * Currently a simplified version of the IndexGraphQLSchema, with gtfs terminology replaced with corresponding terms from Transmodel.
 */
public class TransmodelIndexGraphQLSchema {
    private static final Logger LOG = LoggerFactory.getLogger(TransmodelIndexGraphQLSchema.class);

    private final DefaultRoutingRequest routing;

    private GraphQLOutputType noticeType = new GraphQLTypeReference("Notice");

    private GraphQLOutputType authorityType = new GraphQLTypeReference("Authority");

    private GraphQLOutputType operatorType = new GraphQLTypeReference("Operator");

    private GraphQLNamedOutputType ptSituationElementType = new GraphQLTypeReference("PtSituationElement");

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

    private GraphQLNamedOutputType quayAtDistance = new GraphQLTypeReference("QuayAtDistance");

    private GraphQLOutputType multilingualStringType = new GraphQLTypeReference("TranslatedString");

    private GraphQLNamedOutputType placeAtDistanceType = new GraphQLTypeReference("PlaceAtDistance");

    private GraphQLOutputType bookingArrangementType = new GraphQLTypeReference("BookingArrangement");

    private GraphQLOutputType contactType = new GraphQLTypeReference("Contact");

    private GraphQLOutputType systemNoticeType = new GraphQLTypeReference("SystemNotice");

  private GraphQLOutputType serverInfoType = new GraphQLTypeReference("OtpVersion");

  private GraphQLInputObjectType locationType;

    private GraphQLInputObjectType modesInputType;

    private GraphQLObjectType keyValueType;

    //private GraphQLObjectType brandingType;

    private GraphQLObjectType linkGeometryType;

    private GraphQLObjectType queryType;

    private GraphQLOutputType tripType = new GraphQLTypeReference("Trip");

    private GraphQLOutputType tripMetadataType = new GraphQLTypeReference("TripMetadata");

    private GraphQLOutputType interchangeType = new GraphQLTypeReference("interchange");

    private GraphQLInputObjectType allowedModesType;

    private TransmodelMappingUtil mappingUtil;

    private TripTimeShortHelper tripTimeShortHelper;
    private String fixedAgencyId;

    private GraphQLScalarType dateTimeScalar;
    private GraphQLObjectType timeType;
    private GraphQLScalarType dateScalar;
    private GraphQLObjectType destinationDisplayType;
    private GraphQLScalarType localTimeScalar;

    public GraphQLSchema indexSchema;

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

    private Agency getAgency(FeedScopedId agencyId, RoutingService routingService) {
        return routingService.getAgencyForId(agencyId);
    }

    @SuppressWarnings("unchecked")
    public TransmodelIndexGraphQLSchema(Graph graph, RoutingRequest defaultRequest, TransmodelMappingUtil mappingUtil) {
        this.mappingUtil = mappingUtil;
        this.routing = new DefaultRoutingRequest(defaultRequest);

        tripTimeShortHelper = new TripTimeShortHelper();
        dateTimeScalar = DateTimeScalarFactory.createMillisecondsSinceEpochAsDateTimeStringScalar(graph.getTimeZone());
        timeType = TimeScalarFactory.createSecondsSinceMidnightAsTimeObject();
        dateScalar = DateScalarFactory.createSecondsSinceEpochAsDateStringScalar(graph.getTimeZone());
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

        serverInfoType = GraphQLObjectType.newObject()
            .name("ServerInfo")
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("version")
                .description("Maven version")
                .type(Scalars.GraphQLString)
                .dataFetcher(e -> MavenVersion.VERSION.version)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("buildTime")
                .description("OTP Build timestamp")
                .type(Scalars.GraphQLString)
                .dataFetcher(e -> MavenVersion.VERSION.buildTime)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gitBranch")
                .description("")
                .type(Scalars.GraphQLString)
                .dataFetcher(e -> MavenVersion.VERSION.branch)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gitCommit")
                .description("")
                .type(Scalars.GraphQLString)
                .dataFetcher(e -> MavenVersion.VERSION.commit)
                .build())
            .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("gitCommitTime")
                .description("")
                .type(Scalars.GraphQLString)
                .dataFetcher(e -> MavenVersion.VERSION.commitTime)
                .build())
            .build();

        modesInputType = GraphQLInputObjectType.newInputObject()
            .name("Modes")
            .description("Input format for specifying which modes will be allowed for this search. "
                + "If this element is not present, it will default to accessMode/egressMode/directMode "
                + "of foot and all transport modes will be allowed.")
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("accessMode")
                .description("The mode used to get from the origin to the access stops in the transit "
                    + "network the transit network (first-mile). If the element is not present or null,"
                    + "only transit that can be immediately boarded from the origin will be used.")
                .type(STREET_MODE)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("egressMode")
                .description("The mode used to get from the egress stops in the transit network to"
                    + "the destination (last-mile). If the element is not present or null,"
                    + "only transit that can immediately arrive at the origin will be used.")
                .type(STREET_MODE)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("directMode")
                .description("The mode used to get from the origin to the destination directly, "
                    + "without using the transit network. If the element is not present or null,"
                    + "direct travel without using transit will be disallowed.")
                .type(STREET_MODE)
                .build())
            .field(GraphQLInputObjectField.newInputObjectField()
                .name("transportMode")
                .description("The allowed modes for the transit part of the trip. Use an empty list "
                    + "to disallow transit for this search. If the element is not present or null, "
                    + "it will default to all transport modes.")
                .type(new GraphQLList(TRANSPORT_MODE))
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

        systemNoticeType = GraphQLObjectType.newObject()
                .name("SystemNotice")
                .description("A system notice is used to tag elements with system information for "
                        + "debugging or other system related purpose. One use-case is to run a "
                        + "routing search with 'debugItineraryFilter: true'. This will then tag "
                        + "itineraries instead of removing them from the result. This make it "
                        + "possible to inspect the itinerary-filter-chain. A SystemNotice only "
                        + "have english text, because the primary user are technical staff, like "
                        + "testers and developers.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("tag")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(env -> ((SystemNotice) env.getSource()).tag)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("text")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(env -> ((SystemNotice) env.getSource()).text)
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
                        .name("otherThanPreferredLinesPenalty")
                        .description("Penalty added for using a line that is not preferred if user has set any line as preferred. In number of seconds that user is willing to wait for preferred line.")
                        .type(Scalars.GraphQLInt)
                        .defaultValue(routing.request.otherThanPreferredRoutesPenalty)
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
                .build();

        GraphQLInputObjectType bannedInputType = GraphQLInputObjectType.newInputObject()
                .name("InputBanned")
                .description("Filter trips by disallowing lines involving certain "
                    + "elements. If both lines and authorities are specified, only one must be valid "
                    + "for each line to be banned. If a line is both banned and whitelisted, "
                    + "it will be counted as banned.")
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
                .description("Filter trips by only allowing lines involving certain "
                    + "elements. If both lines and authorities are specified, only one must be valid "
                    + "for each line to be used. If a line is both banned and whitelisted, it will "
                    + "be counted as banned.")
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
                .build();

        GraphQLInputObjectType transportSubmodeFilterInputType = GraphQLInputObjectType.newInputObject()
                .name("TransportSubmodeFilter")
                .description("Filter trips by allowing only certain transport submodes per mode.")
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("transportMode")
                        .description("Set of ids for lines that should be used")
                        .type(new GraphQLNonNull(TRANSPORT_MODE))
                        .build())
                .field(GraphQLInputObjectField.newInputObjectField()
                        .name("transportSubmodes")
                        .description("Set of transport submodes allowed for transport mode.")
                        .type(new GraphQLNonNull(new GraphQLList(TRANSPORT_SUBMODE)))
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
                        .description("The length of the search-window in minutes. This is normally dynamically calculated by the server, but you may override this by setting it. The search-window used in a request is returned in the response metadata. To get the \"next page\" of trips use the metadata(searchWindowUsed and nextWindowDateTime) to create a new request. If not provided the value is resolved depending on the other input parameters, available transit options and realtime changes.")
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
                        .defaultValue(routing.request.wheelchairAccessible)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("numTripPatterns")
                        .description("The maximum number of trip patterns to return.")
                        .defaultValue(routing.request.numItineraries)
                        .type(Scalars.GraphQLInt)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("maximumWalkDistance")
                        .description("DEPRECATED - Use maxPreTransitWalkDistance/maxTransferWalkDistance instead. " +
                                "The maximum distance (in meters) the user is willing to walk. Note that trip patterns with " +
                                "longer walking distances will be penalized, but not altogether disallowed. Maximum allowed value is 15000 m")
                        .defaultValue(routing.request.maxWalkDistance)
                        .type(Scalars.GraphQLFloat)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("maxTransferWalkDistance")
                        .description("The maximum walk distance allowed for transfers.")
                        .defaultValue(routing.request.maxTransferWalkDistance)
                        .type(Scalars.GraphQLFloat)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("walkSpeed")
                        .description("The maximum walk speed along streets, in meters per second")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(routing.request.walkSpeed)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("bikeSpeed")
                        .description("The maximum bike speed along streets, in meters per second")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(routing.request.bikeSpeed)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("optimisationMethod")
                        .description("The set of characteristics that the user wants to optimise for -- defaults to " + reverseMapEnumVal(EnumTypes.OPTIMISATION_METHOD, routing.request.optimize))
                        .type(EnumTypes.OPTIMISATION_METHOD)
                        .defaultValue(routing.request.optimize)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("arriveBy")
                        .description("Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(routing.request.arriveBy)
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
                        .defaultValue(routing.request.transferCost)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("modes")
                        .description("The set of access/egress/direct/transit modes to be used for "
                            + "this search.")
                        .type(modesInputType)
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
                        .defaultValue(routing.request.bikeRental)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("minimumTransferTime")
                        .description("DEPRECATED - Use 'transferSlack/boardSlack/alightSlack' instead.  ")
                        .type(Scalars.GraphQLInt)
                        .defaultValue(routing.request.transferSlack)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("transferSlack")
                        .description("An expected transfer time (in seconds) that specifies the amount of time that must pass between exiting one public transport vehicle and boarding another. This time is in addition to time it might take to walk between stops.")
                        .type(Scalars.GraphQLInt)
                        .defaultValue(routing.request.transferSlack)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("boardSlackDefault")
                        .description(TransportModeSlack.boardSlackDescription("boardSlackList"))
                        .type(Scalars.GraphQLInt)
                        .defaultValue(routing.request.boardSlack)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("boardSlackList")
                        .description(TransportModeSlack.slackByGroupDescription(
                                "boardSlack", routing.request.boardSlackForMode
                        ))
                        .type(TransportModeSlack.SLACK_LIST_INPUT_TYPE)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("alightSlackDefault")
                        .description(TransportModeSlack.alightSlackDescription("alightSlackList"))
                        .type(Scalars.GraphQLInt)
                        .defaultValue(routing.request.alightSlack)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("alightSlackList")
                        .description(TransportModeSlack.slackByGroupDescription(
                                "alightSlack", routing.request.alightSlackForMode
                        ))
                        .type(TransportModeSlack.SLACK_LIST_INPUT_TYPE)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("maximumTransfers")
                        .description("Maximum number of transfers")
                        .type(Scalars.GraphQLInt)
                        .defaultValue(routing.request.maxTransfers)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("ignoreRealtimeUpdates")
                        .description("When true, realtime updates are ignored during this search.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(routing.request.ignoreRealtimeUpdates)
                        .build())
                /*
                .argument(GraphQLArgument.newArgument()
                        .name("includePlannedCancellations")
                        .description("When true, service journeys cancelled in scheduled route data will be included during this search.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.defaults.includePlannedCancellations)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("ignoreInterchanges")
                        .description("DEPRECATED - For debugging only. Ignores interchanges defined in timetable data.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.defaults.ignoreInterchanges)
                        .build())
                 */
                .argument(GraphQLArgument.newArgument()
                        .name("locale")
                        .type(EnumTypes.LOCALE)
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
                        .defaultValue(routing.request.maxPreTransitTime)
                        .build())
                /*
                .argument(GraphQLArgument.newArgument()
                        .name("preTransitReluctance")
                        .description("How much worse driving before and after transit is than riding on transit. Applies to ride and kiss, kiss and ride and park and ride.")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(defaultRoutingRequest.defaults.preTransitReluctance)
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
                        .defaultValue(defaultRoutingRequest.defaults.banFirstTripsFromReuseNo)
                        .build())
                 */
                .argument(GraphQLArgument.newArgument()
                        .name("walkReluctance")
                        .description("Walk cost is multiplied by this value. This is the main parameter to use for limiting walking.")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(routing.request.walkReluctance)
                        .build())
                /*
                .argument(GraphQLArgument.newArgument()
                        .name("ignoreMinimumBookingPeriod")
                        .description("Ignore the MinimumBookingPeriod defined on the ServiceJourney and allow itineraries to start immediately after the current time.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(defaultRoutingRequest.defaults.ignoreDrtAdvanceBookMin)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("transitDistanceReluctance")
                        .description("The extra cost per meter that is travelled by transit. This is a cost point peter meter, so it should in most\n" +
                                "cases be a very small fraction. The purpose of assigning a cost to distance is often because it correlates with\n" +
                                "fare prices and you want to avoid situations where you take detours or travel back again even if it is\n" +
                                "technically faster. Setting this value to 0 turns off the feature altogether.")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(defaultRoutingRequest.defaults.transitDistanceReluctance)
                        .build())
                */
                .argument(GraphQLArgument.newArgument()
                        .name("debugItineraryFilter")
                        .description("Debug the itinerary-filter-chain. The filters will mark itineraries as deleted, but NOT delete them when this is enabled.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(routing.request.debugItineraryFilter)
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
                        .dataFetcher(environment -> getAgency(((AlertPatch) environment.getSource()).getAgency(), getRoutingService(environment)))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lines")
                        .type(new GraphQLNonNull(new GraphQLList(lineType)))
                        .dataFetcher(environment -> wrapInListUnlessNull(getRoutingService(environment)
                            .getRouteForId(((AlertPatch) environment.getSource()).getRoute())))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourneys")
                        .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
                        .dataFetcher(environment -> wrapInListUnlessNull(getRoutingService(environment)
                            .getTripForId().get(((AlertPatch) environment.getSource()).getTrip())))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quays")
                        .type(new GraphQLNonNull(new GraphQLList(quayType)))
                        .dataFetcher(environment ->
                                wrapInListUnlessNull(getRoutingService(environment).getStopForId(((AlertPatch) environment.getSource()).getStop()))
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
                    .type(EnumTypes.REPORT_TYPE)
                    .description("ReportType of this situation")
                    .dataFetcher(environment -> ((AlertPatch) environment.getSource()).getAlert().alertType)
                    .build())
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
                        .dataFetcher(environment -> getAgency(((AlertPatch) environment.getSource()).getAgency(), getRoutingService(environment)))
                        .build())
                .build();


        quayAtDistance = GraphQLObjectType.newObject()
                .name("QuayAtDistance")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment -> relay.toGlobalId(quayAtDistance.getName(),
                                Integer.toString((int) ((StopAtDistance) environment.getSource()).distance) + ";" +
                                        mappingUtil.toIdString(((StopAtDistance) environment.getSource()).stop.getId())))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quay")
                        .type(quayType)
                        .dataFetcher(environment -> ((StopAtDistance) environment.getSource()).stop)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("distance")
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(environment -> ((StopAtDistance) environment.getSource()).distance)
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
                        .type(EnumTypes.WHEELCHAIR_BOARDING)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("weighting")
                        .description("Relative weighting of this stop with regards to interchanges. NOT IMPLEMENTED")
                        .type(EnumTypes.INTERCHANGE_WEIGHTING)
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
                        .type(TRANSPORT_MODE)
                        .dataFetcher(environment -> "unknown")
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transportSubmode")
                        .description("The transport submode serviced by this stop place. NOT IMPLEMENTED")
                        .type(TRANSPORT_SUBMODE)
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
                                quays=quays.stream().filter(stop ->  !getRoutingService(environment)
                                    .getPatternsForStop(stop,true).isEmpty()).collect(Collectors.toList());
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
                                                    lineIds,
                                                    getRoutingService(environment)
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
                                    getRoutingService(environment).getMultiModalStationForStations().get(station));
                            } else {
                                return null;
                            }
                        }
                    )
                    .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("wheelchairAccessible")
                        .type(EnumTypes.WHEELCHAIR_BOARDING)
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
                        .dataFetcher(environment -> getRoutingService(environment)
                            .getPatternsForStop(environment.getSource(),true)
                                .stream()
                                .map(pattern -> pattern.route)
                                .distinct()
                                .collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("journeyPatterns")
                        .description("List of journey patterns servicing this quay")
                        .type(new GraphQLNonNull(new GraphQLList(journeyPatternType)))
                        .dataFetcher(environment -> getRoutingService(environment).getPatternsForStop(environment.getSource(), true))
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
                                    lineIds,
                                    getRoutingService(environment)
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
                    .dataFetcher(env -> getRoutingService(env).getSiriAlertPatchService()
                        .getStopPatches(((Stop)env.getSource()).getId()))
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
                        .dataFetcher(environment -> getRoutingService(environment).getStopForId(
                                ((TripTimeShort) environment.getSource()).stopId
                        )).build())
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
                        .dataFetcher(environment -> getRoutingService(environment).getPatternForTrip()
                                .get(getRoutingService(environment).getTripForId().get(((TripTimeShort) environment.getSource()).tripId))
                                .getBoardType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("forAlighting")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether vehicle may be alighted at quay.")
                        .dataFetcher(environment -> getRoutingService(environment).getPatternForTrip()
                                .get(getRoutingService(environment).getTripForId().get(((TripTimeShort) environment.getSource()).tripId))
                                .getAlightType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("requestStop")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether vehicle will only stop on request.")
                        .dataFetcher(environment -> getRoutingService(environment).getPatternForTrip()
                                .get(getRoutingService(environment).getTripForId().get(((TripTimeShort) environment.getSource()).tripId))
                                .getAlightType(((TripTimeShort) environment.getSource()).stopIndex) == PICKDROP_COORDINATE_WITH_DRIVER)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourney")
                        .type(serviceJourneyType)
                        .dataFetcher(environment -> getRoutingService(environment).getTripForId()
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
                        .dataFetcher(environment -> getRoutingService(environment).getStopForId(
                                ((TripTimeShort) environment.getSource()).stopId)
                        ).build())
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
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("predictionInaccurate")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether the updated estimates are expected to be inaccurate. NOT IMPLEMENTED")
                        .dataFetcher(environment -> false)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("realtimeState")
                        .type(EnumTypes.REALTIME_STATE)
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
                        return getRoutingService(environment).getPatternForTrip()
                            .get(getRoutingService(environment).getTripForId().get(((TripTimeShort) environment.getSource()).tripId))
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
                        return getRoutingService(environment).getPatternForTrip()
                            .get(getRoutingService(environment).getTripForId().get(((TripTimeShort) environment.getSource()).tripId))
                            .getAlightType(((TripTimeShort) environment.getSource()).stopIndex) != PICKDROP_NONE;

                    })
                    .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("requestStop")
                        .type(Scalars.GraphQLBoolean)
                        .description("Whether vehicle will only stop on request.")
                        .dataFetcher(environment -> getRoutingService(environment).getPatternForTrip()
                                .get(getRoutingService(environment).getTripForId().get(((TripTimeShort) environment.getSource()).tripId))
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
                        .dataFetcher(environment -> getRoutingService(environment).getTripForId()
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
                        .dataFetcher(environment -> getAllRelevantAlerts(environment.getSource(), getRoutingService(environment)))
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
                        .dataFetcher(environment -> getRoutingService(environment).getCalendarService()
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
                        .type(TRANSPORT_SUBMODE)
                        .description("The transport submode of the journey, if different from lines transport submode. NOT IMPLEMENTED")
                        .dataFetcher(environment -> TransmodelTransportSubmode.UNDEFINED)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("publicCode")
                        .type(Scalars.GraphQLString)
                        .description("Publicly announced code for service journey, differentiating it from other service journeys for the same line.")
                        .dataFetcher(environment -> (((Trip) environment.getSource()).getTripShortName()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("privateCode")
                        .type(Scalars.GraphQLString)
                        .description("For internal use by operators.")
                        .dataFetcher(environment -> (((Trip) environment.getSource()).getInternalPlanningCode()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("operator")
                        .type(operatorType)
                        .dataFetcher(
                                environment -> (((Trip) environment.getSource()).getOperator()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("directionType")
                        .type(EnumTypes.DIRECTION_TYPE)
                        .dataFetcher(environment -> directIdStringToInt(((Trip) environment.getSource()).getDirectionId()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("wheelchairAccessible")
                        .type(EnumTypes.WHEELCHAIR_BOARDING)
                        .description("Whether service journey is accessible with wheelchair.")
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikesAllowed")
                        .type(EnumTypes.BIKES_ALLOWED)
                        .description("Whether bikes are allowed on service journey.")
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("journeyPattern")
                        .type(journeyPatternType)
                        .dataFetcher(
                                environment -> getRoutingService(environment).getPatternForTrip().get(environment.getSource()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quays")
                        .description("Quays visited by service journey")
                        .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(quayType))))
                        .dataFetcher(environment -> getRoutingService(environment).getPatternForTrip()
                                .get(environment.getSource()).getStops())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("passingTimes")
                        .type(new GraphQLNonNull(new GraphQLList(timetabledPassingTimeType)))
                        .description("Returns scheduled passing times only - without realtime-updates, for realtime-data use 'estimatedCalls'")
                        .dataFetcher(environment -> TripTimeShort.fromTripTimes(
                                getRoutingService(environment).getPatternForTrip().get((Trip) environment.getSource()).scheduledTimetable,
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
                            return getRoutingService(environment).getTripTimesShort(trip, serviceDate);
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("pointsOnLink")
                        .type(linkGeometryType)
                        .description("Detailed path travelled by service journey.")
                        .dataFetcher(environment -> {
                                    LineString geometry = getRoutingService(environment).getPatternForTrip()
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
                                return getRoutingService(environment).getNoticesByEntity(trip);
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("Get all situations active for the service journey.")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                        .dataFetcher(environment ->
                            getRoutingService(environment).getSiriAlertPatchService().getTripPatches(
                            environment.getSource()))
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
                        .type(EnumTypes.DIRECTION_TYPE)
                        .dataFetcher(environment -> ((TripPattern) environment.getSource()).directionId)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((TripPattern) environment.getSource()).name)
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

                            BitSet services = getRoutingService(environment).getServicesRunningForDate(mappingUtil.secondsSinceEpochToServiceDate(environment.getArgument("date")));
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
                    .dataFetcher(environment -> getRoutingService(environment).getSiriAlertPatchService().getTripPatternPatches(
                        environment.getSource()))
                    .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("notices")
                        .type(new GraphQLNonNull(new GraphQLList(noticeType)))
                        .dataFetcher(environment -> {
                            TripPattern tripPattern = environment.getSource();
                            return getRoutingService(environment).getNoticesByEntity(tripPattern);
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
                        .name("publicCode")
                        .type(Scalars.GraphQLString)
                        .description("Publicly announced code for line, differentiating it from other lines for the same operator.")
                        .dataFetcher(environment -> (((Route) environment.getSource()).getShortName()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> ((Route) environment.getSource()).getLongName())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transportMode")
                        .type(TRANSPORT_MODE)
                        .dataFetcher(environment -> ((Route)environment.getSource()).getMode())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transportSubmode")
                        .type(EnumTypes.TRANSPORT_SUBMODE)
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
                        .type(EnumTypes.BIKES_ALLOWED)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("journeyPatterns")
                        .type(new GraphQLList(journeyPatternType))
                        .dataFetcher(environment -> getRoutingService(environment).getPatternsForRoute()
                                .get(environment.getSource()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quays")
                        .type(new GraphQLNonNull(new GraphQLList(quayType)))
                        .dataFetcher(environment -> getRoutingService(environment).getPatternsForRoute()
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
                        .dataFetcher(environment -> getRoutingService(environment).getPatternsForRoute()
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
                            return getRoutingService(environment).getNoticesByEntity(route);
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("Get all situations active for the line.")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                    .dataFetcher(environment -> getRoutingService(environment).getSiriAlertPatchService().getRoutePatches(
                        environment.getSource()))
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

        authorityType = GraphQLObjectType.newObject()
                .name("Authority")
                .description("Authority involved in public transportation. An organisation under which the responsibility of organising the transport service in a certain area is placed.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .description("Authority id")
                        .type(new GraphQLNonNull(Scalars.GraphQLID))
                        .dataFetcher(environment -> ((Agency) environment.getSource()).getId().getId())
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
                        .dataFetcher(environment -> getRoutingService(environment).getAllRoutes()
                                .stream()
                                .filter(route -> Objects.equals(route.getAgency(), environment.getSource()))
                                .collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("Get all situations active for the authority.")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                        .dataFetcher(environment -> getRoutingService(environment).getSiriAlertPatchService() .getAgencyPatches(
                            ((Agency)environment.getSource()).getId()))
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
                        .dataFetcher(environment -> getRoutingService(environment).getAllRoutes()
                                .stream()
                                .filter(route -> Objects.equals(route.getOperator(), environment.getSource()))
                                .collect(Collectors.toList()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourney")
                        .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
                        .dataFetcher(environment -> getRoutingService(environment).getTripForId().values()
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
                                getRoutingService(environment)
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
                                            getRoutingService(environment)
                                        ))
                                        .collect(Collectors.toList());
                            }
                            return new ArrayList<>(getRoutingService(environment).getStations());
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
                                .type(EnumTypes.MULTI_MODAL_MODE)
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
                            Stream<Station> stations = getRoutingService(env).getStopsByBoundingBox(
                                    env.getArgument("minimumLatitude"),
                                    env.getArgument("minimumLongitude"),
                                    env.getArgument("maximumLatitude"),
                                    env.getArgument("maximumLongitude")
                            ).stream()
                                .map(Stop::getParentStation)
                                .filter(Objects::nonNull)
                                .distinct()
                                .filter(station -> {
                                    String authority = env.getArgument("authority");
                                    return authority == null || station.getId().getFeedId().equalsIgnoreCase(authority);
                                });

                                if (Boolean.TRUE.equals(env.getArgument("filterByInUse"))){
                                    stations = stations.filter(s -> isStopPlaceInUse(s, getRoutingService(env)));
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
                        .dataFetcher(environment -> getRoutingService(environment).getStopForId(
                                mappingUtil.fromIdString(environment.getArgument("id")))
                        ).build())
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
                                RoutingService routingService = getRoutingService(environment);
                                return ((List<String>) environment.getArgument("ids"))
                                        .stream()
                                        .map(id -> routingService.getStopForId(mappingUtil.fromIdString(id)))
                                        .collect(Collectors.toList());
                            }
                            if (environment.getArgument("name") == null) {
                                return getRoutingService(environment).getAllStops();
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
                        .dataFetcher(environment -> getRoutingService(environment).getStopsByBoundingBox(
                                environment.getArgument("minimumLatitude"),
                                environment.getArgument("minimumLongitude"),
                                environment.getArgument("maximumLatitude"),
                                environment.getArgument("maximumLongitude")
                        ).stream()
                                .filter(stop -> environment.getArgument("authority") == null ||
                                        stop.getId().getFeedId().equalsIgnoreCase(environment.getArgument("authority")))
                                .filter(stop -> !Boolean.TRUE.equals(environment.getArgument("filterByInUse"))
                                                        || !getRoutingService(environment)
                                    .getPatternsForStop(stop,true).isEmpty())
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
                        .arguments(relay.getConnectionFieldArguments())
                        .dataFetcher(environment -> {
                            List<StopAtDistance> stops;
                            try {
                                stops = getRoutingService(environment).findClosestStops(
                                        environment.getArgument("latitude"),
                                        environment.getArgument("longitude"),
                                        environment.getArgument("radius"))
                                        .stream()
                                        .filter(stopAndDistance -> environment.getArgument("authority") == null ||
                                                stopAndDistance.stop.getId().getFeedId()
                                                        .equalsIgnoreCase(environment.getArgument("authority")))
                                        .sorted(Comparator.comparing(s -> s.distance))
                                        .collect(Collectors.toList());
                            } catch (RoutingValidationException e) {
                                LOG.warn(
                                    "findClosestPlacesByWalking failed with exception, returning empty list of places. ",
                                    e);
                                stops = List.of();
                            }
                            return new SimpleListConnection<>(stops).get(environment);
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
                            getRoutingService(environment).getAgencyForId(
                                mappingUtil.fromIdString(environment.getArgument("id"))
                            ))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("authorities")
                        .description("Get all authorities")
                        .type(new GraphQLNonNull(new GraphQLList(authorityType)))
                        .dataFetcher(environment -> new ArrayList<>(getRoutingService(environment).getAgencies()))
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
                                getRoutingService(environment).getOperatorForId().get(
                                        mappingUtil.fromIdString(environment.getArgument("id"))
                                ))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("operators")
                        .description("Get all operators")
                        .type(new GraphQLNonNull(new GraphQLList(operatorType)))
                        .dataFetcher(environment -> new ArrayList<>(getRoutingService(environment).getAllOperators()))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("line")
                        .description("Get a single line based on its id")
                        .type(lineType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> getRoutingService(environment).getRouteForId(
                                mappingUtil.fromIdString(environment.getArgument("id")))
                        ).build())
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
                                .type(new GraphQLList(TRANSPORT_MODE))
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
                                        .map(id -> getRoutingService(environment).getRouteForId(
                                                mappingUtil.fromIdString(id))
                                        ).collect(Collectors.toList());
                            }
                            Stream<Route> stream = getRoutingService(environment).getAllRoutes().stream();
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
                                        .filter(route -> modes.contains(route.getMode()));
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
                        .dataFetcher(environment -> getRoutingService(environment).getTripForId()
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
                                return getRoutingService(environment).getTripForId().values().stream()
                                        .filter(t -> CollectionUtils.isEmpty(lineIds) || lineIds.contains(t.getRoute().getId().getId()))
                                        //.filter(t -> CollectionUtils.isEmpty(privateCodes) || privateCodes.contains(t.getTripPrivateCode()))
                                        .filter(t -> CollectionUtils.isEmpty(authorities) || authorities.contains(t.getRoute().getAgency().getId()))
                                        .filter(t -> CollectionUtils.isEmpty(activeDates) || getRoutingService(environment).getCalendarService().getServiceDatesForServiceId(t.getServiceId()).stream().anyMatch(sd -> activeDates.contains(mappingUtil.serviceDateToSecondsSinceEpoch(sd))))
                                        .collect(Collectors.toList());
                        })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("bikeRentalStations")
                    .description("Get all bike rental stations")
                    .argument(GraphQLArgument.newArgument()
                        .name("ids")
                        .type(new GraphQLList(Scalars.GraphQLString))
                        .build())
                    .type(new GraphQLNonNull(new GraphQLList(bikeRentalStationType)))
                    .dataFetcher(environment -> {
                        Collection<BikeRentalStation> all =
                            new ArrayList<>(getRoutingService(environment)
                                .getBikerentalStationService()
                                .getBikeRentalStations());
                        List<String> filterByIds = environment.getArgument("ids");
                        if (!CollectionUtils.isEmpty(filterByIds)) {
                            return all.stream().filter(station -> filterByIds.contains(station.id)).collect(Collectors.toList());
                        }
                        return all;
                    })
                .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalStation")
                        .description("Get all bike rental stations")
                        .type(bikeRentalStationType)
                        .argument(GraphQLArgument.newArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(environment -> getRoutingService(environment).getBikerentalStationService()
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
                        .dataFetcher(environment -> getRoutingService(environment).getBikerentalStationService()
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
                        .dataFetcher(environment -> new ArrayList<>(getRoutingService(environment).getBikerentalStationService().getBikeParks()))
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
                        .type(this.routing.graphQLType)
                        .dataFetcher(environment -> routing.request)
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
                                .type(new GraphQLList(EnumTypes.SEVERITY))
                                .build())
                        .dataFetcher(environment -> {
                            Collection<AlertPatch> alerts = getRoutingService(environment).getSiriAlertPatchService().getAllAlertPatches();
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
                .field(GraphQLFieldDefinition.newFieldDefinition()
                    .name("serverInfo")
                    .description("Get OTP server information")
                    .type(new GraphQLNonNull(serverInfoType))
                    .dataFetcher(e -> MavenVersion.VERSION)
                    .build())
                .build();

        Set<GraphQLType> dictionary = new HashSet<>();
        dictionary.add(placeInterface);
        dictionary.add(Relay.pageInfoType);

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
     */

    private Collection<AlertPatch> getAllRelevantAlerts(
        TripTimeShort tripTimeShort,
        RoutingService routingService
    ) {
        FeedScopedId tripId = tripTimeShort.tripId;
        Trip trip = routingService.getTripForId().get(tripId);
        FeedScopedId routeId = trip.getRoute().getId();

        FeedScopedId stopId = tripTimeShort.stopId;

        Stop stop = routingService.getStopForId(stopId);
        FeedScopedId parentStopId = stop.getParentStation().getId();

        Collection<AlertPatch> allAlerts = new HashSet<>();

        AlertPatchService alertPatchService = routingService.getSiriAlertPatchService();

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
        allAlerts.addAll(alertPatchService.getTripPatternPatches(routingService.getPatternForTrip().get(trip)));

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
                        .type(EnumTypes.VERTEX_TYPE)
                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("latitude")
                        .description("The latitude of the place.")
                        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                        .dataFetcher(environment -> ((Place) environment.getSource()).coordinate.latitude())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("longitude")
                        .description("The longitude of the place.")
                        .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                        .dataFetcher(environment -> ((Place) environment.getSource()).coordinate.longitude())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("quay")
                        .description("The quay related to the place.")
                        .type(quayType)
                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.TRANSIT) ? getRoutingService(environment)
                            .getStopForId(((Place) environment.getSource()).stopId) : null)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalStation")
                        .type(bikeRentalStationType)
                        .description("The bike rental station related to the place")
                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.BIKESHARE) ?
                                getRoutingService(environment).getBikerentalStationService()
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
                        .type(EnumTypes.RELATIVE_DIRECTION)
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
                        .type(EnumTypes.ABSOLUTE_DIRECTION)
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
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).startLocation.latitude())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("longitude")
                        .description("The longitude of the step.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).startLocation.longitude())
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
                        .type(EnumTypes.MODE)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).mode)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("transportSubmode")
                        .description("The transport sub mode (e.g., localBus or expressBus) used when traversing this leg. Null if leg is not a ride")
                        .type(EnumTypes.TRANSPORT_SUBMODE)
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
                        .dataFetcher(environment -> getAgency(((Leg) environment.getSource()).agencyId, getRoutingService(environment)))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("operator")
                        .description("For ride legs, the operator used for this legs. For non-ride legs, null.")
                        .type(operatorType)
                        .dataFetcher(
                                environment -> {
                                    FeedScopedId tripId = ((Leg) environment.getSource()).tripId;
                                    return tripId == null ? null : getRoutingService(environment)
                                        .getTripForId().get(tripId).getOperator();
                                }
                        )
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
                        .dataFetcher(environment -> getRoutingService(environment).getRouteForId(
                                ((Leg) environment.getSource()).routeId)
                        ).build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourney")
                        .description("For ride legs, the service journey. For non-ride legs, null.")
                        .type(serviceJourneyType)
                        .dataFetcher(environment -> getRoutingService(environment).getTripForId().get(((Leg) environment.getSource()).tripId))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("intermediateQuays")
                        .description("For ride legs, intermediate quays between the Place where the leg originates and the Place where the leg ends. For non-ride legs, empty list.")
                        .type(new GraphQLNonNull(new GraphQLList(quayType)))
                        .dataFetcher(environment -> {
                                List<StopArrival> stops = ((Leg) environment.getSource()).intermediateStops;
                                if (stops == null) {
                                    return new ArrayList<>();
                                }
                                else {
                                    return (stops.stream()
                                            .filter(stop -> stop.place.stopId != null)
                                            .map(s -> getRoutingService(environment).getStopForId(s.place.stopId))
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
                        .dataFetcher(environment -> tripTimeShortHelper.getAllTripTimeShortsForLegsTrip(environment.getSource(), getRoutingService(environment)))
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
                        .name("legs")
                        .description("A list of legs. Each leg is either a walking (cycling, car) portion of the trip, or a ride leg on a particular vehicle. So a trip where the use walks to the Q train, transfers to the 6, then walks to their destination, has four legs.")
                        .type(new GraphQLNonNull(new GraphQLList(legType)))
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).legs)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("systemNotices")
                        .description("Get all system notices.")
                        .type(new GraphQLNonNull(new GraphQLList(systemNoticeType)))
                        .dataFetcher(env -> ((Itinerary) env.getSource()).systemNotices)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("weight")
                        .description("Generalized cost or weight of the itinerary. Used for debugging.")
                        .type(Scalars.GraphQLFloat)
                        .dataFetcher(env -> ((Itinerary) env.getSource()).generalizedCost)
                        .build())
                .build();


        tripMetadataType = GraphQLObjectType.newObject()
                .name("TripSearchData")
                .description("Trips search metadata.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("searchWindowUsed")
                        .description(
                            "The search-window used in the current trip request. Use the value in "
                                + "the next request and set the request 'dateTime' to "
                                + "'nextDateTime' or 'prevDateTime' to get the previous/next "
                                + "\"window\" of results. No duplicate trips should be returned, "
                                + "unless a trip is delayed and new realtime-data is available."
                                + "Unit: minutes."
                        )
                        .type(new GraphQLNonNull(Scalars.GraphQLInt))
                        .dataFetcher(e -> ((TripSearchMetadata) e.getSource()).searchWindowUsed.toMinutes())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("nextDateTime")
                        .description(
                                "This is the suggested search time for the \"next page\" or time "
                                + "window. Insert it together with the 'searchWindowUsed' in the "
                                + "request to get a new set of trips following in the time-window "
                                + "AFTER the current search."
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
                                + "time-window BEFORE the current search."
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
                        .dataFetcher(
                                env -> ((PlanResponse) env.getSource())
                                .listErrorMessages(env.getArgument("locale"))
                        )
                        .build())
                // TODO OTP2 - Next version: Wrap errors, include data like witch parameter
                //           - is causing a problem (like from/to not found).
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
            Set<FeedScopedId> lineIdsWhiteListed,
            RoutingService routingService
    ) {

        boolean limitOnDestinationDisplay = departuresPerLineAndDestinationDisplay != null &&
                departuresPerLineAndDestinationDisplay > 0 &&
                departuresPerLineAndDestinationDisplay < numberOfDepartures;

        int departuresPerTripPattern = limitOnDestinationDisplay ? departuresPerLineAndDestinationDisplay : numberOfDepartures;

        List<StopTimesInPattern> stopTimesInPatterns = routingService.stopTimesForStop(
                stop, startTimeSeconds, timeRage, departuresPerTripPattern, omitNonBoarding
        );

        Stream<TripTimeShort> tripTimesStream = stopTimesInPatterns.stream().flatMap(p -> p.times.stream());

        tripTimesStream = whiteListAuthoritiesAndOrLines(tripTimesStream,  authorityIdsWhiteListed, lineIdsWhiteListed, routingService);

        if(!limitOnDestinationDisplay) {
            return tripTimesStream;
        }
        // Group by line and destination display, limit departures per group and merge
        return tripTimesStream
                .collect(Collectors.groupingBy(t -> destinationDisplayPerLine(((TripTimeShort)t), routingService)))
                .values()
                .stream()
                .flatMap(tripTimes ->
                        tripTimes.stream()
                                .sorted(TripTimeShort.compareByDeparture())
                                .distinct()
                                .limit(departuresPerLineAndDestinationDisplay)
                );
    }

    private String destinationDisplayPerLine(TripTimeShort t, RoutingService routingService) {
        Trip trip = routingService.getTripForId().get(t.tripId);
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


    private Stream<TripTimeShort> whiteListAuthoritiesAndOrLines(
        Stream<TripTimeShort> stream,
        Set<String> authorityIds,
        Set<FeedScopedId> lineIds,
        RoutingService routingService
    ) {
        if (CollectionUtils.isEmpty(authorityIds) && CollectionUtils.isEmpty(lineIds)) {
            return stream;
        }
        return stream.filter(it -> isTripTimeShortAcceptable(
            it,
            authorityIds,
            lineIds,
            routingService
        ));
    }

    private boolean isTripTimeShortAcceptable(
        TripTimeShort tts,
        Set<String> authorityIds,
        Set<FeedScopedId> lineIds,
        RoutingService routingService
    ) {
        Trip trip = routingService.getTripForId().get(tts.tripId);

        if (trip == null || trip.getRoute() == null) {
            return true;
        }

        Route route = trip.getRoute();
        boolean okForAuthority = authorityIds.contains(route.getAgency().getId());
        boolean okForLine = lineIds.contains(route.getId());

        return okForAuthority || okForLine;
    }

    private boolean isStopPlaceInUse(StopCollection station, RoutingService routingService) {
        for (Stop quay: station.getChildStops()) {
            if (!routingService.getPatternsForStop(quay,true).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private RoutingService getRoutingService(DataFetchingEnvironment environment) {
        return ((TransmodelRequestContext) environment.getContext()).getRoutingService();
    }
}
