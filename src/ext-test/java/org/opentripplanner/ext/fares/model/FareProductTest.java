package org.opentripplanner.ext.fares.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.test.support.VariableSource;
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

  static Stream<Arguments> testCases = Stream.of(
    Arguments.of(fareProduct(null, null, null), ZDT, "72c415fb-ef6d-3b12-a8ce-36eb7895c43f"),
    Arguments.of(
      fareProduct(null, null, null),
      ZDT.plusHours(1),
      "32c72720-b983-3148-bcec-d4d099ed202a"
    ),
    Arguments.of(
      fareProduct(Duration.ofHours(2), CATEGORY, null),
      ZDT,
      "9c5f4abf-ffce-3f89-bf93-e7f900bea286"
    ),
    Arguments.of(
      fareProduct(Duration.ofHours(2), CATEGORY, MEDIUM),
      ZDT,
      "0cc60b70-b4a7-3082-a89d-5c5d73c7b2c7"
    )
  );

  @ParameterizedTest
  @VariableSource("testCases")
  void instanceId(FareProduct fareProduct, ZonedDateTime startTime, String expectedInstanceId) {
    var instanceId = fareProduct.uniqueInstanceId(startTime);

    assertEquals(expectedInstanceId, instanceId);
  }

  @Nonnull
  private static FareProduct fareProduct(Duration duration, RiderCategory cat, FareMedium medium) {
    var fp = new FareProduct(
      new FeedScopedId("fares", "daypass"),
      "day pass",
      Money.euros(1_000),
      duration,
      cat,
      medium
    );
    return fp;
  }
}
