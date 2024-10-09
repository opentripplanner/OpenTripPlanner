package org.opentripplanner.ext.restapi.model;

public record ApiCurrency(
  String currency,
  int defaultFractionDigits,
  String currencyCode,
  String symbol
) {}
