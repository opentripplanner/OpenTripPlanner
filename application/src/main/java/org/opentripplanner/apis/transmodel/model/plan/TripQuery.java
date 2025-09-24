package org.opentripplanner.apis.transmodel.model.plan;

import static org.opentripplanner.apis.transmodel.model.framework.StreetModeDurationInputType.mapDurationForStreetModeGraphQLValue;

import graphql.Scalars;
import graphql.language.NullValue;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLPlanner;
import org.opentripplanner.apis.transmodel.model.DefaultRouteRequestType;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.TransportModeSlack;
import org.opentripplanner.apis.transmodel.model.framework.LocationInputType;
import org.opentripplanner.apis.transmodel.model.framework.PassThroughPointInputType;
import org.opentripplanner.apis.transmodel.model.framework.PenaltyForStreetModeType;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;

public class TripQuery {

  public static final String ACCESS_EGRESS_PENALTY = "accessEgressPenalty";
  public static final String MAX_ACCESS_EGRESS_DURATION_FOR_MODE = "maxAccessEgressDurationForMode";
  public static final String MAX_DIRECT_DURATION_FOR_MODE = "maxDirectDurationForMode";
  public static final String TRIP_VIA_PARAMETER = "via";
  public static final String DOC_VIA =
    """
    The list of via locations the journey is required to visit. All locations are
    visited in the order they are listed.
    """;

  private final TransmodelGraphQLPlanner graphQLPlanner;

  public TripQuery(FeedScopedIdMapper idMapper) {
    this.graphQLPlanner = new TransmodelGraphQLPlanner(idMapper);
  }

  public GraphQLFieldDefinition create(
    DefaultRouteRequestType routing,
    TransitTuningParameters transitTuningParameters,
    GraphQLOutputType tripType,
    GraphQLInputObjectType durationPerStreetModeType,
    GraphQLInputObjectType penaltyForStreetMode,
    GraphQLScalarType dateTimeScalar
  ) {
    RoutingPreferences preferences = routing.request.preferences();

    return GraphQLFieldDefinition.newFieldDefinition()
      .name("trip")
      .description(
        "Input type for executing a travel search for a trip between two locations. Returns " +
        "trip patterns describing suggested alternatives for the trip."
      )
      .type(new GraphQLNonNull(tripType))
      .withDirective(TransmodelDirectives.TIMING_DATA)
      .argument(
        GraphQLArgument.newArgument()
          .name("dateTime")
          .description(
            "The date and time for the earliest time the user is willing to start the journey " +
            "(if `false` or not set) or the latest acceptable time of arriving " +
            "(`true`). Defaults to now."
          )
          .type(dateTimeScalar)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("bookingTime")
          .description(
            """
            The date and time for the latest time the user is expected to book the journey.
            Normally this is when the search is performed (now), plus a small grace period to
            complete the booking. Services which must be booked before this time is excluded. The
            `latestBookingTime` and `minimumBookingPeriod` in `BookingArrangement` (flexible
            services only) is used to enforce this. If this parameter is _not set_, no booking-time
            restrictions are applied - all journeys are listed.
            """
          )
          .type(dateTimeScalar)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("searchWindow")
          .description(
            """
            The length of the search-window in minutes. This parameter is optional.

            The search-window is defined as the duration between the earliest-departure-time (EDT)
            and the latest-departure-time (LDT). OTP will search for all itineraries in this
            departure window. If `arriveBy=true` the `dateTime` parameter is the
            latest-arrival-time, so OTP will dynamically calculate the EDT. Using a short
            search-window is faster than using a longer one, but the search duration is not linear.
            Using a \"too\" short search-window will waste resources server side, while using a
            search-window that is too long will be slow.

            OTP will dynamically calculate a reasonable value for the search-window, if not
            provided. The calculation comes with a significant overhead (10-20%% extra). Whether
            you should use the dynamic calculated value or pass in a value depends on your use-case.
            For a travel planner in a small geographical area, with a dense network of public
            transportation, a fixed value between 40 minutes and 2 hours makes sense. To find the
            appropriate search-window, adjust it so that the number of itineraries on average is
            around the wanted `numTripPatterns`. Make sure you set the `numTripPatterns` to a high
            number while testing. For a country wide area like Norway, using the dynamic
            search-window is the best.

            When paginating, the search-window is calculated using the `numTripPatterns` in the
            original search together with statistics from the search for the last page. This
            behaviour is configured server side, and can not be overridden from the client. The
            paging may even exceed the maximum value.

            The search-window used is returned to the response metadata as `searchWindowUsed`.
            This can be used by the client to calculate the when the next page start/end.

            Note! In some cases you may have to page many times to get all the results you want.
            This is intended. Increasing the search-window beyond the max value is NOT going to be
            much faster. Instead the client can inform the user about the progress.

            Maximum value: %d minutes (%dh)
            """.formatted(
                transitTuningParameters.maxSearchWindow().toMinutes(),
                transitTuningParameters.maxSearchWindow().toHours()
              )
          )
          .type(Scalars.GraphQLInt)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("pageCursor")
          .description(
            """
            Use the cursor to go to the next \"page\" of itineraries. Copy the cursor from the last
            response and keep the original request as is. This will enable you to search for
            itineraries in the next or previous search-window. The paging will automatically scale
            up/down the search-window to fit the `numTripPatterns`.
            """
          )
          .type(Scalars.GraphQLString)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("timetableView")
          .description(
            "Search for the best trip options within a time window. If `true` two " +
            "TripPatterns are considered optimal if one is better on arrival time" +
            "(earliest wins) and the other is better on departure time (latest wins)." +
            "In combination with `arriveBy` this parameter cover the following 3 use " +
            "cases:\n\n" +
            "\n" +
            "  - Traveler want to find the best alternative within a time window. Set " +
            "    `timetableView=true` and `arriveBy=false`. This is the default, and if " +
            "    the intention of the traveler is unknown it gives the best result, " +
            "    because it includes the two next use-cases. This option also work well " +
            "    with paging. Setting the `arriveBy=true`, covers the same use-case, but " +
            "    the input time is interpreted as latest-arrival-time, and not " +
            "    earliest-departure-time.\n" +
            "\n" +
            "  - Traveler want to find the best alternative with departure after a " +
            "    specific time. For example: I am at the station now and want to get " +
            "    home as quickly as possible. Set `timetableView=false` and " +
            "    `arriveBy=false`. Do not support paging.\n" +
            "\n" +
            "  - Traveler want to find the best alternative with arrival before a" +
            "    specific time. For example going to a meeting. Set `timetableView=false` " +
            "    and `arriveBy=true`. Do not support paging.\n" +
            "\n" +
            "Default: `true`"
          )
          .type(Scalars.GraphQLBoolean)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("from")
          .description("The start location")
          .type(new GraphQLNonNull(LocationInputType.INPUT_TYPE))
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("to")
          .description("The destination location")
          .type(new GraphQLNonNull(LocationInputType.INPUT_TYPE))
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("passThroughPoints")
          .deprecate("Use via instead")
          .description("The list of points the journey is required to pass through.")
          .type(new GraphQLList(new GraphQLNonNull(PassThroughPointInputType.INPUT_TYPE)))
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name(TRIP_VIA_PARAMETER)
          .description(DOC_VIA)
          .type(new GraphQLList(new GraphQLNonNull(ViaLocationInputType.VIA_LOCATION_INPUT)))
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("arriveBy")
          .description(
            "Whether the trip should depart at dateTime (false, the default), or arrive at " +
            "dateTime. See `timetableView` for use-cases where this parameter is relevant."
          )
          .type(Scalars.GraphQLBoolean)
          .defaultValue(routing.request.arriveBy())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("wheelchairAccessible")
          .description(
            "Whether the trip must be wheelchair accessible. Supported for the street part to " +
            "the search, not implemented for the transit yet."
          )
          .type(Scalars.GraphQLBoolean)
          .defaultValue(routing.request.journey().wheelchair())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("ignoreRealtimeUpdates")
          .description("When true, real-time updates are ignored during this search.")
          .type(Scalars.GraphQLBoolean)
          .defaultValue(preferences.transit().ignoreRealtimeUpdates())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("includePlannedCancellations")
          .description(
            "When true, service journeys cancelled in scheduled route data will be included during this search."
          )
          .type(Scalars.GraphQLBoolean)
          .defaultValue(preferences.transit().includePlannedCancellations())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("includeRealtimeCancellations")
          .description(
            "When true, service journeys cancelled by real-time updates will be included during this search."
          )
          .type(Scalars.GraphQLBoolean)
          .defaultValue(preferences.transit().includeRealtimeCancellations())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("locale")
          .description(
            "The preferable language to use for text targeted the end user. Note! The data " +
            "quality is limited, only stop and quay names are translates, and not in all " +
            "places of the API."
          )
          .type(EnumTypes.LOCALE)
          .defaultValue("no")
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("modes")
          .description(
            "The set of access/egress/direct/transit modes to be used for this search. " +
            "Note that this only works at the Line level. If individual ServiceJourneys have " +
            "modes that differ from the Line mode, this will NOT be accounted for."
          )
          .type(ModeInputType.INPUT_TYPE)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("banned")
          .description("Banned")
          .description(
            "Parameters for indicating authorities, lines or quays not be used in the trip patterns"
          )
          .type(BannedInputType.INPUT_TYPE)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("whiteListed")
          .description(
            "Parameters for indicating the only authorities, lines or quays to be used in the trip patterns"
          )
          .type(JourneyWhiteListed.INPUT_TYPE)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("filters")
          .description(
            "A list of filters for which trips should be included. " +
            "A trip will be included if it matches with at least one filter. " +
            "An empty list of filters means that all trips should be included. " +
            "If a search include this parameter, \"whiteListed\", \"banned\" & \"modes.transportModes\" filters will be ignored."
          )
          .type(new GraphQLList(new GraphQLNonNull(FilterInputType.INPUT_TYPE)))
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("relaxTransitGroupPriority")
          .description(
            """
            Relax generalized-cost when comparing trips with a different set of
            transit-group-priorities. The groups are set server side for service-journey and
            can not be configured in the API. This mainly helps to return competition neutral
            services. Long distance authorities are put in different transit-groups.

            This relaxes the comparison inside the routing engine for each stop-arrival. If two
            paths have a different set of transit-group-priorities, then the generalized-cost
            comparison is relaxed. The final set of paths are filtered through the normal
            itinerary-filters.

            - The `ratio` must be greater or equal to 1.0 and less then 1.2.
            - The `constant` must be greater or equal to '0s' and less then '1h'.

            THIS IS STILL AN EXPERIMENTAL FEATURE - IT MAY CHANGE WITHOUT ANY NOTICE!
            """.stripIndent()
          )
          .type(RelaxCostType.INPUT_TYPE)
          .defaultValueLiteral(
            preferences.transit().relaxTransitGroupPriority().isNormal()
              ? NullValue.of()
              : RelaxCostType.valueOf(preferences.transit().relaxTransitGroupPriority())
          )
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("walkSpeed")
          .description("The maximum walk speed along streets, in meters per second.")
          .type(Scalars.GraphQLFloat)
          .defaultValue(preferences.walk().speed())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("walkReluctance")
          .description(
            "Walk cost is multiplied by this value. This is the main parameter to use for limiting walking."
          )
          .type(Scalars.GraphQLFloat)
          .defaultValue(preferences.walk().reluctance())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("waitReluctance")
          .description(
            "Wait cost is multiplied by this value. Setting this to a value lower than 1 " +
            "indicates that waiting is better than staying on a vehicle. This should never " +
            "be set higher than walkReluctance, since that would lead to walking down a line " +
            "to avoid waiting."
          )
          .type(Scalars.GraphQLFloat)
          .defaultValue(preferences.transfer().waitReluctance())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("bikeSpeed")
          .description("The maximum bike speed along streets, in meters per second")
          .type(Scalars.GraphQLFloat)
          .defaultValue(preferences.bike().speed())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("bicycleOptimisationMethod")
          .description(
            "The set of characteristics that the user wants to optimise for during bicycle " +
            "searches -- defaults to " +
            enumValAsString(
              EnumTypes.BICYCLE_OPTIMISATION_METHOD,
              preferences.bike().optimizeType()
            )
          )
          .type(EnumTypes.BICYCLE_OPTIMISATION_METHOD)
          .defaultValue(preferences.bike().optimizeType())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("triangleFactors")
          .description(
            "When setting the " +
            EnumTypes.BICYCLE_OPTIMISATION_METHOD.getName() +
            " to '" +
            enumValAsString(
              EnumTypes.BICYCLE_OPTIMISATION_METHOD,
              VehicleRoutingOptimizeType.TRIANGLE
            ) +
            "', use these values to tell the routing engine how important each of the factors is compared to the others. All values should add up to 1."
          )
          .type(TriangleFactorsInputType.INPUT_TYPE)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("useBikeRentalAvailabilityInformation")
          .description(
            "Whether or not bike rental availability information will be used to plan bike " +
            "rental trips."
          )
          .type(Scalars.GraphQLBoolean)
          .defaultValue(preferences.bike().rental().useAvailabilityInformation())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("transferPenalty")
          .description(
            "An extra penalty added on transfers (i.e. all boardings except the first one). " +
            "The transferPenalty is used when a user requests even less transfers. In the " +
            "latter case, we don't actually optimise for fewest transfers, as this can lead " +
            "to absurd results. Consider a trip in New York from Grand Army Plaza (the one " +
            "in Brooklyn) to Kalustyan's at noon. The true lowest transfers trip pattern is " +
            "to wait until midnight, when the 4 train runs local the whole way. The actual " +
            "fastest trip pattern is the 2/3 to the 4/5 at Nevins to the 6 at Union Square, " +
            "which takes half an hour. Even someone optimise for fewest transfers doesn't " +
            "want to wait until midnight. Maybe they would be willing to walk to 7th Ave " +
            "and take the Q to Union Square, then transfer to the 6. If this takes less than " +
            "transferPenalty seconds, then that's what we'll return."
          )
          .type(Scalars.GraphQLInt)
          .defaultValue(preferences.transfer().cost())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("transferSlack")
          .description(
            "An expected transfer time (in seconds) that specifies the amount of time that " +
            "must pass between exiting one public transport vehicle and boarding another. " +
            "This time is in addition to time it might take to walk between stops."
          )
          .type(Scalars.GraphQLInt)
          .defaultValue(preferences.transfer().slack().toSeconds())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("boardSlackDefault")
          .description(TransportModeSlack.boardSlackDescription("boardSlackList"))
          .type(Scalars.GraphQLInt)
          .defaultValue(preferences.transit().boardSlack().defaultValueSeconds())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("boardSlackList")
          .description(
            TransportModeSlack.slackByGroupDescription(
              "boardSlack",
              preferences.transit().boardSlack()
            )
          )
          .type(TransportModeSlack.SLACK_LIST_INPUT_TYPE)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("alightSlackDefault")
          .description(TransportModeSlack.alightSlackDescription("alightSlackList"))
          .type(Scalars.GraphQLInt)
          .defaultValue(preferences.transit().alightSlack().defaultValueSeconds())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("alightSlackList")
          .description(
            TransportModeSlack.slackByGroupDescription(
              "alightSlack",
              preferences.transit().alightSlack()
            )
          )
          .type(TransportModeSlack.SLACK_LIST_INPUT_TYPE)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("numTripPatterns")
          .description(
            "The maximum number of trip patterns to return. Note! This reduce the number of " +
            "trip patterns AFTER the OTP travel search is done in a post-filtering process. " +
            "There is little/no performance gain in reducing the number of trip patterns " +
            "returned. See also the trip meta-data on how to implement paging."
          )
          .defaultValue(routing.request.numItineraries())
          .type(Scalars.GraphQLInt)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("maximumTransfers")
          .description(
            "Maximum number of transfers. Note! The best way to reduce the number of " +
            "transfers is to set the `transferPenalty` parameter."
          )
          .type(Scalars.GraphQLInt)
          .defaultValue(preferences.transfer().maxTransfers())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("maximumAdditionalTransfers")
          .description(
            "Maximum number of additional transfers compared to the best number of transfers " +
            "allowed. Note! The best way to reduce the number of transfers is to set the " +
            "`transferPenalty` parameter."
          )
          .type(Scalars.GraphQLInt)
          .defaultValue(preferences.transfer().maxAdditionalTransfers())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("debugItineraryFilter")
          .description(
            "Debug the itinerary-filter-chain. OTP will attach a system notice to itineraries " +
            "instead of removing them. This is very convenient when tuning the filters."
          )
          .type(Scalars.GraphQLBoolean)
          .defaultValue(preferences.itineraryFilter().debug().debugEnabled())
          .deprecate("Use `itineraryFilter.debug` instead.")
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("itineraryFilters")
          .description(
            "Configure the itinerary-filter-chain. NOTE! THESE PARAMETERS ARE USED " +
            "FOR SERVER-SIDE TUNING AND IS AVAILABLE HERE FOR TESTING ONLY."
          )
          .type(ItineraryFiltersInputType.create(preferences.itineraryFilter()))
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name(ACCESS_EGRESS_PENALTY)
          .description("Time and cost penalty on access/egress modes.")
          .type(new GraphQLList(new GraphQLNonNull(penaltyForStreetMode)))
          .defaultValueLiteral(
            PenaltyForStreetModeType.mapToGraphQLValue(
              preferences.street().accessEgress().penalty()
            )
          )
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name(MAX_ACCESS_EGRESS_DURATION_FOR_MODE)
          .description(
            "Maximum duration for access/egress for street searches per respective mode. " +
            "Cannot be higher than default value. This is a performance optimisation parameter, avoid using it to limit the search. "
          )
          .type(new GraphQLList(new GraphQLNonNull(durationPerStreetModeType)))
          .defaultValueLiteral(
            mapDurationForStreetModeGraphQLValue(preferences.street().accessEgress().maxDuration())
          )
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name(MAX_DIRECT_DURATION_FOR_MODE)
          .description(
            "Maximum duration for direct street searchers per respective mode. " +
            "Cannot be higher than default value. This is a performance optimisation parameter, avoid using it to limit the search."
          )
          .type(new GraphQLList(new GraphQLNonNull(durationPerStreetModeType)))
          .defaultValueLiteral(
            mapDurationForStreetModeGraphQLValue(preferences.street().maxDirectDuration())
          )
          .build()
      )
      .dataFetcher(graphQLPlanner::plan)
      .build();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private static String enumValAsString(GraphQLEnumType enumType, Enum<?> otpVal) {
    return enumType
      .getValues()
      .stream()
      .filter(e -> e.getValue().equals(otpVal))
      .findFirst()
      .get()
      .getName();
  }
}
