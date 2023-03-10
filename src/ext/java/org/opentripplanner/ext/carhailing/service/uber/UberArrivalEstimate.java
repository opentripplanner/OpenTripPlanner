package org.opentripplanner.ext.carhailing.service.uber;

public record UberArrivalEstimate(
  String display_name,
  int estimate,
  String localized_display_name,
  String product_id
) {}
