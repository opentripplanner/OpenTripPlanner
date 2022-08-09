package org.opentripplanner.api.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record ApiItineraryFares(
  Map<String, ApiMoney> fare,
  Map<String, List<ApiFareComponent>> details,
  List<ApiFareProduct> coveringItinerary,
  List<ApiLegProducts> legProducts
) {}
