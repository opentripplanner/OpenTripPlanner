package org.opentripplanner.apis.gtfs.support.time;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.core.model.time.TimePeriod;

class OffsetDateTimeRangeUtilTest {

  private static final OffsetDateTime START = OffsetDateTime.parse("2026-06-01T10:00:00+02:00");
  private static final OffsetDateTime END = OffsetDateTime.parse("2026-06-01T12:00:00+02:00");
  private static final String FIELD = "runningTimeRanges";

  @Test
  void nullRangesAreNotMapped() {
    assertNull(OffsetDateTimeRangeUtil.mapRanges(null, FIELD));
  }

  @Test
  void mapRanges() {
    var ranges = List.of(
      new GraphQLTypes.GraphQLOffsetDateTimeRangeInput(Map.of("start", START, "end", END)),
      new GraphQLTypes.GraphQLOffsetDateTimeRangeInput(Map.of("start", START)),
      new GraphQLTypes.GraphQLOffsetDateTimeRangeInput(Map.of("end", END)),
      new GraphQLTypes.GraphQLOffsetDateTimeRangeInput(Map.of())
    );

    assertThat(OffsetDateTimeRangeUtil.mapRanges(ranges, FIELD))
      .containsExactly(
        TimePeriod.of(START.toInstant(), END.toInstant()),
        TimePeriod.of(START.toInstant(), null),
        TimePeriod.of(null, END.toInstant()),
        TimePeriod.ofUnbounded()
      )
      .inOrder();
  }

  @Test
  void emptyRangesAreForbidden() {
    var exception = assertThrows(InvalidInputException.class, () ->
      OffsetDateTimeRangeUtil.mapRanges(List.of(), FIELD)
    );
    assertThat(exception.getMessage()).isEqualTo(
      "Time range filter 'runningTimeRanges' must be either null or have at least one entry."
    );
  }

  @Test
  void startAfterEndIsForbidden() {
    var exception = assertThrows(InvalidInputException.class, () ->
      OffsetDateTimeRangeUtil.mapRange(END, START, FIELD)
    );
    assertThat(exception.getMessage()).isEqualTo(
      "The start of the time range 'runningTimeRanges' must not be after its end."
    );
  }
}
