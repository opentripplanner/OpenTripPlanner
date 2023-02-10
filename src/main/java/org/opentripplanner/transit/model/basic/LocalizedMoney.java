package org.opentripplanner.transit.model.basic;

import java.util.Locale;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * TODO RTM - This has nothing to do with the transit model, move somewhere else
 *          - No entity in the model have a reference to this.
 */
public record LocalizedMoney(Money money) implements I18NString {
  @Override
  public String toString(Locale locale) {
    if (locale == null) {
      return money.localize(Locale.ROOT);
    }
    return money.localize(locale);
  }
}
