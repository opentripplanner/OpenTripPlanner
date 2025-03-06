package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;
import java.util.Map;
import java.util.function.Consumer;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
import org.opentripplanner.apis.transmodel.model.scalars.DoubleFunction;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;

public class ItineraryFiltersInputType {

  private static final String DEBUG = "debug";
  private static final String MIN_SAFE_TRANSFER_TIME_FACTOR = "minSafeTransferTimeFactor";
  private static final String TRANSIT_GENERALIZED_COST_LIMIT = "transitGeneralizedCostLimit";
  private static final String GROUP_SIMILARITY_KEEP_ONE = "groupSimilarityKeepOne";
  private static final String GROUP_SIMILARITY_KEEP_THREE = "groupSimilarityKeepThree";
  private static final String GROUPED_OTHER_THAN_SAME_LEGS_MAX_COST_MULTIPLIER =
    "groupedOtherThanSameLegsMaxCostMultiplier";
  private static final String GROUP_SIMILARITY_KEEP_N_ITINERARIES =
    "groupSimilarityKeepNumOfItineraries";

  public static GraphQLInputObjectType create(ItineraryFilterPreferences dft) {
    return GraphQLInputObjectType.newInputObject()
      .name("ItineraryFilters")
      .description(
        "Parameters for the OTP Itinerary Filter Chain. These parameters SHOULD be " +
        "configured on the server side and should not be used by the client. They " +
        "are made available here to be able to experiment and tune the server."
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name(MIN_SAFE_TRANSFER_TIME_FACTOR)
          .type(Scalars.GraphQLFloat)
          .deprecate("This filter is removed, it has undesired side-effects")
          .description(
            "Add an additional cost for short transfers on long transit itineraries. " +
            "See javaDoc on `AddMinSafeTransferCostFilter` details."
          )
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name(TRANSIT_GENERALIZED_COST_LIMIT)
          .type(
            GraphQLInputObjectType.newInputObject()
              .name("TransitGeneralizedCostFilterParams")
              .field(
                GraphQLInputObjectField.newInputObjectField()
                  .name("costLimitFunction")
                  .type(new GraphQLNonNull(TransmodelScalars.DOUBLE_FUNCTION_SCALAR))
                  .build()
              )
              .field(
                GraphQLInputObjectField.newInputObjectField()
                  .name("intervalRelaxFactor")
                  .type(new GraphQLNonNull(Scalars.GraphQLFloat))
                  .build()
              )
              .build()
          )
          // There is a bug in the GraphQL lib. The default value is shown as a `boolean`
          // with value `false`, not the actual value. Hence; The default is added to the
          // description instead.
          .description(
            "Set a relative limit for all transit itineraries. The limit is calculated based on " +
            "the transit itinerary generalized-cost and the time between itineraries " +
            "Itineraries without transit legs are excluded from this filter. Example: " +
            "costLimitFunction(x) = 3600 + 2.0 x and intervalRelaxFactor = 0.5. " +
            "If the lowest cost returned is 10 000, then the limit " +
            "is set to: 3 600 + 2 * 10 000 = 26 600 plus half of the time between either departure" +
            " or arrival times of the itinerary. " +
            "Default: {\"costLimitFunction\": " +
            dft.transitGeneralizedCostLimit().costLimitFunction().serialize() +
            ", \"intervalRelaxFactor\": " +
            dft.transitGeneralizedCostLimit().intervalRelaxFactor() +
            "}"
          )
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name(GROUP_SIMILARITY_KEEP_ONE)
          .type(Scalars.GraphQLFloat)
          .description(
            "Pick ONE itinerary from each group after putting itineraries that is 85% " +
            "similar together."
          )
          .defaultValue(dft.groupSimilarityKeepOne())
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .type(Scalars.GraphQLFloat)
          .name(GROUP_SIMILARITY_KEEP_THREE)
          .description(
            "Reduce the number of itineraries in each group to to maximum 3 itineraries. " +
            "The itineraries are grouped by similar legs (on board same journey). So, if " +
            " 68% of the distance is traveled by similar legs, then two itineraries are " +
            "in the same group. Default value is 68%, must be at least 50%."
          )
          .defaultValue(dft.groupSimilarityKeepThree())
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .type(Scalars.GraphQLFloat)
          .name(GROUP_SIMILARITY_KEEP_N_ITINERARIES)
          .deprecate("Use " + GROUP_SIMILARITY_KEEP_THREE + " instead.")
          .defaultValue(dft.groupSimilarityKeepThree())
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .type(Scalars.GraphQLFloat)
          .name(GROUPED_OTHER_THAN_SAME_LEGS_MAX_COST_MULTIPLIER)
          .description(
            "Of the itineraries grouped to maximum of three itineraries, how much worse can the " +
            "non-grouped legs be compared to the lowest cost. 2.0 means that they can be " +
            "double the cost, and any itineraries having a higher cost will be filtered. " +
            "Default value is 2.0, use a value lower than 1.0 to turn off"
          )
          .defaultValue(dft.groupedOtherThanSameLegsMaxCostMultiplier())
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .type(EnumTypes.ITINERARY_FILTER_DEBUG_PROFILE)
          .name(DEBUG)
          .description(
            """
            Use this parameter to debug the itinerary-filter-chain. The default is `off`
            (itineraries are filtered and not returned). For all other values the unwanted
            itineraries are returned with a system notice, and not deleted."""
          )
          .defaultValue(dft.debug())
          .build()
      )
      .build();
  }

  public static void mapToRequest(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith,
    ItineraryFilterPreferences.Builder builder
  ) {
    if (!GqlUtil.hasArgument(environment, "itineraryFilters")) {
      return;
    }
    setField(callWith, GROUP_SIMILARITY_KEEP_ONE, builder::withGroupSimilarityKeepOne);

    // This is deprecated, sets same value as GROUP_SIMILARITY_KEEP_THREE
    setField(callWith, GROUP_SIMILARITY_KEEP_N_ITINERARIES, builder::withGroupSimilarityKeepThree);

    setField(callWith, GROUP_SIMILARITY_KEEP_THREE, builder::withGroupSimilarityKeepThree);
    setField(
      callWith,
      GROUPED_OTHER_THAN_SAME_LEGS_MAX_COST_MULTIPLIER,
      builder::withGroupedOtherThanSameLegsMaxCostMultiplier
    );
    setField(callWith, TRANSIT_GENERALIZED_COST_LIMIT, (Map<String, ?> v) ->
      builder.withTransitGeneralizedCostLimit(
        new TransitGeneralizedCostFilterParams(
          ((DoubleFunction) v.get("costLimitFunction")).asCostLinearFunction(),
          (double) v.get("intervalRelaxFactor")
        )
      )
    );
    setField(callWith, DEBUG, builder::withDebug);
  }

  private static <T> void setField(
    DataFetcherDecorator callWith,
    String field,
    Consumer<T> consumer
  ) {
    callWith.argument("itineraryFilters." + field, consumer);
  }
}
