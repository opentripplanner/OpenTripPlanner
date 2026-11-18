package org.opentripplanner.ext.fares.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.basic.Money;

class FareLegRuleTest {

  private static final FeedScopedId ID = id("rule");
  private static final List<FareProduct> PRODUCTS = List.of(product("daypass"));
  private static final FareLegRule SUBJECT = builder(ID, PRODUCTS).build();

  @Test
  void testEqualsAndHashCode() {
    var same = builder(ID, PRODUCTS).build();

    assertEquals(SUBJECT, same);
    assertEquals(SUBJECT.hashCode(), same.hashCode());
  }

  private static Stream<Arguments> equalsAndHashCodeFieldCases() {
    return Stream.of(
      Arguments.of("id", (Supplier<FareLegRule>) () -> builder(id("other"), PRODUCTS).build()),
      Arguments.of(
        "fareProducts",
        (Supplier<FareLegRule>) () -> builder(ID, List.of(product("other"))).build()
      ),
      fieldCase("legGroupId", b -> b.withLegGroupId(id("otherGroup"))),
      fieldCase("networkId", b -> b.withNetworkId(id("otherNetwork"))),
      fieldCase("fromAreaId", b -> b.withFromAreaId(id("otherFromArea"))),
      fieldCase("toAreaId", b -> b.withToAreaId(id("otherToArea"))),
      fieldCase("fareDistance", b -> b.withFareDistance(new FareDistance.Stops(2, 6))),
      fieldCase("priority", b -> b.withPriority(2)),
      fieldCase("fromTimeframes", b -> b.withFromTimeframes(List.of(timeframe(9)))),
      fieldCase("toTimeframes", b -> b.withToTimeframes(List.of(timeframe(11))))
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("equalsAndHashCodeFieldCases")
  void fieldIsPartOfEqualsAndHashCode(String field, Supplier<FareLegRule> otherSupplier) {
    var other = otherSupplier.get();

    assertNotEquals(SUBJECT, other);
    assertNotEquals(SUBJECT.hashCode(), other.hashCode());
  }

  private static Arguments fieldCase(String field, Consumer<FareLegRuleBuilder> change) {
    Supplier<FareLegRule> other = () -> {
      var builder = builder(ID, PRODUCTS);
      change.accept(builder);
      return builder.build();
    };
    return Arguments.of(field, other);
  }

  private static FareLegRuleBuilder builder(FeedScopedId id, Collection<FareProduct> products) {
    return FareLegRule.of(id, products)
      .withLegGroupId(id("group"))
      .withNetworkId(id("network"))
      .withFromAreaId(id("fromArea"))
      .withToAreaId(id("toArea"))
      .withFareDistance(new FareDistance.Stops(1, 5))
      .withPriority(1)
      .withFromTimeframes(List.of(timeframe(8)))
      .withToTimeframes(List.of(timeframe(10)));
  }

  private static FeedScopedId id(String id) {
    return new FeedScopedId("fares", id);
  }

  private static FareProduct product(String id) {
    return FareProduct.of(id(id), id, Money.euros(10)).build();
  }

  private static Timeframe timeframe(int startHour) {
    return Timeframe.of()
      .withServiceId(id("service"))
      .withStart(LocalTime.of(startHour, 0))
      .withEnd(LocalTime.of(18, 0))
      .build();
  }
}
