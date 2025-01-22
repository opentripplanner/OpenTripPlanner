package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.transit.model.basic.Locales.NORWEGIAN_BOKMAL;

import java.io.Serializable;
import java.util.Currency;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.model.basic.Money;

class MoneyTest {

  private static final Money hundredNOK = Money.ofFractionalAmount(
    Currency.getInstance("NOK"),
    100
  );
  private static final Money oneDollar = Money.usDollars(1);
  private static final Money twoDollars = Money.usDollars(2);
  static Money threeEuroTwelve = Money.euros(3.12f);

  static Stream<Arguments> testCases() {
    return Stream.of(
      of(oneDollar, Locale.US, "$1.00"),
      of(oneDollar, Locale.GERMANY, "1,00 $"),
      of(Money.euros(1), Locale.GERMANY, "1,00 €"),
      of(oneDollar, NORWEGIAN_BOKMAL, "USD 1,00"),
      //of(oneDollar, NORWEGIAN_NYNORSK, "1.00 USD"),
      of(hundredNOK, NORWEGIAN_BOKMAL, "kr 100,00")
      //of(hundredNOK, NORWEGIAN_NYNORSK, "100.00 kr")
    );
  }

  @Disabled
  @ParameterizedTest(name = "{0} with locale {1} should localise to \"{2}\"")
  @MethodSource("testCases")
  void localize(Money money, Locale locale, String expected) {
    var localized = money.localize(locale);
    assertEquals(expected, localized);
  }

  static Stream<Arguments> amountCases() {
    return Stream.of(
      of(oneDollar, 1.0f),
      of(threeEuroTwelve, 3.12f),
      of(Money.euros(3.1f), 3.1f),
      of(Money.euros(999.99f), 999.99f),
      of(hundredNOK, 100.0f),
      // Yen doesn't have fractional digits
      of(yen(1000), 1000f),
      of(yen(9999), 9999f)
    );
  }

  @ParameterizedTest
  @MethodSource("amountCases")
  void fractionalAmount(Money money, float expected) {
    var fractionalAmount = money.fractionalAmount();
    assertEquals(expected, fractionalAmount.floatValue());
  }

  private static Money yen(int amount) {
    return Money.ofFractionalAmount(Currency.getInstance("JPY"), amount);
  }

  @Test
  void plus() {
    assertEquals(twoDollars, oneDollar.plus(oneDollar));
  }

  @Test
  void minus() {
    assertEquals(oneDollar, twoDollars.minus(oneDollar));
  }

  @Test
  void times() {
    assertEquals(Money.usDollars(4), oneDollar.times(4));
  }

  @Test
  void half() {
    assertEquals(Money.usDollars(0.50f), Money.usDollars(0.99f).half());
    assertEquals(Money.usDollars(0.38f), Money.usDollars(0.75f).half());
  }

  @Test
  void roundDownToNearestFiveMinorUnits() {
    assertEquals(Money.usDollars(0.1f), Money.usDollars(0.11f).roundDownToNearestFiveMinorUnits());
    assertEquals(Money.usDollars(0.5f), Money.usDollars(0.54f).roundDownToNearestFiveMinorUnits());
  }

  @Test
  void greaterThan() {
    assertTrue(twoDollars.greaterThan(oneDollar));
    assertFalse(oneDollar.greaterThan(oneDollar));
    assertFalse(oneDollar.greaterThan(twoDollars));
  }

  @Test
  void serializable() {
    assertInstanceOf(Serializable.class, oneDollar);
  }

  @Test
  void equalHashCode() {
    assertEquals(Money.usDollars(5).hashCode(), Money.usDollars(5).hashCode());
  }
}
