package org.opentripplanner.ext.fares.service.gtfs.v2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import com.google.common.collect.ImmutableMultimap;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.model.plan.TestTransitLeg;
import org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory;

class TimeframeMatcherTest implements FareTestConstants {

  public static final FareLegRule RULE = FareLegRule.of(id("1"), FARE_PRODUCT_A)
    .withLegGroupId(id("1"))
    .withFromTimeframes(List.of(TIMEFRAME_TWELVE_TO_TWO))
    .withToTimeframes(List.of(TIMEFRAME_THREE_TO_FIVE))
    .build();
  public static final FeedScopedId ID2 = FeedScopedIdForTestFactory.id("2");

  private static List<Arguments> outsideTimeframeCases() {
    return List.of(
      Arguments.of("00:00", "11:59"),
      Arguments.of("10:00", "11:00"),
      Arguments.of("10:00", "11:59"),
      Arguments.of("10:00", "12:01"),
      Arguments.of("14:01", "14:05"),
      Arguments.of("11:59", "12:01"),
      Arguments.of("11:00", "12:00"),
      Arguments.of("18:00", "19:05"),
      Arguments.of("13:00", "14:01"),
      Arguments.of("14:00", "17:01")
    );
  }

  @ParameterizedTest
  @MethodSource("outsideTimeframeCases")
  void outsideTimeframes(String startTime, String endTime) {
    var leg = TestTransitLeg.of().withStartTime(startTime).withEndTime(endTime).build();
    var matcher = new TimeframeMatcher(
      ImmutableMultimap.of(TIMEFRAME_TWELVE_TO_TWO.serviceId(), leg.serviceDate())
    );
    assertFalse(matcher.matchesTimeframes(leg, RULE));
  }

  private static List<Arguments> withinTimeframeCases() {
    return List.of(
      Arguments.of("12:01", "15:01"),
      Arguments.of("14:00", "15:01"),
      Arguments.of("12:00", "15:00"),
      Arguments.of("14:00", "17:00")
    );
  }

  @ParameterizedTest
  @MethodSource("withinTimeframeCases")
  void withinTimeframes(String startTime, String endTime) {
    var serviceId = TIMEFRAME_TWELVE_TO_TWO.serviceId();
    var leg = TestTransitLeg.of().withStartTime(startTime).withEndTime(endTime).build();
    var matcher = new TimeframeMatcher(ImmutableMultimap.of(serviceId, leg.serviceDate()));
    assertTrue(matcher.matchesTimeframes(leg, RULE));
  }

  @ParameterizedTest
  @MethodSource("withinTimeframeCases")
  void differentServiceId(String startTime, String endTime) {
    var leg = TestTransitLeg.of()
      .withStartTime(startTime)
      .withEndTime(endTime)
      .withServiceId(ID2)
      .build();
    var matcher = new TimeframeMatcher(
      ImmutableMultimap.of(
        TIMEFRAME_TWELVE_TO_TWO.serviceId(),
        leg.serviceDate().plusDays(1),
        TIMEFRAME_THREE_TO_FIVE.serviceId(),
        leg.serviceDate().plusDays(1),
        ID2,
        leg.serviceDate()
      )
    );
    assertFalse(matcher.matchesTimeframes(leg, RULE));
  }
}
