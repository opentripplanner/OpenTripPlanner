package org.opentripplanner.apis.transmodel.model.framework;

import graphql.language.ArrayValue;
import graphql.language.EnumValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.utils.time.DurationUtils;

public class StreetModeDurationInputType {

  private static final String FIELD_STREET_MODE = "streetMode";
  private static final String FIELD_DURATION = "duration";

  public static GraphQLInputObjectType create() {
    return GraphQLInputObjectType.newInputObject()
      .name("StreetModeDurationInput")
      .description("A combination of street mode and corresponding duration")
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name(FIELD_STREET_MODE)
          .type(new GraphQLNonNull(EnumTypes.STREET_MODE))
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name(FIELD_DURATION)
          .type(new GraphQLNonNull(TransmodelScalars.DURATION_SCALAR))
      )
      .build();
  }

  public static Value mapDurationForStreetModeGraphQLValue(
    DurationForEnum<StreetMode> durationForStreetMode
  ) {
    List<Value> list = EnumTypes.STREET_MODE.getValues()
      .stream()
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
    return ObjectValue.newObjectValue()
      .objectField(
        ObjectField.newObjectField()
          .name(FIELD_STREET_MODE)
          .value(EnumValue.of(gqlModeType.getName()))
          .build()
      )
      .objectField(
        ObjectField.newObjectField()
          .name(FIELD_DURATION)
          .value(StringValue.newStringValue(duration.toString()).build())
          .build()
      )
      .build();
  }

  public static void mapDurationForStreetModeAndAssertValueIsGreaterThenDefault(
    DurationForEnum.Builder<StreetMode> builder,
    DataFetchingEnvironment environment,
    String fieldName,
    DurationForEnum<StreetMode> defaultValue
  ) {
    if (GqlUtil.hasArgument(environment, fieldName)) {
      for (var entry : environment.<List<Map<String, ?>>>getArgument(fieldName)) {
        StreetMode streetMode = (StreetMode) entry.get(FIELD_STREET_MODE);
        var value = (Duration) entry.get(FIELD_DURATION);
        var defaultValue1 = defaultValue.valueOf(streetMode);

        assertDurationIsGreaterThanDefault(streetMode, value, defaultValue1);
        builder.with(streetMode, value);
      }
    }
  }

  private static <E extends Enum<E>> void assertDurationIsGreaterThanDefault(
    E key,
    Duration value,
    Duration defaultValue
  ) {
    if (defaultValue.minus(value).isNegative()) {
      throw new IllegalArgumentException(
        "Invalid duration for mode %s. The value %s is not greater than the default %s.".formatted(
            key,
            DurationUtils.durationToStr(value),
            DurationUtils.durationToStr(defaultValue)
          )
      );
    }
  }
}
