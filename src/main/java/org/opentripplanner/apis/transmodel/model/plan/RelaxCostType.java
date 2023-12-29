package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

public class RelaxCostType {

  public static final String RATIO = "ratio";
  public static final String CONSTANT = "constant";

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType
    .newInputObject()
    .name("RelaxCostInput")
    .description(
      """
      A relax-cost is used to increase the limit when comparing one cost to another cost.
      This is used to include more results into the result. A `ratio=2.0` means a path(itinerary)
      with twice as high cost as another one, is accepted. A `constant=$300` means a "fixed"
      constant is added to the limit. A `{ratio=1.0, constant=0}` is said to be the NORMAL relaxed
      cost - the limit is the same as the cost used to calculate the limit. The NORMAL is usually
      the default. We can express the RelaxCost as a function `f(x) = constant + ratio * x`.
      `f(x)=x` is the NORMAL function.
      """
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name(RATIO)
        .description("The factor to multiply with the 'other cost'. Minimum value is 1.0.")
        .defaultValueLiteral(FloatValue.of(1.0))
        .type(Scalars.GraphQLFloat)
        .build()
    )
    .field(
      GraphQLInputObjectField
        .newInputObjectField()
        .name(CONSTANT)
        .description(
          "The constant value to add to the limit. Must be a positive number. The unit" +
          " is cost-seconds."
        )
        .defaultValueLiteral(IntValue.of(0))
        .type(new GraphQLList(new GraphQLNonNull(Scalars.GraphQLID)))
        .build()
    )
    .build();

  public static ObjectValue valueOf(CostLinearFunction value) {
    return ObjectValue
      .newObjectValue()
      .objectField(
        ObjectField.newObjectField().name(RATIO).value(FloatValue.of(value.coefficient())).build()
      )
      .objectField(
        ObjectField
          .newObjectField()
          .name(CONSTANT)
          .value(IntValue.of(value.constant().toSeconds()))
          .build()
      )
      .build();
  }
}
