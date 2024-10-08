package org.opentripplanner.ext.fares.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class OrcaFareFactory extends DefaultFareServiceFactory {

  protected Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();

  @Override
  public FareService makeFareService() {
    return new OrcaFareService(regularFareRules.values());
  }

  /**
   * This step ensures that the fares in the source GTFS data are accounted for correctly.
   */
  @Override
  public void processGtfs(FareRulesData fareRuleService, OtpTransitService transitService) {
    fillFareRules(fareRuleService.fareAttributes(), fareRuleService.fareRules(), regularFareRules);
    // ORCA agencies don't rely on fare attributes without rules, so let's remove them.
    regularFareRules.entrySet().removeIf(entry -> !entry.getValue().hasRules());
  }

  /**
   * There is no configuration code in DefaultFareServiceFactory. We override the super class's method just in case it changes.
   */
  @Override
  public void configure(JsonNode config) {
    // No configuration at the moment.
  }
}
