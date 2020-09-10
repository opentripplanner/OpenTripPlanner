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
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import org.apache.commons.collections.CollectionUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.ext.transmodelapi.mapping.TransmodelMappingUtil;
import org.opentripplanner.ext.transmodelapi.model.DefaultRoutingRequestType;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.ext.transmodelapi.model.PlanResponse;
import org.opentripplanner.ext.transmodelapi.model.TransmodelPlaceType;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.ext.transmodelapi.model.TransportModeSlack;
import org.opentripplanner.ext.transmodelapi.model.TripTimeShortHelper;
import org.opentripplanner.ext.transmodelapi.model.framework.AuthorityType;
import org.opentripplanner.ext.transmodelapi.model.framework.InfoLinkType;
import org.opentripplanner.ext.transmodelapi.model.framework.LocationInputType;
import org.opentripplanner.ext.transmodelapi.model.framework.MultilingualStringType;
import org.opentripplanner.ext.transmodelapi.model.framework.NoticeType;
import org.opentripplanner.ext.transmodelapi.model.framework.OperatorType;
import org.opentripplanner.ext.transmodelapi.model.framework.PointsOnLinkType;
import org.opentripplanner.ext.transmodelapi.model.framework.ServerInfoType;
import org.opentripplanner.ext.transmodelapi.model.framework.SystemNoticeType;
import org.opentripplanner.ext.transmodelapi.model.framework.ValidityPeriodType;
import org.opentripplanner.ext.transmodelapi.model.network.DestinationDisplayType;
import org.opentripplanner.ext.transmodelapi.model.network.JourneyPatternType;
import org.opentripplanner.ext.transmodelapi.model.network.LineType;
import org.opentripplanner.ext.transmodelapi.model.network.PresentationType;
import org.opentripplanner.ext.transmodelapi.model.route.JourneyWhiteListed;
import org.opentripplanner.ext.transmodelapi.model.siri.et.EstimatedCallType;
import org.opentripplanner.ext.transmodelapi.model.siri.sx.PtSituationElementType;
import org.opentripplanner.ext.transmodelapi.model.stop.BikeParkType;
import org.opentripplanner.ext.transmodelapi.model.stop.BikeRentalStationType;
import org.opentripplanner.ext.transmodelapi.model.stop.MonoOrMultiModalStation;
import org.opentripplanner.ext.transmodelapi.model.stop.PlaceAtDistanceType;
import org.opentripplanner.ext.transmodelapi.model.stop.PlaceInterfaceType;
import org.opentripplanner.ext.transmodelapi.model.stop.QuayAtDistanceType;
import org.opentripplanner.ext.transmodelapi.model.stop.QuayType;
import org.opentripplanner.ext.transmodelapi.model.stop.StopPlaceType;
import org.opentripplanner.ext.transmodelapi.model.stop.TariffZoneType;
import org.opentripplanner.ext.transmodelapi.model.timetable.BookingArrangementType;
import org.opentripplanner.ext.transmodelapi.model.timetable.InterchangeType;
import org.opentripplanner.ext.transmodelapi.model.timetable.ServiceJourneyType;
import org.opentripplanner.ext.transmodelapi.model.timetable.TimetabledPassingTimeType;
import org.opentripplanner.ext.transmodelapi.model.timetable.TripMetadataType;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.StopArrival;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper.mapIDsToDomain;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.MODE;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.MULTI_MODAL_MODE;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.STREET_MODE;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.TRANSPORT_MODE;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.filterPlaceTypeEnum;

/**
 * Schema definition for the Transmodel GraphQL API.
 * <p>
 * Currently a simplified version of the IndexGraphQLSchema, with gtfs terminology replaced with corresponding terms from Transmodel.
 */
public class TransmodelGraphQLSchema {
  private static final Logger LOG = LoggerFactory.getLogger(TransmodelGraphQLSchema.class);

  private final DefaultRoutingRequestType routing;

  //private GraphQLObjectType brandingType;


  private final TripTimeShortHelper tripTimeShortHelper;

  private final GqlUtil gqlUtil;

  private final Relay relay = new Relay();


  private TransmodelGraphQLSchema(RoutingRequest defaultRequest, GqlUtil gqlUtil) {
    this.gqlUtil = gqlUtil;
    this.routing = new DefaultRoutingRequestType(defaultRequest);
    this.tripTimeShortHelper = new TripTimeShortHelper();
  }

  public static GraphQLSchema create(RoutingRequest defaultRequest, GqlUtil qglUtil) {
    return new TransmodelGraphQLSchema(defaultRequest, qglUtil).create();
  }


    @SuppressWarnings("unchecked")
  private GraphQLSchema create() {
    /*
    multilingualStringType, validityPeriodType, infoLinkType, bookingArrangementType, systemNoticeType,
    linkGeometryType, serverInfoType, authorityType, operatorType, noticeType



    */

    // Framework
    GraphQLOutputType multilingualStringType = MultilingualStringType.create();
    GraphQLObjectType validityPeriodType = ValidityPeriodType.create(gqlUtil);
    GraphQLObjectType infoLinkType = InfoLinkType.create();
    GraphQLOutputType bookingArrangementType = BookingArrangementType.create(gqlUtil);
    GraphQLOutputType systemNoticeType = SystemNoticeType.create();
    GraphQLOutputType linkGeometryType = PointsOnLinkType.create();
    GraphQLOutputType serverInfoType = ServerInfoType.create();
    GraphQLOutputType authorityType = AuthorityType.create(LineType.REF, PtSituationElementType.REF);
    GraphQLOutputType operatorType = OperatorType.create(LineType.REF, ServiceJourneyType.REF);
    GraphQLOutputType noticeType = NoticeType.create();

    // Stop
    GraphQLOutputType tariffZoneType = TariffZoneType.createTZ();
    GraphQLInterfaceType placeInterface = PlaceInterfaceType.create();
    GraphQLOutputType bikeRentalStationType = BikeRentalStationType.create(placeInterface);
    GraphQLOutputType bikeParkType = BikeParkType.createB(placeInterface);
//  GraphQLOutputType carParkType = new GraphQLTypeReference("CarPark");
    GraphQLOutputType stopPlaceType = StopPlaceType.create(
        placeInterface,
        QuayType.REF,
        tariffZoneType,
        EstimatedCallType.REF,
        gqlUtil
    );
    GraphQLOutputType quayType = QuayType.create(
        placeInterface,
        stopPlaceType,
        LineType.REF,
        JourneyPatternType.REF,
        EstimatedCallType.REF,
        PtSituationElementType.REF,
        tariffZoneType,
        gqlUtil
    );
    GraphQLNamedOutputType quayAtDistance = QuayAtDistanceType.createQD(quayType, relay);
    GraphQLNamedOutputType placeAtDistanceType = PlaceAtDistanceType.create(relay, placeInterface);

    // Network
    GraphQLObjectType presentationType = PresentationType.create();
    GraphQLOutputType destinationDisplayType = DestinationDisplayType.create();
    GraphQLOutputType lineType = LineType.create(
          bookingArrangementType,
          authorityType,
          operatorType,
          noticeType,
          quayType,
          presentationType,
          JourneyPatternType.REF,
          ServiceJourneyType.REF,
          PtSituationElementType.REF
      );
      GraphQLOutputType interchangeType = InterchangeType.create(lineType, ServiceJourneyType.REF);

      // Timetable
      GraphQLNamedOutputType ptSituationElementType = PtSituationElementType.create(
          authorityType, quayType, lineType, ServiceJourneyType.REF, multilingualStringType, validityPeriodType, infoLinkType, relay
      );
      GraphQLOutputType journeyPatternType = JourneyPatternType.create(
          linkGeometryType, noticeType, quayType, lineType, ServiceJourneyType.REF, ptSituationElementType, gqlUtil
      );
      GraphQLOutputType estimatedCallType = EstimatedCallType.create(
          bookingArrangementType, noticeType, quayType, destinationDisplayType, ptSituationElementType, ServiceJourneyType.REF, gqlUtil
      );

      GraphQLOutputType serviceJourneyType = ServiceJourneyType.create(
          bookingArrangementType,
          linkGeometryType,
          operatorType,
          noticeType,
          quayType,
          lineType,
          ptSituationElementType,
          journeyPatternType,
          estimatedCallType,
          TimetabledPassingTimeType.REF,
          gqlUtil
      );

      GraphQLOutputType timetabledPassingTime  = TimetabledPassingTimeType.create(
          bookingArrangementType,
          noticeType,
          quayType,
          destinationDisplayType,
          serviceJourneyType,
          gqlUtil
      );


      GraphQLInputObjectType modesInputType = GraphQLInputObjectType
        .newInputObject()
        .name("Modes")
        .description("Input format for specifying which modes will be allowed for this search. "
            + "If this element is not present, it will default to accessMode/egressMode/directMode "
            + "of foot and all transport modes will be allowed.")
        .field(GraphQLInputObjectField
            .newInputObjectField()
            .name("accessMode")
            .description("The mode used to get from the origin to the access stops in the transit "
                + "network the transit network (first-mile). If the element is not present or null,"
                + "only transit that can be immediately boarded from the origin will be used.")
            .type(STREET_MODE)
            .build())
        .field(GraphQLInputObjectField
            .newInputObjectField()
            .name("egressMode")
            .description("The mode used to get from the egress stops in the transit network to"
                + "the destination (last-mile). If the element is not present or null,"
                + "only transit that can immediately arrive at the origin will be used.")
            .type(STREET_MODE)
            .build())
        .field(GraphQLInputObjectField
            .newInputObjectField()
            .name("directMode")
            .description("The mode used to get from the origin to the destination directly, "
                + "without using the transit network. If the element is not present or null,"
                + "direct travel without using transit will be disallowed.")
            .type(STREET_MODE)
            .build())
        .field(GraphQLInputObjectField
            .newInputObjectField()
            .name("transportMode")
            .description("The allowed modes for the transit part of the trip. Use an empty list "
                + "to disallow transit for this search. If the element is not present or null, "
                + "it will default to all transport modes.")
            .type(new GraphQLList(TRANSPORT_MODE))
            .build())
        .build();

        GraphQLOutputType tripType = createPlanType(
            bookingArrangementType,
            interchangeType,
            linkGeometryType,
            systemNoticeType,
            authorityType,
            operatorType,
            bikeRentalStationType,
            quayType,
            estimatedCallType,
            lineType,
            serviceJourneyType,
            ptSituationElementType

        );

        GraphQLInputObjectType preferredInputType = GraphQLInputObjectType.newInputObject()
                .name("InputPreferred")
                .description("Preferences for trip search.")
                .field(GqlUtil.newIdListInputField(
                    "lines",
                    "Set of ids of lines preferred by user."
                ))
                .field(GqlUtil.newIdListInputField(
                    "authorities",
                    "Set of ids of authorities preferred by user."
                ))
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
                .field(GqlUtil.newIdListInputField(
                    "lines",
                    "Set of ids of lines user prefers not to use."
                ))
                .field(GqlUtil.newIdListInputField(
                    "authorities",
                    "Set of ids of authorities user prefers not to use."
                ))
                .build();

        GraphQLInputObjectType bannedInputType = GraphQLInputObjectType.newInputObject()
                .name("InputBanned")
                .description("Filter trips by disallowing lines involving certain "
                    + "elements. If both lines and authorities are specified, only one must be valid "
                    + "for each line to be banned. If a line is both banned and whitelisted, "
                    + "it will be counted as banned.")
                .field(GqlUtil.newIdListInputField(
                    "lines",
                    "Set of ids for lines that should not be used"
                ))
                .field(GqlUtil.newIdListInputField(
                    "authorities",
                    "Set of ids for authorities that should not be used"
                ))
                // TODO trip ids (serviceJourneys) are expected on format AgencyId:trip-id[:stop ordinal:stop ordinal..] and thus will not work with serviceJourney ids containing ':'.
                // Need to subclass GraphQLPlanner if this field is to be supported
//                                                         .field(GraphQLInputObjectField.newInputObjectField()
//                                                                        .name("serviceJourneys")
//                                                                        .description("Do not use certain named serviceJourneys")
//                                                                        .type(new GraphQLList(Scalars.GraphQLString))
//                                                                        .build())
                .field(GqlUtil.newIdListInputField(
                    "quays",
                    "Set of ids of quays that should not be allowed for boarding or alighting. Trip patterns that travel through the quay will still be permitted."
                ))
                .field(GqlUtil.newIdListInputField(
                    "quaysHard",
                    "Set of ids of quays that should not be allowed for boarding, alighting or traveling thorugh."
                ))
                .field(GqlUtil.newIdListInputField(
                    "serviceJourneys",
                    "Set of ids of service journeys that should not be used."
                ))
                .build();

        /*
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
         */


        GraphQLFieldDefinition tripFieldType = GraphQLFieldDefinition.newFieldDefinition()
                .name("trip")
                .description("Input type for executing a travel search for a trip between two locations. Returns trip patterns describing suggested alternatives for the trip.")
                .type(tripType)
                .argument(GraphQLArgument.newArgument()
                        .name("dateTime")
                        .description("Date and time for the earliest time the user is willing to start the journey (if arriveBy=false/not set) or the latest acceptable time of arriving (arriveBy=true). Defaults to now")
                        .type(gqlUtil.dateTimeScalar)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("searchWindow")
                        .description("The length of the search-window in minutes. This is normally dynamically calculated by the server, but you may override this by setting it. The search-window used in a request is returned in the response metadata. To get the \"next page\" of trips use the metadata(searchWindowUsed and nextWindowDateTime) to create a new request. If not provided the value is resolved depending on the other input parameters, available transit options and realtime changes.")
                        .type(Scalars.GraphQLInt)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("from")
                        .description("The start location")
                        .type(new GraphQLNonNull(LocationInputType.INPUT_TYPE))
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("to")
                        .description("The end location")
                        .type(new GraphQLNonNull(LocationInputType.INPUT_TYPE))
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("wheelchairAccessible")
                        .description("Whether the trip must be wheelchair accessible. NOT IMPLEMENTED")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(routing.request.wheelchairAccessible)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("numTripPatterns")
                        .description("The maximum number of trip patterns to return for this search "
                            + "window. This may cause relevant trips to be missed, so it is preferable "
                            + "to set this high and use other filtering options instead.")
                        .defaultValue(routing.request.numItineraries)
                        .type(Scalars.GraphQLInt)
                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("maximumWalkDistance")
//                        .description("DEPRECATED - Use maxPreTransitWalkDistance/maxTransferWalkDistance instead. " +
//                                "The maximum distance (in meters) the user is willing to walk. Note that trip patterns with " +
//                                "longer walking distances will be penalized, but not altogether disallowed. Maximum allowed value is 15000 m")
//                        .defaultValue(routing.request.maxWalkDistance)
//                        .type(Scalars.GraphQLFloat)
//                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("maxTransferWalkDistance")
//                        .description("The maximum walk distance allowed for transfers.")
//                        .defaultValue(routing.request.maxTransferWalkDistance)
//                        .type(Scalars.GraphQLFloat)
//                        .build())
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
                        .name("bicycleOptimisationMethod")
                        .description("The set of characteristics that the user wants to optimise for during bicycle searches -- defaults to " + reverseMapEnumVal(EnumTypes.BICYCLE_OPTIMISATION_METHOD, routing.request.optimize))
                        .type(EnumTypes.BICYCLE_OPTIMISATION_METHOD)
                        .defaultValue(routing.request.optimize)
                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("arriveBy")
                        .description("Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(routing.request.arriveBy)
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
                        .type(JourneyWhiteListed.INPUT_TYPE)
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
//                .argument(GraphQLArgument.newArgument()
//                         .name("transportSubmodes")
//                         .description("Optional set of allowed submodes per transport mode provided in 'modes'. If at least one submode is set for a transport mode all submodes not set will be disregarded. Note that transportMode must also be included in 'modes' for the submodes to be allowed")
//                        .type(new GraphQLList(transportSubmodeFilterInputType))
//                         .defaultValue(List.of())
//                         .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("minimumTransferTime")
//                        .description("DEPRECATED - Use 'transferSlack/boardSlack/alightSlack' instead.  ")
//                        .type(Scalars.GraphQLInt)
//                        .defaultValue(routing.request.transferSlack)
//                        .build())
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
//                .argument(GraphQLArgument.newArgument()
//                        .name("includePlannedCancellations")
//                        .description("When true, service journeys cancelled in scheduled route data will be included during this search.")
//                        .type(Scalars.GraphQLBoolean)
//                        .defaultValue(defaultRoutingRequest.defaults.includePlannedCancellations)
//                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("ignoreInterchanges")
//                        .description("DEPRECATED - For debugging only. Ignores interchanges defined in timetable data.")
//                        .type(Scalars.GraphQLBoolean)
//                        .defaultValue(defaultRoutingRequest.defaults.ignoreInterchanges)
//                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("locale")
                        .type(EnumTypes.LOCALE)
                        .defaultValue("no")
                        .build())
               .argument(GraphQLArgument.newArgument()
                        .name("transitGeneralizedCostLimit")
                        .description("Set a relative limit for all transit itineraries. The limit "
                            + "is calculated based on the best transit itinerary generalized-cost. "
                            + "Itineraries without transit legs are excluded from this filter. "
                            + "Example: f(x) = 3600 + 2.0 x. If the lowest cost returned is 10 000, "
                            + "then the limit is set to: 3 600 + 2 * 10 000 = 26 600. Then all "
                            + "itineraries with at least one transit leg and a cost above 26 600 "
                            + "is removed from the result. Default: "
                            + RequestFunctions.serialize(routing.request.transitGeneralizedCostLimit)
                        )
                        .type(gqlUtil.doubleFunctionScalar)
                        // There is a bug in the GraphQL lib. The default value is shown as a
                   // `boolean` with value `false`, not the actual value. Hence; The default
                   // is added                    to the description above instead.
                        // .defaultValue(routing.request.transitGeneralizedCostLimit)
                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("compactLegsByReversedSearch")
//                        .description("DEPRECATED - NO EFFECT IN OTP2")
//                        .type(Scalars.GraphQLBoolean)
//                        .defaultValue(false)
//                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("reverseOptimizeOnTheFly")
//                        .description("DEPRECATED - NO EFFECT IN OTP2.")
//                        .type(Scalars.GraphQLBoolean)
//                        .defaultValue(false)
//                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("maxPreTransitTime")
//                        .description("Maximum time for the ride part of \"kiss and ride\" and \"ride and kiss\".")
//                        .type(Scalars.GraphQLInt)
//                        .defaultValue(routing.request.maxPreTransitTime)
//                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("preTransitReluctance")
//                        .description("How much worse driving before and after transit is than riding on transit. Applies to ride and kiss, kiss and ride and park and ride.")
//                        .type(Scalars.GraphQLFloat)
//                        .defaultValue(defaultRoutingRequest.defaults.preTransitReluctance)
//                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("maxPreTransitWalkDistance")
//                        .description("Max walk distance for access/egress legs. NOT IMPLEMENTED")
//                        .type(Scalars.GraphQLFloat)
//                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("useFlex")
//                        .type(Scalars.GraphQLBoolean)
//                        .description("NOT IMPLEMENTED")
//                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("banFirstServiceJourneysFromReuseNo")
//                        .description("How many service journeys used in a tripPatterns should be banned from inclusion in successive tripPatterns. Counting from start of tripPattern.")
//                        .type(Scalars.GraphQLInt)
//                        .defaultValue(defaultRoutingRequest.defaults.banFirstTripsFromReuseNo)
//                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("walkReluctance")
                        .description("Walk cost is multiplied by this value. This is the main parameter to use for limiting walking.")
                        .type(Scalars.GraphQLFloat)
                        .defaultValue(routing.request.walkReluctance)
                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("ignoreMinimumBookingPeriod")
//                        .description("Ignore the MinimumBookingPeriod defined on the ServiceJourney and allow itineraries to start immediately after the current time.")
//                        .type(Scalars.GraphQLBoolean)
//                        .defaultValue(defaultRoutingRequest.defaults.ignoreDrtAdvanceBookMin)
//                        .build())
//                .argument(GraphQLArgument.newArgument()
//                        .name("transitDistanceReluctance")
//                        .description("The extra cost per meter that is travelled by transit. This is a cost point peter meter, so it should in most\n" +
//                                "cases be a very small fraction. The purpose of assigning a cost to distance is often because it correlates with\n" +
//                                "fare prices and you want to avoid situations where you take detours or travel back again even if it is\n" +
//                                "technically faster. Setting this value to 0 turns off the feature altogether.")
//                        .type(Scalars.GraphQLFloat)
//                        .defaultValue(defaultRoutingRequest.defaults.transitDistanceReluctance)
//                        .build())
                .argument(GraphQLArgument.newArgument()
                        .name("debugItineraryFilter")
                        .description("Debug the itinerary-filter-chain. The filters will mark itineraries as deleted, but NOT delete them when this is enabled.")
                        .type(Scalars.GraphQLBoolean)
                        .defaultValue(routing.request.debugItineraryFilter)
                        .build())

                .dataFetcher(environment -> new TransmodelGraphQLPlanner().plan(environment))
                .build();

//        carParkType = GraphQLObjectType.newObject()
//                .name("CarPark")
//                .withInterface(placeInterface)
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("id")
//                        .type(new GraphQLNonNull(Scalars.GraphQLID))
//                        .dataFetcher(environment -> ((CarPark) environment.getSource()).id)
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("name")
//                        .type(new GraphQLNonNull(Scalars.GraphQLString))
//                        .dataFetcher(environment -> ((CarPark) environment.getSource()).name)
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("capacity")
//                        .type(Scalars.GraphQLInt)
//                        .dataFetcher(environment -> ((CarPark) environment.getSource()).maxCapacity)
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("spacesAvailable")
//                        .type(Scalars.GraphQLInt)
//                        .dataFetcher(environment -> ((CarPark) environment.getSource()).spacesAvailable)
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("realtimeOccupancyAvailable")
//                        .type(Scalars.GraphQLBoolean)
//                        .dataFetcher(environment -> ((CarPark) environment.getSource()).realTimeData)
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("longitude")
//                        .type(Scalars.GraphQLFloat)
//                        .dataFetcher(environment -> ((CarPark) environment.getSource()).x)
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("latitude")
//                        .type(Scalars.GraphQLFloat)
//                        .dataFetcher(environment -> ((CarPark) environment.getSource()).y)
//                        .build())
//                .build();

        GraphQLInputObjectType inputPlaceIds = GraphQLInputObjectType.newInputObject()
                .name("InputPlaceIds")
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

    //                                String multiModalMode=environment.getArgument("multiModalMode");
    //                                if ("parent".equals(multiModalMode)){
    //                                    stops = stops.map(s -> getParentStopPlace(s).orElse(s));
    //                                }
    //                                List<Stop> stopList=stops.distinct().collect(Collectors.toList());
    //                                if ("all".equals(multiModalMode)) {
    //                                    stopList.addAll(stopList.stream().map(s -> getParentStopPlace(s).orElse(null)).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
    //                                }
    //                                return stopList;
    //                            else {
    //                                return index.getLuceneIndex().query(environment.getArgument("name"), true, true, false)
    //                                        .stream()
    //                                        .map(result -> index.stopForId.get(mapper.fromIdString(result.id)));
    //                            }
    // TODO OTP2 - FIX THIS, THIS IS A BUG
    //List<String> privateCodes=environment.getArgument("privateCodes");
    // TODO OTP2 - Use FeedScoped ID
    //.filter(t -> CollectionUtils.isEmpty(privateCodes) || privateCodes.contains(t.getTripPrivateCode()))
    //                .field(GraphQLFieldDefinition.newFieldDefinition()
    //                        .name("carPark")
    //                        .description("Get a single car park based on its id")
    //                        .type(carParkType)
    //                        .argument(GraphQLArgument.newArgument()
    //                                .name("id")
    //                                .type(new GraphQLNonNull(Scalars.GraphQLString))
    //                                .build())
    //                        .dataFetcher(environment -> index.graph.getService(CarParkService.class)
    //                                .getCarParks()
    //                                .stream()
    //                                .filter(carPark -> carPark.id.equals(environment.getArgument("id")))
    //                                .findFirst()
    //                                .orElse(null))
    //                        .build())
    //                .field(GraphQLFieldDefinition.newFieldDefinition()
    //                        .name("carParks")
    //                        .description("Get all car parks")
    //                        .type(new GraphQLNonNull(new GraphQLList(carParkType)))
    //                        .argument(GraphQLArgument.newArgument()
    //                                .name("ids")
    //                                .type(new GraphQLList(Scalars.GraphQLString))
    //                                .build())
    //                        .dataFetcher(environment -> {
    //                            if ((environment.getArgument("ids") instanceof List)) {
    //                                Map<String, CarPark> carParks = index.graph.getService(CarParkService.class).getCarParkById();
    //                                return ((List<String>) environment.getArgument("ids"))
    //                                        .stream()
    //                                        .map(carParks::get)
    //                                        .collect(Collectors.toList());
    //                            }
    //                            return new ArrayList<>(index.graph.getService(CarParkService.class).getCarParks());
    //                        })
    //                        .build())
    //                .field(GraphQLFieldDefinition.newFieldDefinition()
    //                        .name("notices")
    //                        .description("Get all notices")
    //                        .type(new GraphQLNonNull(new GraphQLList(noticeType)))
    //                        .dataFetcher(environment -> index.getNoticesByEntity().values())
    //                        .build())
    GraphQLObjectType queryType = GraphQLObjectType
        .newObject()
        .name("QueryType")
        .field(tripFieldType)
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("stopPlace")
            .description("Get a single stopPlace based on its id)")
            .type(stopPlaceType)
            .argument(GraphQLArgument
                .newArgument()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .dataFetcher(
                env -> StopPlaceType.fetchStopPlaceById(
                    TransitIdMapper.mapIDToDomain(env.getArgument("id")),
                    env
                )
            )
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("stopPlaces")
            .description("Get all stopPlaces")
            .type(new GraphQLNonNull(new GraphQLList(stopPlaceType)))
            .argument(GraphQLArgument
                .newArgument()
                .name("ids")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .dataFetcher(env -> {
              if ((env.getArgument("ids") instanceof List)) {
                return ((List<String>) env.getArgument("ids"))
                    .stream()
                    .map(TransitIdMapper::mapIDToDomain)
                    .map(id ->
                        StopPlaceType.fetchStopPlaceById(id, env)
                    )
                    .collect(Collectors.toList());
              }
              return new ArrayList<>(GqlUtil.getRoutingService(env).getStations());
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("stopPlacesByBbox")
            .description("Get all stop places within the specified bounding box")
            .type(new GraphQLNonNull(new GraphQLList(stopPlaceType)))
            .argument(GraphQLArgument
                .newArgument()
                .name("minimumLatitude")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("minimumLongitude")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("maximumLatitude")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("maximumLongitude")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("authority")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("multiModalMode")
                .type(MULTI_MODAL_MODE)
                .description(
                    "MultiModalMode for query. To control whether multi modal parent stop places, their mono modal children or both are included in the response."
                        + " Does not affect mono modal stop places that do not belong to a multi modal stop place.")
                .defaultValue("parent")
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("filterByInUse")
                .description(
                    "If true only stop places with at least one visiting line are included.")
                .type(Scalars.GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .build())
            .dataFetcher(env -> {
                double minLat = env.getArgument("minimumLatitude");
                double minLon = env.getArgument("minimumLongitude");
                double maxLat = env.getArgument("maximumLatitude");
                double maxLon = env.getArgument("maximumLongitude");
                String authority = env.getArgument("authority");
                boolean filterByInUse = TRUE.equals(env.getArgument("filterByInUse"));
                String multiModalMode = env.getArgument("multiModalMode");

                return StopPlaceType.fetchStopPlaces(minLat, minLon, maxLat, maxLon, authority, filterByInUse, multiModalMode, env);
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("quay")
            .description("Get a single quay based on its id)")
            .type(quayType)
            .argument(GraphQLArgument
                .newArgument()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment).getStopForId(TransitIdMapper.mapIDToDomain(
                  environment.getArgument("id")));
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("quays")
            .description("Get all quays")
            .type(new GraphQLNonNull(new GraphQLList(quayType)))
            .argument(GraphQLArgument
                .newArgument()
                .name("ids")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("name")
                .type(Scalars.GraphQLString)
                .build())
            .dataFetcher(environment -> {
              if ((environment.getArgument("ids") instanceof List)) {
                if (environment
                    .getArguments()
                    .entrySet()
                    .stream()
                    .filter(stringObjectEntry -> stringObjectEntry.getValue() != null)
                    .collect(Collectors.toList())
                    .size() != 1) {
                  throw new IllegalArgumentException("Unable to combine other filters with ids");
                }
                RoutingService routingService = GqlUtil.getRoutingService(environment);
                return ((List<String>) environment.getArgument("ids"))
                    .stream()
                    .map(id -> routingService.getStopForId(TransitIdMapper.mapIDToDomain(id)))
                    .collect(Collectors.toList());
              }
              if (environment.getArgument("name") == null) {
                return GqlUtil.getRoutingService(environment).getAllStops();
              }
              //                            else {
              //                                return index.getLuceneIndex().query(environment.getArgument("name"), true, true, false)
              //                                        .stream()
              //                                        .map(result -> index.stopForId.get(mapper.fromIdString(result.id)));
              //                            }
              return emptyList();
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("quaysByBbox")
            .description("Get all quays within the specified bounding box")
            .type(new GraphQLNonNull(new GraphQLList(quayType)))
            .argument(GraphQLArgument
                .newArgument()
                .name("minimumLatitude")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("minimumLongitude")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("maximumLatitude")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("maximumLongitude")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("authority")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("filterByInUse")
                .description("If true only quays with at least one visiting line are included.")
                .type(Scalars.GraphQLBoolean)
                .defaultValue(Boolean.FALSE)
                .build())
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment)
                  .getStopsByBoundingBox(
                      environment.getArgument("minimumLatitude"),
                      environment.getArgument("minimumLongitude"),
                      environment.getArgument("maximumLatitude"),
                      environment.getArgument("maximumLongitude")
                  )
                  .stream()
                  .filter(stop -> environment.getArgument("authority") == null || stop
                      .getId()
                      .getFeedId()
                      .equalsIgnoreCase(environment.getArgument("authority")))
                  .filter(stop -> {
                    boolean filterByInUse = TRUE.equals(environment.getArgument("filterByInUse"));
                    boolean inUse = !GqlUtil
                        .getRoutingService(environment)
                        .getPatternsForStop(stop, true)
                        .isEmpty();
                    return !filterByInUse || inUse;
                  })
                  .collect(Collectors.toList());
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("quaysByRadius")
            .description(
                "Get all quays within the specified walking radius from a location. The returned type has two fields quay and distance")
            .type(relay.connectionType("quayAtDistance",
                relay.edgeType("quayAtDistance", quayAtDistance, null, new ArrayList<>()),
                new ArrayList<>()
            ))
            .argument(GraphQLArgument
                .newArgument()
                .name("latitude")
                .description("Latitude of the location")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("longitude")
                .description("Longitude of the location")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("radius")
                .description("Radius via streets (in meters) to search for from the specified location")
                .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("authority")
                .type(Scalars.GraphQLString)
                .build())
            .arguments(relay.getConnectionFieldArguments())
            .dataFetcher(environment -> {
              List<NearbyStop> stops;
              try {
                stops = GqlUtil.getRoutingService(environment)
                    .findClosestStops(environment.getArgument("latitude"),
                        environment.getArgument("longitude"),
                        environment.getArgument("radius")
                    )
                    .stream()
                    .filter(stopAtDistance -> environment.getArgument("authority") == null
                        || stopAtDistance.stop
                        .getId()
                        .getFeedId()
                        .equalsIgnoreCase(environment.getArgument("authority")))
                    .sorted(Comparator.comparing(s -> s.distance))
                    .collect(Collectors.toList());
              }
              catch (RoutingValidationException e) {
                LOG.warn(
                    "findClosestPlacesByWalking failed with exception, returning empty list of places. ",
                    e
                );
                stops = List.of();
              }

              if (CollectionUtils.isEmpty(stops)) {
                return new DefaultConnection<>(
                    Collections.emptyList(),
                    new DefaultPageInfo(null, null, false, false)
                );
              }
              return new SimpleListConnection<>(stops).get(environment);
            })
            .build())
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
              .type(new GraphQLNonNull(Scalars.GraphQLFloat))
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
              .type(new GraphQLList(TRANSPORT_MODE))
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
              .type(inputPlaceIds)
              .build())
          .argument(GraphQLArgument.newArgument()
              .name("multiModalMode")
              .type(MULTI_MODAL_MODE)
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
            @SuppressWarnings("rawtypes") Map filterByIds = environment.getArgument("filterByIds");
            if (filterByIds != null) {
              filterByStops = toIdList(((List<String>) filterByIds.get("quays")));
              filterByRoutes = toIdList(((List<String>) filterByIds.get("lines")));
              filterByBikeRentalStations = filterByIds.get("bikeRentalStations") != null ? (List<String>) filterByIds.get("bikeRentalStations") : Collections.emptyList();
              filterByBikeParks = filterByIds.get("bikeParks") != null ? (List<String>) filterByIds.get("bikeParks") : Collections.emptyList();
              filterByCarParks = filterByIds.get("carParks") != null ? (List<String>) filterByIds.get("carParks") : Collections.emptyList();
            }

            List<TransitMode> filterByTransportModes = environment.getArgument("filterByModes");
            List<TransmodelPlaceType> placeTypes = environment.getArgument("filterByPlaceTypes");
            if (CollectionUtils.isEmpty(placeTypes)) {
              placeTypes = Arrays.asList(TransmodelPlaceType.values());
            }
            List<PlaceType> filterByPlaceTypes = TransmodelMappingUtil.mapPlaceTypes(placeTypes);

            // Need to fetch more than requested no of places if stopPlaces are allowed, as this requires fetching potentially multiple quays for the same stop place and mapping them to unique stop places.
            int orgMaxResults = environment.getArgument("maximumResults");
            int maxResults = orgMaxResults;
            if (placeTypes.contains(TransmodelPlaceType.STOP_PLACE)) {
              maxResults *= 5;
            }

            List<PlaceAtDistance> places;
            places = GqlUtil.getRoutingService(environment).findClosestPlaces(
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
                GqlUtil.getRoutingService(environment)
            );

            places = TransmodelMappingUtil.convertQuaysToStopPlaces(placeTypes, places,  environment.getArgument("multiModalMode"), GqlUtil.getRoutingService(environment)).stream().limit(orgMaxResults).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(places)) {
              return new DefaultConnection<>(Collections.emptyList(), new DefaultPageInfo(null, null, false, false));
            }
            return new SimpleListConnection(places).get(environment);
          })
          .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("authority")
            .description("Get an authority by ID")
            .type(authorityType)
            .argument(GraphQLArgument
                .newArgument()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment).getAgencyForId(
                  TransitIdMapper.mapIDToDomain(environment.getArgument("id")));
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("authorities")
            .description("Get all authorities")
            .type(new GraphQLNonNull(new GraphQLList(authorityType)))
            .dataFetcher(environment -> {
              return new ArrayList<>(GqlUtil.getRoutingService(environment).getAgencies());
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("operator")
            .description("Get a operator by ID")
            .type(operatorType)
            .argument(GraphQLArgument
                .newArgument()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment)
                  .getOperatorForId()
                  .get(TransitIdMapper.mapIDToDomain(environment.getArgument("id")));
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("operators")
            .description("Get all operators")
            .type(new GraphQLNonNull(new GraphQLList(operatorType)))
            .dataFetcher(environment -> {
              return new ArrayList<>(GqlUtil.getRoutingService(environment).getAllOperators());
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("line")
            .description("Get a single line based on its id")
            .type(lineType)
            .argument(GraphQLArgument
                .newArgument()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID))
                .build())
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment).getRouteForId(TransitIdMapper
                  .mapIDToDomain(environment.getArgument("id")));
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("lines")
            .description("Get all lines")
            .type(new GraphQLNonNull(new GraphQLList(lineType)))
            .argument(GraphQLArgument
                .newArgument()
                .name("ids")
                .type(new GraphQLList(Scalars.GraphQLID))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("name")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("publicCode")
                .type(Scalars.GraphQLString)
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("publicCodes")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("transportModes")
                .type(new GraphQLList(TRANSPORT_MODE))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("authorities")
                .description("Set of ids of authorities to fetch lines for.")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .dataFetcher(environment -> {
              if ((environment.getArgument("ids") instanceof List)) {
                if (environment
                    .getArguments()
                    .entrySet()
                    .stream()
                    .filter(it -> it.getValue() != null)
                    .count() != 1) {
                  throw new IllegalArgumentException("Unable to combine other filters with ids");
                }
                return ((List<String>) environment.getArgument("ids"))
                    .stream()
                    .map(id1 -> TransitIdMapper.mapIDToDomain(id1))
                    .map(id -> {
                      return GqlUtil.getRoutingService(environment).getRouteForId(id);
                    })
                    .collect(Collectors.toList());
              }
              Stream<Route> stream = GqlUtil.getRoutingService(environment).getAllRoutes().stream();
              if (environment.getArgument("name") != null) {
                stream = stream
                    .filter(route -> route.getLongName() != null)
                    .filter(route -> route
                        .getLongName()
                        .toLowerCase()
                        .startsWith(((String) environment.getArgument("name")).toLowerCase()));
              }
              if (environment.getArgument("publicCode") != null) {
                stream = stream
                    .filter(route -> route.getShortName() != null)
                    .filter(route -> route
                        .getShortName()
                        .equals(environment.getArgument("publicCode")));
              }
              if (environment.getArgument("publicCodes") instanceof List) {
                Set<String> publicCodes = Set.copyOf(environment.getArgument("publicCodes"));
                stream = stream
                    .filter(route -> route.getShortName() != null)
                    .filter(route -> publicCodes.contains(route.getShortName()));
              }
              if (environment.getArgument("transportModes") != null) {

                Set<TraverseMode> modes = (
                    (List<TraverseMode>) environment.getArgument("transportModes")
                ).stream().filter(TraverseMode::isTransit).collect(Collectors.toSet());
                // TODO OTP2 - FIX THIS, THIS IS A BUG
                stream = stream.filter(route -> modes.contains(route.getMode()));
              }
              if ((environment.getArgument("authorities") instanceof Collection)) {
                Collection<String> authorityIds = environment.getArgument("authorities");
                stream = stream.filter(route -> route.getAgency() != null && authorityIds.contains(
                    route.getAgency().getId().getId()));
              }
              return stream.collect(Collectors.toList());
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("serviceJourney")
            .description("Get a single service journey based on its id")
            .type(serviceJourneyType)
            .argument(GraphQLArgument
                .newArgument()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment)
                  .getTripForId()
                  .get(TransitIdMapper.mapIDToDomain(environment.getArgument("id")));
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("serviceJourneys")
            .description("Get all service journeys")
            .type(new GraphQLNonNull(new GraphQLList(serviceJourneyType)))
            .argument(GraphQLArgument
                .newArgument()
                .name("lines")
                .description("Set of ids of lines to fetch serviceJourneys for.")
                .type(new GraphQLList(Scalars.GraphQLID))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("privateCodes")
                .description("Set of ids of private codes to fetch serviceJourneys for.")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("activeDates")
                .description("Set of ids of active dates to fetch serviceJourneys for.")
                .type(new GraphQLList(gqlUtil.dateScalar))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("authorities")
                .description("Set of ids of authorities to fetch serviceJourneys for.")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .dataFetcher(environment -> {
              List<FeedScopedId> lineIds = mapIDsToDomain(environment.getArgument("lines"));
              //List<String> privateCodes=environment.getArgument("privateCodes");
              List<Long> activeDates = environment.getArgument("activeDates");
              // TODO OTP2 - Use FeedScoped ID
              List<String> authorities = environment.getArgument("authorities");
              return GqlUtil.getRoutingService(environment)
                  .getTripForId()
                  .values()
                  .stream()
                  .filter(t -> CollectionUtils.isEmpty(lineIds) || lineIds.contains(t
                      .getRoute()
                      .getId()))
                  //.filter(t -> CollectionUtils.isEmpty(privateCodes) || privateCodes.contains(t.getTripPrivateCode()))
                  .filter(t -> CollectionUtils.isEmpty(authorities) || authorities.contains(t
                      .getRoute()
                      .getAgency()
                      .getId()
                      .getId()))
                  .filter(t -> {
                    return CollectionUtils.isEmpty(activeDates)
                        || GqlUtil.getRoutingService(environment)
                        .getCalendarService()
                        .getServiceDatesForServiceId(t.getServiceId())
                        .stream()
                        .anyMatch(sd -> activeDates.contains(gqlUtil.serviceDateMapper.serviceDateToSecondsSinceEpoch(
                            sd)));
                  })
                  .collect(Collectors.toList());
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("bikeRentalStations")
            .description("Get all bike rental stations")
            .argument(GraphQLArgument
                .newArgument()
                .name("ids")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .type(new GraphQLNonNull(new GraphQLList(bikeRentalStationType)))
            .dataFetcher(environment -> {
              Collection<BikeRentalStation> all = new ArrayList<>(GqlUtil
                  .getRoutingService(environment)
                  .getBikerentalStationService()
                  .getBikeRentalStations());
              List<String> filterByIds = environment.getArgument("ids");
              if (!CollectionUtils.isEmpty(filterByIds)) {
                return all
                    .stream()
                    .filter(station -> filterByIds.contains(station.id))
                    .collect(Collectors.toList());
              }
              return all;
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("bikeRentalStation")
            .description("Get all bike rental stations")
            .type(bikeRentalStationType)
            .argument(GraphQLArgument
                .newArgument()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment)
                  .getBikerentalStationService()
                  .getBikeRentalStations()
                  .stream()
                  .filter(bikeRentalStation -> bikeRentalStation.id.equals(environment.getArgument(
                      "id")))
                  .findFirst()
                  .orElse(null);
            })
            .build())

        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("bikeRentalStationsByBbox")
            .description(
                "Get all bike rental stations within the specified bounding box.")
            .type(new GraphQLNonNull(new GraphQLList(bikeRentalStationType)))
            .argument(GraphQLArgument
                .newArgument()
                .name("minimumLatitude")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("minimumLongitude")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("maximumLatitude")
                .type(Scalars.GraphQLFloat)
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("maximumLongitude")
                .type(Scalars.GraphQLFloat)
                .build())
            .dataFetcher(environment -> GqlUtil
                .getRoutingService(environment)
                .getBikerentalStationService()
                .getBikeRentalStationForEnvelope(new Envelope(new Coordinate(
                    environment.getArgument("minimumLongitude"),
                    environment.getArgument("minimumLatitude")
                ), new Coordinate(
                    environment.getArgument("maximumLongitude"),
                    environment.getArgument("maximumLatitude")
                ))))
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("bikePark")
            .description("Get a single bike park based on its id")
            .type(bikeParkType)
            .argument(GraphQLArgument
                .newArgument()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLString))
                .build())
            .dataFetcher(environment -> {
              return GqlUtil.getRoutingService(environment)
                  .getBikerentalStationService()
                  .getBikeParks()
                  .stream()
                  .filter(bikePark -> bikePark.id.equals(environment.getArgument("id")))
                  .findFirst()
                  .orElse(null);
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("bikeParks")
            .description("Get all bike parks")
            .type(new GraphQLNonNull(new GraphQLList(bikeParkType)))
            .dataFetcher(environment -> {
              return new ArrayList<>(GqlUtil.getRoutingService(environment)
                  .getBikerentalStationService()
                  .getBikeParks());
            })
            .build())
        //                .field(GraphQLFieldDefinition.newFieldDefinition()
        //                        .name("carPark")
        //                        .description("Get a single car park based on its id")
        //                        .type(carParkType)
        //                        .argument(GraphQLArgument.newArgument()
        //                                .name("id")
        //                                .type(new GraphQLNonNull(Scalars.GraphQLString))
        //                                .build())
        //                        .dataFetcher(environment -> index.graph.getService(CarParkService.class)
        //                                .getCarParks()
        //                                .stream()
        //                                .filter(carPark -> carPark.id.equals(environment.getArgument("id")))
        //                                .findFirst()
        //                                .orElse(null))
        //                        .build())
        //                .field(GraphQLFieldDefinition.newFieldDefinition()
        //                        .name("carParks")
        //                        .description("Get all car parks")
        //                        .type(new GraphQLNonNull(new GraphQLList(carParkType)))
        //                        .argument(GraphQLArgument.newArgument()
        //                                .name("ids")
        //                                .type(new GraphQLList(Scalars.GraphQLString))
        //                                .build())
        //                        .dataFetcher(environment -> {
        //                            if ((environment.getArgument("ids") instanceof List)) {
        //                                Map<String, CarPark> carParks = index.graph.getService(CarParkService.class).getCarParkById();
        //                                return ((List<String>) environment.getArgument("ids"))
        //                                        .stream()
        //                                        .map(carParks::get)
        //                                        .collect(Collectors.toList());
        //                            }
        //                            return new ArrayList<>(index.graph.getService(CarParkService.class).getCarParks());
        //                        })
        //                        .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("routingParameters")
            .description("Get default routing parameters.")
            .type(this.routing.graphQLType)
            .dataFetcher(environment -> routing.request)
            .build())
        //                .field(GraphQLFieldDefinition.newFieldDefinition()
        //                        .name("notices")
        //                        .description("Get all notices")
        //                        .type(new GraphQLNonNull(new GraphQLList(noticeType)))
        //                        .dataFetcher(environment -> index.getNoticesByEntity().values())
        //                        .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("situations")
            .description("Get all active situations.")
            .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
            .argument(GraphQLArgument
                .newArgument()
                .name("authorities")
                .description("Filter by reporting authorities.")
                .type(new GraphQLList(Scalars.GraphQLString))
                .build())
            .argument(GraphQLArgument
                .newArgument()
                .name("severities")
                .description("Filter by severity.")
                .type(new GraphQLList(EnumTypes.SEVERITY))
                .build())
            .dataFetcher(environment -> {
              Collection<TransitAlert> alerts = GqlUtil.getRoutingService(environment)
                  .getTransitAlertService()
                  .getAllAlerts();
              if ((environment.getArgument("authorities") instanceof List)) {
                List<String> authorities = environment.getArgument("authorities");
                alerts = alerts
                    .stream()
                    .filter(alert -> authorities.contains(alert.getFeedId()))
                    .collect(Collectors.toSet());
              }
              if ((environment.getArgument("severities") instanceof List)) {
                List<String> severities = environment.getArgument("severities");
                alerts = alerts
                    .stream()
                    .filter(alert -> severities.contains(alert.severity))
                    .collect(Collectors.toSet());
              }
              return alerts;
            })
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("serverInfo")
            .description("Get OTP server information")
            .type(new GraphQLNonNull(serverInfoType))
            .dataFetcher(e -> MavenVersion.VERSION)
            .build())
        .build();

        Set<GraphQLType> dictionary = new HashSet<>();
        dictionary.add(placeInterface);
        dictionary.add(timetabledPassingTime);
        dictionary.add(Relay.pageInfoType);

        return GraphQLSchema.newSchema().query(queryType).build(dictionary);
    }

  //    private BookingArrangement getBookingArrangementForTripTimeShort(TripTimeShort tripTimeShort) {
//        Trip trip = index.tripForId.get(tripTimeShort.tripId);
//        if (trip == null) {
//            return null;
//        }
//        TripPattern tripPattern = index.patternForTrip.get(trip);
//        if (tripPattern == null || tripPattern.stopPattern == null) {
//            return null;
//        }
//        return tripPattern.stopPattern.bookingArrangements[tripTimeShort.stopIndex];
//    }

    private List<FeedScopedId> toIdList(List<String> ids) {
        if (ids == null) return Collections.emptyList();
        return ids.stream().map(id -> TransitIdMapper.mapIDToDomain(id)).collect(Collectors.toList());
    }

    public GraphQLObjectType createPlanType(
        GraphQLOutputType bookingArrangementType,
        GraphQLOutputType interchangeType,
        GraphQLOutputType linkGeometryType,
        GraphQLOutputType systemNoticeType,
        GraphQLOutputType authorityType,
        GraphQLOutputType operatorType,
        GraphQLOutputType bikeRentalStationType,
        GraphQLOutputType quayType,
        GraphQLOutputType estimatedCallType,
        GraphQLOutputType lineType,
        GraphQLOutputType serviceJourneyType,
        GraphQLOutputType ptSituationElementType
        ) {
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
                        .dataFetcher(environment ->
                            ((Place) environment.getSource()).vertexType.equals(VertexType.TRANSIT)
                                ? GqlUtil.getRoutingService(environment).getStopForId(((Place) environment.getSource()).stopId)
                                : null
                        )
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("bikeRentalStation")
                        .type(bikeRentalStationType)
                        .description("The bike rental station related to the place")
                        .dataFetcher(environment -> {
                          return ((Place) environment.getSource()).vertexType.equals(VertexType.BIKESHARE) ?
                                  GqlUtil
                                      .getRoutingService(environment)
                                      .getBikerentalStationService()
                                          .getBikeRentalStations()
                                          .stream()
                                          .filter(bikeRentalStation -> bikeRentalStation.id.equals(((Place) environment.getSource()).bikeShareId))
                                          .findFirst()
                                          .orElse(null)
                                  : null;
                        })
                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("bikePark")
//                        .type(bikeParkType)
//                        .description("The bike parking related to the place")
//                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.BIKEPARK) ?
//                                index.graph.getService(BikeRentalStationService.class)
//                                        .getBikeParks()
//                                        .stream()
//                                        .filter(bikePark -> bikePark.id.equals(((Place) environment.getSource()).bikeParkId))
//                                        .findFirst()
//                                        .orElse(null)
//                                : null)
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("carPark")
//                        .type(carParkType)
//                        .description("The car parking related to the place")
//                        .dataFetcher(environment -> ((Place) environment.getSource()).vertexType.equals(VertexType.PARKANDRIDE) ?
//                                index.graph.getService(CarParkService.class)
//                                        .getCarParks()
//                                        .stream()
//                                        .filter(carPark -> carPark.id.equals(((Place) environment.getSource()).carParkId))
//                                        .findFirst()
//                                        .orElse(null)
//                                : null)
//                        .build())
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
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("legStepText")
//                        .description("Direction information as readable text.")
//                        .type(Scalars.GraphQLString)
//                        .argument(GraphQLArgument.newArgument()
//                                .name("locale")
//                                .type(localeEnum)
//                                .defaultValue("no")
//                                .build())
//                        .dataFetcher(environment -> ((WalkStep) environment.getSource()).getLegStepText(environment))
//                        .build())
                .build();

        final GraphQLObjectType legType = GraphQLObjectType.newObject()
                .name("Leg")
                .description("Part of a trip pattern. Either a ride on a public transport vehicle or access or path link to/from/between places")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("aimedStartTime")
                        .description("The aimed date and time this leg starts.")
                        .type(gqlUtil.dateTimeScalar)
                        .dataFetcher(// startTime is already adjusted for realtime - need to subtract delay to get aimed time
                                environment -> ((Leg) environment.getSource()).startTime.getTimeInMillis() -
                                        (1000 * ((Leg) environment.getSource()).departureDelay))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("expectedStartTime")
                        .description("The expected, realtime adjusted date and time this leg starts.")
                        .type(gqlUtil.dateTimeScalar)
                        .dataFetcher(
                                environment -> ((Leg) environment.getSource()).startTime.getTimeInMillis())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("aimedEndTime")
                        .description("The aimed date and time this leg ends.")
                        .type(gqlUtil.dateTimeScalar)
                        .dataFetcher(// endTime is already adjusted for realtime - need to subtract delay to get aimed time
                                environment -> ((Leg) environment.getSource()).endTime.getTimeInMillis() -
                                        (1000 * ((Leg) environment.getSource()).arrivalDelay))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("expectedEndTime")
                        .description("The expected, realtime adjusted date and time this leg ends.")
                        .type(gqlUtil.dateTimeScalar)
                        .dataFetcher(
                                environment -> ((Leg) environment.getSource()).endTime.getTimeInMillis())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("mode")
                        .description("The mode of transport or access (e.g., foot) used when traversing this leg.")
                        .type(MODE)
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
                        .dataFetcher(environment -> ((Leg) environment.getSource()).getAgency())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("operator")
                        .description("For ride legs, the operator used for this legs. For non-ride legs, null.")
                        .type(operatorType)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).getOperator())
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
                        .description("EstimatedCall for the quay where the leg originates.")
                        .type(estimatedCallType)
                        .dataFetcher(environment -> tripTimeShortHelper.getTripTimeShortForFromPlace(environment.getSource(), gqlUtil.getRoutingService(environment)))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("toEstimatedCall")
                        .description("EstimatedCall for the quay where the leg ends.")
                        .type(estimatedCallType)
                        .dataFetcher(environment -> tripTimeShortHelper.getTripTimeShortForToPlace(environment.getSource(), gqlUtil.getRoutingService(environment)))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("line")
                        .description("For ride legs, the line. For non-ride legs, null.")
                        .type(lineType)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).getRoute())
                    .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourney")
                        .description("For ride legs, the service journey. For non-ride legs, null.")
                        .type(serviceJourneyType)
                        .dataFetcher(environment -> ((Leg) environment.getSource()).getTrip())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("intermediateQuays")
                        .description("For ride legs, intermediate quays between the Place where the leg originates and the Place where the leg ends. For non-ride legs, empty list.")
                        .type(new GraphQLNonNull(new GraphQLList(quayType)))
                        .dataFetcher(environment -> {
                                List<StopArrival> stops = ((Leg) environment.getSource()).intermediateStops;
                                if (stops == null || stops.isEmpty()) {
                                    return List.of();
                                }
                                else {
                                    return (stops.stream()
                                            .filter(stop -> stop.place.stopId != null)
                                            .map(s -> {
                                              return GqlUtil.getRoutingService(environment).getStopForId(s.place.stopId);
                                            })
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList()));
                                }
                            })
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("intermediateEstimatedCalls")
                        .description("For ride legs, estimated calls for quays between the Place where the leg originates and the Place where the leg ends. For non-ride legs, empty list.")
                        .type(new GraphQLNonNull(new GraphQLList(estimatedCallType)))
                        .dataFetcher(environment -> tripTimeShortHelper.getIntermediateTripTimeShortsForLeg((environment.getSource()), GqlUtil.getRoutingService(environment)))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("serviceJourneyEstimatedCalls")
                        .description("For ride legs, all estimated calls for the service journey. For non-ride legs, empty list.")
                        .type(new GraphQLNonNull(new GraphQLList(estimatedCallType)))
                        .dataFetcher(environment -> {
                          return tripTimeShortHelper.getAllTripTimeShortsForLegsTrip(environment.getSource(),
                              GqlUtil.getRoutingService(environment)
                          );
                        })
                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition()
//                        .name("via")
//                        .description("Do we continue from a specified via place")
//                        .type(Scalars.GraphQLBoolean)
//                        .dataFetcher(environment -> ((Leg) environment.getSource()).intermediatePlace)
//                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("situations")
                        .description("All relevant situations for this leg")
                        .type(new GraphQLNonNull(new GraphQLList(ptSituationElementType)))
                        .dataFetcher(environment -> ((Leg) environment.getSource()).transitAlerts)
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
                        .type(gqlUtil.dateTimeScalar)
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).startTime().getTime().getTime())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("endTime")
                        .description("Time that the trip arrives.")
                        .type(gqlUtil.dateTimeScalar)
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
                        .dataFetcher(environment -> ((Itinerary) environment.getSource()).distanceMeters())
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


        GraphQLObjectType tripMetadataType = TripMetadataType.create(gqlUtil);

        return GraphQLObjectType.newObject()
                .name("Trip")
                .description("Description of a travel between two places.")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("dateTime")
                        .description("The time and date of travel")
                        .type(gqlUtil.dateTimeScalar)
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


    //private <T extends Object> List<String> reverseMapEnumVals(GraphQLEnumType enumType, Collection<T> otpVals) {
    //    return enumType.getValues().stream().filter(e -> otpVals.contains(e.getValue())).map(e -> e.getName()).collect(Collectors.toList());
    //}

    private static String reverseMapEnumVal(GraphQLEnumType enumType, Object otpVal) {
        return enumType.getValues().stream().filter(e -> e.getValue().equals(otpVal)).findFirst().get().getName();
    }
}
