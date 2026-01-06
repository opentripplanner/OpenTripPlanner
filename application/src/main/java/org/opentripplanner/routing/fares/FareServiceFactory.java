package org.opentripplanner.routing.fares;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.fares.model.FareRulesData;

public interface FareServiceFactory {
  FareService makeFareService();

  void processGtfs(FareRulesData fareRuleService);

  void configure(JsonNode config);
}
