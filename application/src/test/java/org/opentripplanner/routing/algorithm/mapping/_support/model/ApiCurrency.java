package org.opentripplanner.routing.algorithm.mapping._support.model;

@Deprecated
public record ApiCurrency(
  String currency,
  int defaultFractionDigits,
  String currencyCode,
  String symbol
) {}
