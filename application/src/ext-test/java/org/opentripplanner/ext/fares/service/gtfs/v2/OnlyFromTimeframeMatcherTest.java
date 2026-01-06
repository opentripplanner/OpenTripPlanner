package org.opentripplanner.ext.fares.service.gtfs.v2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import com.google.common.collect.ImmutableMultimap;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.ext.fares.model.Timeframe;
import org.opentripplanner.model.plan.TestTransitLeg;

class OnlyFromTimeframeMatcherTest implements FareTestConstants {

  public static final FeedScopedId S1 = id("s1");
  public static final Timeframe TF_TWELVE_TO_TWO = Timeframe.of()
    .withServiceId(S1)
    .withStart(LocalTime.of(12, 0))
    .withEnd(LocalTime.of(14, 0))
    .build();

  public static final FareLegRule RULE = legRule(TF_TWELVE_TO_TWO);

  private static List<Arguments> outsideTimeframeCases() {
    return List.of(
      Arguments.of("00:00", "11:59"),
      Arguments.of("10:00", "11:00"),
      Arguments.of("10:00", "11:59"),
      Arguments.of("10:00", "12:01"),
      Arguments.of("11:59", "12:01"),
      Arguments.of("11:00", "12:00"),
      Arguments.of("14:01", "14:05"),
      Arguments.of("18:00", "19:05")
    );
  }

  @ParameterizedTest
  @MethodSource("outsideTimeframeCases")
  void outsideTimeframe(String startTime, String endTime) {
    var leg = TestTransitLeg.of().withStartTime(startTime).withEndTime(endTime).build();
    var matcher = new TimeframeMatcher(ImmutableMultimap.of(S1, leg.serviceDate()));
    assertFalse(matcher.matchesTimeframes(leg, RULE));
  }

  private static List<Arguments> withinTimeframeCases() {
    return List.of(
      Arguments.of("12:00", "12:02"),
      Arguments.of("12:01", "12:02"),
      Arguments.of("12:01", "13:00"),
      Arguments.of("13:00", "14:00"),
      Arguments.of("13:00", "14:01")
    );
  }

  @ParameterizedTest
  @MethodSource("withinTimeframeCases")
  void withinTimeframe(String startTime, String endTime) {
    var leg = TestTransitLeg.of().withStartTime(startTime).withEndTime(endTime).build();
    var matcher = new TimeframeMatcher(ImmutableMultimap.of(S1, leg.serviceDate()));
    assertTrue(matcher.matchesTimeframes(leg, RULE));
  }

  private static FareLegRule legRule(Timeframe tf) {
    return FareLegRule.of(id("1"), FARE_PRODUCT_A)
      .withLegGroupId(id("1"))
      .withFromTimeframes(List.of(tf))
      .build();
  }
}
