package org.opentripplanner.ext.fares.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.RiderCategory;
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
    Arguments.of(fareProduct(null, null, null), ZDT, "1ef42792-8f41-349f-8bfa-8864930fc50d"),
    Arguments.of(
      fareProduct(null, null, null),
      ZDT.plusHours(1),
      "8d8177ca-bbcd-3c1e-9651-cf6fd749fac3"
    ),
    Arguments.of(
      fareProduct(Duration.ofHours(2), CATEGORY, null),
      ZDT,
      "ec757263-3fee-32dc-856d-5035856b656d"
    ),
    Arguments.of(
      fareProduct(Duration.ofHours(2), CATEGORY, MEDIUM),
      ZDT,
      "86b3d1ca-1145-3c36-aa1f-8f2a685234b5"
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
    return new FareProduct(
      new FeedScopedId("fares", "daypass"),
      "day pass",
      Money.euros(10),
      duration,
      cat,
      medium
    );
  }
}
