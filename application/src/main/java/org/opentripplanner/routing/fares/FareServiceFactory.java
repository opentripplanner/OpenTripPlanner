package org.opentripplanner.routing.fares;

import org.opentripplanner.ext.fares.model.FareRulesData;
import tools.jackson.databind.JsonNode;

public interface FareServiceFactory {
  FareService makeFareService();

  void processGtfs(FareRulesData fareRuleService);

  void configure(JsonNode config);
}
