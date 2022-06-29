package org.opentripplanner.model;

public record FareLegRule(
  String feedId,
  String groupId,
  String networkId,
  FareProduct fareProduct
) {}
