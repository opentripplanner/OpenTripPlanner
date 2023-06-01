package org.opentripplanner.ext.transmodelapi.model.framework;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.STREET_MODE;

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
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;

public class StreetModeDurationInputType {

  // Unfortunately public to allow unit-testing
  public static final String FIELD_STREET_MODE = "streetMode";
  public static final String FIELD_DURATION = "duration";

  public static GraphQLInputObjectType create(GqlUtil gqlUtil) {
    return GraphQLInputObjectType
      .newInputObject()
      .name("StreetModeDurationInput")
      .description("A combination of street mode and corresponding duration")
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name(FIELD_STREET_MODE)
          .type(new GraphQLNonNull(STREET_MODE))
          .build()
      )
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name(FIELD_DURATION)
          .type(new GraphQLNonNull(gqlUtil.durationScalar))
      )
      .build();
  }

  public static Value mapDurationForStreetModeGraphQLValue(
    DurationForEnum<StreetMode> durationForStreetMode
  ) {
    List<Value> list = STREET_MODE
      .getValues()
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
    return ObjectValue
      .newObjectValue()
      .objectField(
        ObjectField
          .newObjectField()
          .name(FIELD_STREET_MODE)
          .value(EnumValue.of(gqlModeType.getName()))
          .build()
      )
      .objectField(
        ObjectField
          .newObjectField()
          .name(FIELD_DURATION)
          .value(StringValue.newStringValue(duration.toString()).build())
          .build()
      )
      .build();
  }

  public static void mapDurationForStreetMode(
    DurationForEnum.Builder<StreetMode> builder,
    DataFetchingEnvironment environment,
    String fieldName,
    DurationForEnum<StreetMode> defaultValue
  ) {
    mapDurationForStreetMode(builder, environment, fieldName, defaultValue, false);
  }

  public static void mapDurationForStreetModeAndAssertValueIsGreaterThenDefault(
    DurationForEnum.Builder<StreetMode> builder,
    DataFetchingEnvironment environment,
    String fieldName,
    DurationForEnum<StreetMode> defaultValue
  ) {
    mapDurationForStreetMode(builder, environment, fieldName, defaultValue, true);
  }

  private static void mapDurationForStreetMode(
    DurationForEnum.Builder<StreetMode> builder,
    DataFetchingEnvironment environment,
    String fieldName,
    DurationForEnum<StreetMode> defaultValues,
    boolean assertValueIsGreaterThenDefault
  ) {
    if (GqlUtil.hasArgument(environment, fieldName)) {
      List<Map<String, ?>> modes = environment.getArgument(fieldName);
      mapDurationForStreetMode(builder, defaultValues, modes, assertValueIsGreaterThenDefault);
    }
  }

  static void mapDurationForStreetMode(
    DurationForEnum.Builder<StreetMode> builder,
    DurationForEnum<StreetMode> defaultValues,
    List<Map<String, ?>> modes,
    boolean assertValueIsGreaterThenDefault
  ) {
    for (var entry : modes) {
      StreetMode streetMode = (StreetMode) entry.get(FIELD_STREET_MODE);
      var value = (Duration) entry.get(FIELD_DURATION);
      var defaultValue = defaultValues.valueOf(streetMode);

      if (assertValueIsGreaterThenDefault) {
        assertDurationIsGreaterThanDefault(streetMode, value, defaultValue);
      }
      builder.with(streetMode, value);
    }
  }

  private static <E extends Enum<E>> void assertDurationIsGreaterThanDefault(
    E key,
    Duration value,
    Duration defaultValue
  ) {
    if (value.toSeconds() > defaultValue.toSeconds()) {
      throw new IllegalArgumentException(
        "Invalid duration for mode %s. The value %s is greater than the default %s.".formatted(
            key,
            DurationUtils.durationToStr(value),
            DurationUtils.durationToStr(defaultValue)
          )
      );
    }
  }
}
