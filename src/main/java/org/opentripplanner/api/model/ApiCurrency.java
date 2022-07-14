package org.opentripplanner.api.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Currency;

public record ApiCurrency(@JsonSerialize Currency currency) {
  @JsonSerialize
  public int getDefaultFractionDigits() {
    return currency.getDefaultFractionDigits();
  }

  @JsonSerialize
  public String getCurrencyCode() {
    return currency.getCurrencyCode();
  }
  @JsonSerialize
  public String getSymbol() {
    return currency.getSymbol();
  }
}
