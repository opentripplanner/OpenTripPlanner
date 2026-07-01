package org.opentripplanner.apis.transmodel.model.plan;

import static org.opentripplanner.apis.transmodel.model.framework.StreetModeDurationInputType.mapDurationForStreetModeGraphQLValue;

import graphql.Scalars;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.apis.transmodel.mapping.GenericLocationMapper;
import org.opentripplanner.apis.transmodel.mapping.TripRequestMapper;
import org.opentripplanner.apis.transmodel.model.DefaultRouteRequestType;
import org.opentripplanner.apis.transmodel.model.ModeInputType;
import org.opentripplanner.apis.transmodel.model.TransportModeSlack;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.legreference.LegReferenceSerializer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.refetch.RefetchItineraryException;
import org.opentripplanner.routing.refetch.RefetchItineraryService;

public class RefetchTripPatternQuery {

  private final FeedScopedIdMapper idMapper;
  private final GenericLocationMapper genericLocationMapper;

  public RefetchTripPatternQuery(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
    this.genericLocationMapper = new GenericLocationMapper(idMapper);
  }

  public GraphQLFieldDefinition create(
    DefaultRouteRequestType routing,
    GraphQLOutputType tripPatternType,
    GraphQLInputObjectType durationPerStreetModeType,
    GraphQLInputObjectType locationInputType
  ) {
    RoutingPreferences preferences = routing.request.preferences();

    var outputType = GraphQLObjectType.newObject()
      .name("RefetchTripPatternResult")
      .description("Refetched data for a trip pattern")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("tripPattern")
          .description("The refetched trip pattern")
          .type(tripPatternType)
          .dataFetcher(env -> ((RefetchResult) env.getSource()).itinerary())
          .build()
      )
      .build();

    return GraphQLFieldDefinition.newFieldDefinition()
      .name("refetchTripPattern")
      .description("Get an updated version of a trip pattern with realtime changes applied.")
      .withDirective(TransmodelDirectives.TIMING_DATA)
      .type(new GraphQLNonNull(outputType))
      .argument(
        GraphQLArgument.newArgument()
          .name("from")
          .description(
            "The start location, if null the trip pattern will start with the first provided leg"
          )
          .type(locationInputType)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("to")
          .description(
            "The destination location, if null the trip pattern will end with the last provided leg"
          )
          .type(locationInputType)
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("legs")
          .description(
            "A list of legIds describing the trip pattern. At least one leg is required."
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLID))))
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("wheelchairAccessible")
          .description(
            "Whether the trip must be wheelchair accessible. Supported for the street part of the search."
          )
          .type(Scalars.GraphQLBoolean)
          .defaultValue(routing.request.journey().wheelchair())
          .build()
      )
      .argument(
        GraphQLArgument.newArgument()
          .name("modes")
          .description(
            "The set of access/egress modes to be used for this search. The direct and transit modes are ignored"
          )
          .type(ModeInputType.INPUT_TYPE)
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
          .name("maxAccessEgressDurationForMode")
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
      .dataFetcher(this::refetchJourney)
      .deprecate("This query is experimental and might change in the future.")
      .build();
  }

  private DataFetcherResult<RefetchResult> refetchJourney(DataFetchingEnvironment environment) {
    TransmodelRequestContext ctx = environment.getContext();
    var routeRequest = createRouteRequest(environment);
    List<String> legsIds = Objects.requireNonNull(environment.getArgument("legs"));
    var legs = legsIds
      .stream()
      .map(legId ->
        Optional.ofNullable(LegReferenceSerializer.decode(legId)).orElseThrow(() ->
          new InvalidInputException("Invalid leg id")
        )
      )
      .toList();

    if (legs.isEmpty()) {
      throw new InvalidInputException("At least one leg is required");
    }

    var from = mapLocation(environment.getArgument("from"));
    var to = mapLocation(environment.getArgument("to"));

    var refetcher = new RefetchItineraryService(ctx.getServerContext());

    try {
      var itinerary = refetcher.refetchItinerary(from, to, legs, routeRequest);
      RefetchResult apiResponse = new RefetchResult(itinerary);
      return DataFetcherResult.<RefetchResult>newResult().data(apiResponse).build();
    } catch (RefetchItineraryException e) {
      throw new InvalidInputException(e.getMessage());
    }
  }

  @Nullable
  private GenericLocation mapLocation(@Nullable Map<String, Object> location) {
    return Optional.ofNullable(location)
      .flatMap(genericLocationMapper::toGenericLocation)
      .orElse(null);
  }

  private RouteRequest createRouteRequest(DataFetchingEnvironment environment) {
    var mapper = new TripRequestMapper(idMapper);
    return mapper
      .createRequestBuilder(environment)
      // The from and to parameters are ignored in the refetch service.
      .withFrom(GenericLocation.fromCoordinate(0.0, 0.0))
      .withTo(GenericLocation.fromCoordinate(1.0, 1.0))
      .buildRequest();
  }

  private record RefetchResult(Itinerary itinerary) {}
}
