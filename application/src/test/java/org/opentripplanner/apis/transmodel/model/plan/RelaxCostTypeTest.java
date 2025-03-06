package org.opentripplanner.apis.transmodel.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.transmodel.model.plan.RelaxCostType.CONSTANT;
import static org.opentripplanner.apis.transmodel.model.plan.RelaxCostType.RATIO;

import graphql.language.FloatValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

class RelaxCostTypeTest {

  @Test
  void valueOf() {
    assertEquals(
      ObjectValue.newObjectValue()
        .objectField(ObjectField.newObjectField().name(RATIO).value(FloatValue.of(1.0)).build())
        .objectField(
          ObjectField.newObjectField().name(CONSTANT).value(StringValue.of("0s")).build()
        )
        .build()
        .toString(),
      RelaxCostType.valueOf(CostLinearFunction.NORMAL).toString()
    );
    assertEquals(
      ObjectValue.newObjectValue()
        .objectField(ObjectField.newObjectField().name(RATIO).value(FloatValue.of(1.3)).build())
        .objectField(
          ObjectField.newObjectField().name(CONSTANT).value(StringValue.of("1m7s")).build()
        )
        .build()
        .toString(),
      RelaxCostType.valueOf(CostLinearFunction.of(Cost.costOfSeconds(67), 1.3)).toString()
    );
  }

  @Test
  void mapToDomain() {
    Map<String, Object> input;

    input = Map.of(RATIO, 1.0, CONSTANT, Cost.ZERO);
    assertEquals(
      CostLinearFunction.NORMAL,
      RelaxCostType.mapToDomain(input, CostLinearFunction.ZERO)
    );

    input = Map.of(RATIO, 0.0, CONSTANT, Cost.ZERO);
    assertEquals(
      CostLinearFunction.ZERO,
      RelaxCostType.mapToDomain(input, CostLinearFunction.ZERO)
    );

    input = Map.of(RATIO, 1.7, CONSTANT, Cost.costOfSeconds(3600 + 3 * 60 + 7));
    assertEquals(
      CostLinearFunction.of("1h3m7s + 1.7t"),
      RelaxCostType.mapToDomain(input, CostLinearFunction.ZERO)
    );
    assertEquals(
      CostLinearFunction.NORMAL,
      RelaxCostType.mapToDomain(null, CostLinearFunction.NORMAL)
    );
    assertEquals(CostLinearFunction.ZERO, RelaxCostType.mapToDomain(null, CostLinearFunction.ZERO));
  }
}
