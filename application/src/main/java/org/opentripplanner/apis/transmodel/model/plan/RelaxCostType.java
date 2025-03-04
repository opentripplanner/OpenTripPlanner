package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.language.FloatValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import java.util.Map;
import org.opentripplanner.framework.graphql.scalar.CostScalarFactory;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.utils.time.DurationUtils;

public class RelaxCostType {

  public static final String RATIO = "ratio";
  public static final String CONSTANT = "constant";

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("RelaxCostInput")
    .description(
      """
      A relax-cost is used to increase the limit when comparing one cost to another cost.
      This is used to include more results into the result. A `ratio=2.0` means a path(itinerary)
      with twice as high cost as another one, is accepted. A `constant=$300` means a "fixed"
      constant is added to the limit. A `{ratio=1.0, constant=0}` is said to be the NORMAL relaxed
      cost - the limit is the same as the cost used to calculate the limit. The NORMAL is usually
      the default. We can express the RelaxCost as a function `f(t) = constant + ratio * t`.
      `f(t)=t` is the NORMAL function.
      """
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name(RATIO)
        .description("The factor to multiply with the 'other cost'. Minimum value is 1.0.")
        .defaultValueLiteral(FloatValue.of(1.0))
        .type(Scalars.GraphQLFloat)
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name(CONSTANT)
        .description(
          "The constant value to add to the limit. Must be a positive number. The value is " +
          "equivalent to transit-cost-seconds. Integers are treated as seconds, but you may use " +
          "the duration format. Example: '3665 = 'DT1h1m5s' = '1h1m5s'."
        )
        .defaultValueProgrammatic("0s")
        .type(CostScalarFactory.costScalar())
        .build()
    )
    .build();

  public static ObjectValue valueOf(CostLinearFunction value) {
    return ObjectValue.newObjectValue()
      .objectField(
        ObjectField.newObjectField().name(RATIO).value(FloatValue.of(value.coefficient())).build()
      )
      .objectField(
        ObjectField.newObjectField()
          .name(CONSTANT)
          // We only use this to display the default value (this is an input type), so using
          // the lenient OTP version of duration is ok - it is slightly more readable.
          .value(StringValue.of(DurationUtils.durationToStr(value.constant().asDuration())))
          .build()
      )
      .build();
  }

  public static CostLinearFunction mapToDomain(
    Map<String, Object> input,
    CostLinearFunction defaultValue
  ) {
    if (input == null || input.isEmpty()) {
      return defaultValue;
    }

    double ratio = 1.0;
    Cost constant = Cost.ZERO;

    if (input.containsKey(RATIO)) {
      ratio = (Double) input.get(RATIO);
    }
    if (input.containsKey(CONSTANT)) {
      constant = (Cost) input.get(CONSTANT);
    }
    return CostLinearFunction.of(constant, ratio);
  }
}
