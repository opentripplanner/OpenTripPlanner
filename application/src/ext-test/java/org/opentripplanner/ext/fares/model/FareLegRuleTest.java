package org.opentripplanner.ext.fares.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.basic.Money;

class FareLegRuleTest {

  private static final FeedScopedId ID = new FeedScopedId("fares", "rule");
  private static final FeedScopedId LEG_GROUP_ID = new FeedScopedId("fares", "group");
  private static final FareProduct PRODUCT = FareProduct.of(
    new FeedScopedId("fares", "daypass"),
    "day pass",
    Money.euros(10)
  ).build();

  @Test
  void testEqualsAndHashCode() {
    var subject = rule(1, timeframe(8));
    var same = rule(1, timeframe(8));

    assertEquals(subject, same);
    assertEquals(subject.hashCode(), same.hashCode());

    assertNotEquals(subject, rule(2, timeframe(8)));
    assertNotEquals(subject, rule(1, timeframe(9)));
    assertNotEquals(
      subject,
      FareLegRule.of(ID, PRODUCT)
        .withLegGroupId(LEG_GROUP_ID)
        .withPriority(1)
        .withFromTimeframes(List.of(timeframe(8)))
        .withToTimeframes(List.of(timeframe(8)))
        .build()
    );
  }

  private static FareLegRule rule(int priority, Timeframe fromTimeframe) {
    return FareLegRule.of(ID, PRODUCT)
      .withLegGroupId(LEG_GROUP_ID)
      .withPriority(priority)
      .withFromTimeframes(List.of(fromTimeframe))
      .build();
  }

  private static Timeframe timeframe(int startHour) {
    return Timeframe.of()
      .withServiceId(new FeedScopedId("fares", "service"))
      .withStart(LocalTime.of(startHour, 0))
      .withEnd(LocalTime.of(18, 0))
      .build();
  }
}
