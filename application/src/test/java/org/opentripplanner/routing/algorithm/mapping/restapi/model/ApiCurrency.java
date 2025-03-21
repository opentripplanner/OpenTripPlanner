package org.opentripplanner.routing.algorithm.mapping.restapi.model;

public record ApiCurrency(
  String currency,
  int defaultFractionDigits,
  String currencyCode,
  String symbol
) {}
