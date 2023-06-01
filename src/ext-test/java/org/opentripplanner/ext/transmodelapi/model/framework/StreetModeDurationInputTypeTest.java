package org.opentripplanner.ext.transmodelapi.model.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;

class StreetModeDurationInputTypeTest {

  public static final DurationForEnum<StreetMode> DEFAULT_DURATION_FOR_STREET_MODE = DurationForEnum
    .of(StreetMode.class)
    .withDefault(Duration.ofMinutes(10))
    .with(StreetMode.WALK, Duration.ofMinutes(20))
    .build();

  @Test
  void testMapDurationForStreetMode() {
    var builder = DEFAULT_DURATION_FOR_STREET_MODE.copyOf();
    List<Map<String, ?>> input = List.of(
      Map.of(
        StreetModeDurationInputType.FIELD_STREET_MODE,
        StreetMode.WALK,
        StreetModeDurationInputType.FIELD_DURATION,
        Duration.ofMinutes(15)
      ),
      Map.of(
        StreetModeDurationInputType.FIELD_STREET_MODE,
        StreetMode.CAR,
        StreetModeDurationInputType.FIELD_DURATION,
        Duration.ofMinutes(5)
      )
    );

    StreetModeDurationInputType.mapDurationForStreetMode(
      builder,
      DEFAULT_DURATION_FOR_STREET_MODE,
      input,
      true
    );

    assertEquals(
      "DurationForStreetMode{default:10m, WALK:15m, CAR:5m}",
      builder.build().toString()
    );
  }

  @Test
  void testMapDurationForStreetModeWithValuesGreaterThenDefault() {
    var builder = DEFAULT_DURATION_FOR_STREET_MODE.copyOf();
    List<Map<String, ?>> input = List.of(
      Map.of(
        StreetModeDurationInputType.FIELD_STREET_MODE,
        StreetMode.WALK,
        StreetModeDurationInputType.FIELD_DURATION,
        // Add one second to default duration to exceed the limit.
        DEFAULT_DURATION_FOR_STREET_MODE.valueOf(StreetMode.WALK).plusSeconds(1)
      )
    );

    // OK - validation turned off
    StreetModeDurationInputType.mapDurationForStreetMode(
      builder,
      DEFAULT_DURATION_FOR_STREET_MODE,
      input,
      false
    );

    // throw exception when validation is on
    assertThrows(
      IllegalArgumentException.class,
      () ->
        StreetModeDurationInputType.mapDurationForStreetMode(
          builder,
          DEFAULT_DURATION_FOR_STREET_MODE,
          input,
          true
        )
    );
  }
}
