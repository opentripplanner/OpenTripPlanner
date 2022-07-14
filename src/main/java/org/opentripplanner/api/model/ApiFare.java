package org.opentripplanner.api.model;

import java.util.List;
import java.util.Map;

public record ApiFare(
  Map<ApiFareType, ApiMoney> fare,
  Map<ApiFareType, List<ApiFareComponent>> details
) {}
