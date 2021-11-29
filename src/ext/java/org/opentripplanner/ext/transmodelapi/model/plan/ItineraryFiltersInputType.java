package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.routing.api.request.ItineraryFilterParameters;
import org.opentripplanner.routing.api.request.RequestFunctions;

public class ItineraryFiltersInputType {

  private static final String MIN_SAFE_TRANSFER_TIME_FACTOR = "minSafeTransferTimeFactor";
  private static final String TRANSIT_GENERALIZED_COST_LIMIT = "transitGeneralizedCostLimit";
  private static final String GROUP_SIMILARITY_KEEP_ONE = "groupSimilarityKeepOne";
  private static final String GROUP_SIMILARITY_KEEP_THREE = "groupSimilarityKeepThree";
  private static final String GROUPED_OTHER_THAN_SAME_LEGS_MAX_COST_MULTIPLIER = "groupedOtherThanSameLegsMaxCostMultiplier";
  private static final String GROUP_SIMILARITY_KEEP_N_ITINERARIES = "groupSimilarityKeepNumOfItineraries";

  public static GraphQLInputObjectType create(GqlUtil gqlUtil, ItineraryFilterParameters dft) {
    return GraphQLInputObjectType
        .newInputObject()
        .name("ItineraryFilters")
        .description("Parameters for the OTP Itinerary Filter Chain. These parameters SHOULD be " + "configured on the server side and should not be used by the client. They "
            + "are made available here to be able to experiment and tune the server.")
        .field(GraphQLInputObjectField
            .newInputObjectField()
            .name(MIN_SAFE_TRANSFER_TIME_FACTOR)
            .type(Scalars.GraphQLFloat)
            .deprecate("This filter is removed, it has undesired side-effects")
            .description("Add an additional cost for short transfers on long transit itineraries. "
                + "See javaDoc on `AddMinSafeTransferCostFilter` details.")
            .build())
        .field(GraphQLInputObjectField.newInputObjectField().name(TRANSIT_GENERALIZED_COST_LIMIT).type(gqlUtil.doubleFunctionScalar)
            // There is a bug in the GraphQL lib. The default value is shown as a `boolean`
            // with value `false`, not the actual value. Hence; The default is added to the
            // description instead.
            .description("Set a relative limit for all transit itineraries. The limit is " + "calculated based on the best transit itinerary generalized-cost. "
                + "Itineraries without transit legs are excluded from this filter. Example: " + "f(x) = 3600 + 2.0 x. If the lowest cost returned is 10 000, then the limit "
                + "is set to: 3 600 + 2 * 10 000 = 26 600. Then all itineraries with at least " + "one transit leg and a cost above 26 600 is removed from the result. "
                + "Default: " + RequestFunctions.serialize(dft.transitGeneralizedCostLimit)).build())
        .field(GraphQLInputObjectField
            .newInputObjectField()
            .name(GROUP_SIMILARITY_KEEP_ONE)
            .type(Scalars.GraphQLFloat)
            .description("Pick ONE itinerary from each group after putting itineraries that is 85% "
                + "similar together.")
            .defaultValue(dft.groupSimilarityKeepOne)
            .build())
        .field(GraphQLInputObjectField
            .newInputObjectField()
            .type(Scalars.GraphQLFloat)
            .name(GROUP_SIMILARITY_KEEP_THREE)
            .description(
                "Reduce the number of itineraries in each group to to maximum 3 itineraries. "
                + "The itineraries are grouped by similar legs (on board same journey). So, if "
                + " 68% of the distance is traveled by similar legs, then two itineraries are "
                + "in the same group. Default value is 68%, must be at least 50%.")
            .defaultValue(dft.groupSimilarityKeepThree)
            .build())
        .field(GraphQLInputObjectField
                .newInputObjectField()
                .type(Scalars.GraphQLFloat)
                .name(GROUP_SIMILARITY_KEEP_N_ITINERARIES)
                .deprecate("Use " + GROUP_SIMILARITY_KEEP_THREE + " instead.")
                .defaultValue(dft.groupSimilarityKeepThree)
                .build())
        .field(GraphQLInputObjectField
            .newInputObjectField()
            .type(Scalars.GraphQLFloat)
            .name(GROUPED_OTHER_THAN_SAME_LEGS_MAX_COST_MULTIPLIER)
            .description(
                "Of the itineraries grouped to maximum of three itineraries, how much worse can the "
                + "non-grouped legs be compared to the lowest cost. 2.0 means that they can be "
                + "double the cost, and any itineraries having a higher cost will be filtered. "
                + "Default value is 2.0, use a value lower than 1.0 to turn off")
            .defaultValue(dft.groupedOtherThanSameLegsMaxCostMultiplier)
            .build())
        .build();
  }

  public static void mapToRequest(
      DataFetchingEnvironment environment,
      DataFetcherDecorator callWith,
      ItineraryFilterParameters target
  ) {
    if (!GqlUtil.hasArgument(environment, "itineraryFilters")) {
      return;
    }
    setField(callWith, GROUP_SIMILARITY_KEEP_ONE, (Double v) -> target.groupSimilarityKeepOne = v);

    // This is deprecated, sets same value as GROUP_SIMILARITY_KEEP_THREE
    setField(callWith, GROUP_SIMILARITY_KEEP_N_ITINERARIES, (Double v) -> target.groupSimilarityKeepThree = v);

    setField(callWith, GROUP_SIMILARITY_KEEP_THREE, (Double v) -> target.groupSimilarityKeepThree = v);
    setField(callWith, GROUPED_OTHER_THAN_SAME_LEGS_MAX_COST_MULTIPLIER, (Double v) -> target.groupedOtherThanSameLegsMaxCostMultiplier = v);
    setField(callWith, TRANSIT_GENERALIZED_COST_LIMIT, (DoubleFunction<Double> v) -> target.transitGeneralizedCostLimit = v);
  }

  private static <T> void setField(DataFetcherDecorator callWith, String field, Consumer<T> consumer) {
    callWith.argument("itineraryFilters." + field, consumer);
  }
}