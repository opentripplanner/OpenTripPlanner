package org.opentripplanner.routing.fares;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.model.OtpTransitService;

public interface FareServiceFactory {
  FareService makeFareService();

  void processGtfs(FareRulesData fareRuleService, OtpTransitService transitService);

  void configure(JsonNode config);
}
