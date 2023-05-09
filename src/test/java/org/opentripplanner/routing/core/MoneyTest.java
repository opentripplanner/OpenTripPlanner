package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.transit.model.basic.Locales.NORWEGIAN_BOKMAL;
import static org.opentripplanner.transit.model.basic.Locales.NORWEGIAN_NYNORSK;

import java.util.Currency;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model.basic.Money;

class MoneyTest {

  private static final Money hundredNOK = new Money(Currency.getInstance("NOK"), 10000);
  private static final Money oneDollar = Money.usDollars(1);
  private static final Money twoDollars = Money.usDollars(2);
  static Money threeEuroTwelve = Money.euros(3.12f);

  static Stream<Arguments> testCases = Stream.of(
    of(oneDollar, Locale.US, "$1.00"),
    of(oneDollar, Locale.GERMANY, "1,00 $"),
    of(Money.euros(1), Locale.GERMANY, "1,00 €"),
    of(oneDollar, NORWEGIAN_BOKMAL, "USD 1,00"),
    of(oneDollar, NORWEGIAN_NYNORSK, "1.00 USD"),
    of(hundredNOK, NORWEGIAN_BOKMAL, "kr 100,00"),
    of(hundredNOK, NORWEGIAN_NYNORSK, "100.00 kr")
  );

  @ParameterizedTest(name = "{0} with locale {1} should localise to \"{2}\"")
  @VariableSource("testCases")
  void localize(Money money, Locale locale, String expected) {
    var localized = money.localize(locale);
    assertEquals(expected, localized);
  }

  static Stream<Arguments> amountCases = Stream.of(
    of(oneDollar, 1.0d),
    of(threeEuroTwelve, 3.12d),
    of(Money.euros(999.99f), 999.99d),
    of(hundredNOK, 100.0d),
    // Yen doesn't have fractional digits
    of(yen(1000), 1000d),
    of(yen(9999), 9999d)
  );

  @ParameterizedTest
  @VariableSource("amountCases")
  void fractionalAmount(Money money, double expected) {
    var localized = money.fractionalAmount();
    assertEquals(expected, localized);
  }

  private static Money yen(int amount) {
    return new Money(Currency.getInstance("JPY"), amount);
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
  void greaterThan() {
    assertTrue(twoDollars.greaterThan(oneDollar));
    assertFalse(oneDollar.greaterThan(oneDollar));
    assertFalse(oneDollar.greaterThan(twoDollars));
  }
}
