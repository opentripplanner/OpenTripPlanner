package org.opentripplanner.api.model;

public record ApiCurrency(
  String currency,
  int defaultFractionDigits,
  String currencyCode,
  String symbol
) {}
