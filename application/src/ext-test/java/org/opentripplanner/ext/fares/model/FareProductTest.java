package org.opentripplanner.ext.fares.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FareProductTest {

  static ZonedDateTime ZDT = OffsetDateTime.parse("2023-03-27T10:44:54+02:00").toZonedDateTime();

  static RiderCategory CATEGORY = new RiderCategory(
    new FeedScopedId("1", "pensioners"),
    "Pensioners",
    null
  );

  static FareMedium MEDIUM = new FareMedium(new FeedScopedId("1", "app"), "App");

  static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.of(fareProduct(null, null), ZDT, "b18a083d-ee82-3c83-af07-2b8bb11bff9e"),
      Arguments.of(
        fareProduct(null, null),
        ZDT.plusHours(1),
        "2a60adcf-3e56-338a-ab7d-8407a3bc529b"
      ),
      Arguments.of(fareProduct(CATEGORY, null), ZDT, "59e781a8-ee72-3454-a7c3-960feadf85dd"),
      Arguments.of(fareProduct(CATEGORY, MEDIUM), ZDT, "17de57df-da5c-3cf3-8fee-c158559ea560")
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void instanceId(FareProduct fareProduct, ZonedDateTime startTime, String expectedInstanceId) {
    var instanceId = fareProduct.uniqueInstanceId(startTime);

    assertEquals(expectedInstanceId, instanceId);
  }

  private static FareProduct fareProduct(RiderCategory cat, FareMedium medium) {
    return FareProduct.of(new FeedScopedId("fares", "daypass"), "day pass", Money.euros(10))
      .withCategory(cat)
      .withMedium(medium)
      .build();
  }
}
