package org.opentripplanner.ext.transmodelapi.model.framework;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.STREET_MODE;

import graphql.language.ArrayValue;
import graphql.language.EnumValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;

public class StreetModeDurationInputType {

  public static GraphQLInputObjectType create(GqlUtil gqlUtil) {
    return GraphQLInputObjectType
      .newInputObject()
      .name("StreetModeDurationInput")
      .description("A combination of street mode and corresponding duration")
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name("streetMode")
          .type(new GraphQLNonNull(STREET_MODE))
          .build()
      )
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name("duration")
          .type(new GraphQLNonNull(gqlUtil.durationScalar))
      )
      .build();
  }


  public static Value mapDurationForStreetModeGraphQLValue(
    DurationForEnum<StreetMode> durationForStreetMode
  ) {
    List<Value> list = STREET_MODE.getValues().stream()
      .map(gqlModeType -> {
        var mode = (StreetMode) gqlModeType.getValue();
        return durationForStreetMode.isSet(mode)
          ? mapModeDuration(gqlModeType, durationForStreetMode.valueOf(mode))
          : null;
      })
      .filter(Objects::nonNull)
      .toList();
    return ArrayValue.newArrayValue().values(list).build();
  }

  static Value mapModeDuration(GraphQLEnumValueDefinition gqlModeType, Duration duration) {
    return ObjectValue
      .newObjectValue()
      .objectField(
        ObjectField
          .newObjectField()
          .name("streetMode")
          .value(EnumValue.of(gqlModeType.getName()))
          .build()
      )
      .objectField(
        ObjectField
          .newObjectField()
          .name("duration")
          .value(StringValue.newStringValue(duration.toString()).build())
          .build()
      )
      .build();
  }
}
