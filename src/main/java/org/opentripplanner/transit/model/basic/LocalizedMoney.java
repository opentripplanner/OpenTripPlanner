package org.opentripplanner.transit.model.basic;

import java.util.Locale;

public record LocalizedMoney(Money money) implements I18NString {
  @Override
  public String toString(Locale locale) {
    return money.localize(locale);
  }
}
