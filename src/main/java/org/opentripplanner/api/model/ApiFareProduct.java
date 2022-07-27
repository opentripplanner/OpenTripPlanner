package org.opentripplanner.api.model;

public record ApiFareProduct(
  String id,
  String name,
  ApiMoney amount,
  ApiFareQualifier container,
  ApiFareQualifier category
) {}
