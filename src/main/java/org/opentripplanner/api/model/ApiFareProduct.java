package org.opentripplanner.api.model;

public record ApiFareProduct(
  String id,
  String name,
  ApiFareQualifier container,
  ApiFareQualifier category
) {}
