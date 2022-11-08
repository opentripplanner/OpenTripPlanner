package org.opentripplanner.transit.model.basic;

import java.util.Locale;
import org.opentripplanner.routing.core.Money;

public record LocalizedMoney(Money money) implements I18NString {
  @Override
  public String toString(Locale locale) {
    return money.localize(locale);
  }
}
