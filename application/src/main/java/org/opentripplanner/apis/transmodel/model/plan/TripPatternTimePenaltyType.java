package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.utils.time.DurationUtils;

public class TripPatternTimePenaltyType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name("TimePenaltyWithCost")
      .description(
        """
        The time-penalty is applied to either the access-legs and/or egress-legs. Both access and
        egress may contain more than one leg; Hence, the penalty is not a field on leg.

        Note! This is for debugging only. This type can change without notice.
        """
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("appliedTo")
          .description(
            """
            The time-penalty is applied to either the access-legs and/or egress-legs. Both access
            and egress may contain more than one leg; Hence, the penalty is not a field on leg. The
            `appliedTo` describe which part of the itinerary that this instance applies to.
            """
          )
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> penalty(environment).appliesTo())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("timePenalty")
          .description(
            """
            The time-penalty added to the actual time/duration when comparing the itinerary with
            other itineraries. This is used to decide which is the best option, but is not visible
            - the actual departure and arrival-times are not modified.
            """
          )
          .type(Scalars.GraphQLString)
          .dataFetcher(environment ->
            DurationUtils.durationToStr(penalty(environment).penalty().time())
          )
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("generalizedCostDelta")
          .description(
            """
            The time-penalty does also propagate to the `generalizedCost`. As a result of the given
            time-penalty, the generalized-cost also increased by the given amount. This delta is
            included in the itinerary generalized-cost. In some cases the generalized-cost-delta is
            excluded when comparing itineraries - that happens if one of the itineraries is a
            "direct/street-only" itinerary. Time-penalty can not be set for direct searches, so it
            needs to be excluded from such comparison to be fair. The unit is transit-seconds.
            """
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(environment -> penalty(environment).penalty().cost().toSeconds())
          .build()
      )
      .build();
  }

  static TripPlanTimePenaltyDto penalty(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
