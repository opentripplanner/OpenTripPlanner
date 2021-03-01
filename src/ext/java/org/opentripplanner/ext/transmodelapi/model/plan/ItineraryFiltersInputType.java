package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.routing.api.request.ItineraryFilterParameters;
import org.opentripplanner.routing.api.request.RequestFunctions;

import java.util.function.Consumer;
import java.util.function.DoubleFunction;

public class ItineraryFiltersInputType {

  private static final String MIN_SAFE_TRANSFER_TIME_FACTOR = "minSafeTransferTimeFactor";
  private static final String TRANSIT_GENERALIZED_COST_LIMIT = "transitGeneralizedCostLimit";
  private static final String GROUP_SIMILARITY_KEEP_ONE = "groupSimilarityKeepOne";
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
            .description("Add an additional cost for short transfers on long transit itineraries. "
                + "See javaDoc on `AddMinSafeTransferCostFilter` details.")
            .defaultValue(dft.minSafeTransferTimeFactor)
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
            .name(GROUP_SIMILARITY_KEEP_N_ITINERARIES)
            .description(
                "Reduce the number of itineraries to the requested number by reducing each "
                    + "group of itineraries grouped by 68% similarity.")
            .defaultValue(dft.groupSimilarityKeepNumOfItineraries)
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
    setField(callWith, GROUP_SIMILARITY_KEEP_N_ITINERARIES, (Double v) -> target.groupSimilarityKeepNumOfItineraries = v);
    setField(callWith, MIN_SAFE_TRANSFER_TIME_FACTOR, (Double v) -> target.minSafeTransferTimeFactor = v);
    setField(callWith, TRANSIT_GENERALIZED_COST_LIMIT, (DoubleFunction<Double> v) -> target.transitGeneralizedCostLimit = v);
  }

  private static <T> void setField(DataFetcherDecorator callWith, String field, Consumer<T> consumer) {
    callWith.argument("itineraryFilters." + field, consumer);
  }
}