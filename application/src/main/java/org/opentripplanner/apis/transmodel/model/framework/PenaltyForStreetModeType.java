package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.language.ArrayValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.model.scalars.DoubleFunction;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenaltyForEnum;
import org.opentripplanner.utils.lang.ObjectUtils;

/**
 * Access and egress penalty:
 * <pre>
 * accessEgressPenalty: [
 *   {
 *     streetMode: car
 *     timePenalty : "10m + 2.0 t"
 *     # costFactor is optional
 *   }
 *   {
 *     streetMode: flexible
 *     timePenalty : "15m + 1.5 t"
 *     costFactor: 2.5
 *   }
 * ]
 * </pre>
 */
public class PenaltyForStreetModeType {

  private static final String FIELD_STREET_MODE = "streetMode";
  private static final String FIELD_TIME_PENALTY = "timePenalty";
  private static final String FIELD_COST_FACTOR = "costFactor";

  public static GraphQLInputObjectType create() {
    return GraphQLInputObjectType.newInputObject()
      .name("PenaltyForStreetMode")
      .description("A combination of street mode and penalty for time and cost.")
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name(FIELD_STREET_MODE)
          .type(new GraphQLNonNull(EnumTypes.STREET_MODE))
          .description(
            """
            List of modes with the given penalty is applied to. A street-mode should not be listed
            in more than one element. If empty or null the penalty is applied to all unlisted modes,
            it becomes the default penalty for this query.
            """
          )
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name(FIELD_TIME_PENALTY)
          .type(new GraphQLNonNull(TransmodelScalars.DOUBLE_FUNCTION_SCALAR))
          .description(
            """
            Penalty applied to the time for the given list of modes.
            """
          )
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name(FIELD_COST_FACTOR)
          .type(Scalars.GraphQLFloat)
          .defaultValueProgrammatic(1.0)
          .description(
            """
                This is used to take the time-penalty and multiply by the `{fieldCostFactorName}`.
                The result is added to the generalized-cost.
            """.replace("{fieldCostFactorName}", FIELD_COST_FACTOR)
          )
      )
      .build();
  }

  /** Return a list of access-egress penalties */
  public static Value mapToGraphQLValue(TimeAndCostPenaltyForEnum<StreetMode> accessEgressPenalty) {
    List<Value> values = EnumTypes.STREET_MODE.getValues()
      .stream()
      .map(gqlModeType -> {
        var mode = (StreetMode) gqlModeType.getValue();
        return accessEgressPenalty.isSet(mode)
          ? mapPenaltyForStreetMode(gqlModeType, accessEgressPenalty.valueOf(mode))
          : null;
      })
      .filter(Objects::nonNull)
      .toList();
    return ArrayValue.newArrayValue().values(values).build();
  }

  public static void mapPenaltyToDomain(
    TimeAndCostPenaltyForEnum.Builder<StreetMode> builder,
    DataFetchingEnvironment environment,
    String fieldName
  ) {
    if (!GqlUtil.hasArgument(environment, fieldName)) {
      return;
    }

    for (var entry : environment.<List<Map<String, ?>>>getArgument(fieldName)) {
      var streetMode = (StreetMode) entry.get(FIELD_STREET_MODE);
      var timePenalty = ((DoubleFunction) entry.get(FIELD_TIME_PENALTY)).asTimePenalty();
      double costFactor = ObjectUtils.ifNotNull((Double) entry.get(FIELD_COST_FACTOR), 0.0);
      var value = TimeAndCostPenalty.of(timePenalty, costFactor);
      builder.with(streetMode, value);
    }
  }

  private static Value mapPenaltyForStreetMode(
    GraphQLEnumValueDefinition streetModeGQL,
    TimeAndCostPenalty timeAndCostPenalty
  ) {
    return ObjectValue.newObjectValue()
      .objectField(
        ObjectField.newObjectField()
          .name(FIELD_STREET_MODE)
          .value(EnumValue.of(streetModeGQL.getName()))
          .build()
      )
      .objectField(
        ObjectField.newObjectField()
          .name(FIELD_TIME_PENALTY)
          .value(StringValue.newStringValue(timeAndCostPenalty.timePenalty().serialize()).build())
          .build()
      )
      .objectField(
        ObjectField.newObjectField()
          .name(FIELD_COST_FACTOR)
          .value(FloatValue.of(timeAndCostPenalty.costFactor()))
          .build()
      )
      .build();
  }
}
