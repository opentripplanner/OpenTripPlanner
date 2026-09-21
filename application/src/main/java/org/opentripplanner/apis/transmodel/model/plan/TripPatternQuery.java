package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLRequestContext;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReferenceSerializer;
import org.opentripplanner.routing.refetch.ItineraryReferenceMapper;
import org.opentripplanner.routing.refetch.RefetchItineraryException;
import org.opentripplanner.routing.refetch.RefetchItineraryService;

/**
 * Refetch a previously planned trip pattern using its stable, opaque id (see
 * {@link TripPatternType}'s {@code id} field, #7878).
 * <p>
 * Reuses the shared {@link org.opentripplanner.model.plan.itineraryreference.ItineraryReference}/
 * {@link ItineraryReferenceSerializer}/{@link ItineraryReferenceMapper} core - the same one used
 * by the GTFS API - rather than a Transmodel-specific token or reconstruction logic. Intended to
 * replace {@link RefetchTripPatternQuery}, which remains available but deprecated.
 */
public class TripPatternQuery {

  public GraphQLFieldDefinition create(GraphQLOutputType tripPatternType) {
    return GraphQLFieldDefinition.newFieldDefinition()
      .name("tripPattern")
      .description(
        "Refetch a previously returned trip pattern using its stable id (see " +
          "`TripPattern.id`), with current realtime data applied. Returns null if the id is " +
          "invalid, malformed, or refers to a trip pattern shape that cannot be refetched (for " +
          "example, a street-only trip pattern)."
      )
      .withDirective(TransmodelDirectives.TIMING_DATA)
      .type(tripPatternType)
      .argument(
        GraphQLArgument.newArgument()
          .name("id")
          .description("The stable id of the trip pattern to refetch.")
          .type(new GraphQLNonNull(Scalars.GraphQLID))
          .build()
      )
      .dataFetcher(this::refetchItinerary)
      .build();
  }

  Object refetchItinerary(DataFetchingEnvironment environment) {
    TransmodelGraphQLRequestContext ctx = environment.getContext();
    String id = environment.getArgument("id");
    var reference = ItineraryReferenceSerializer.decode(id);
    if (reference == null) {
      return null;
    }

    var refetchItineraryService = new RefetchItineraryService(
      ctx.graph(),
      ctx.transitService(),
      ctx.transitAlertService(),
      ctx.transferService(),
      ctx.streetDetailsService(),
      ctx.linkingContextFactory(),
      ctx.streetLimitationParametersService()
    );

    try {
      return ItineraryReferenceMapper.refetch(
        reference,
        ctx.defaultRouteRequest(),
        refetchItineraryService
      );
    } catch (RefetchItineraryException | IllegalArgumentException e) {
      return null;
    }
  }
}
