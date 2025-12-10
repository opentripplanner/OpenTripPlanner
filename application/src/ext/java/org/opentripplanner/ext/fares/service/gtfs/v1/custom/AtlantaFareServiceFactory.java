package org.opentripplanner.ext.fares.service.gtfs.v1.custom;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareServiceFactory;
import org.opentripplanner.routing.fares.FareService;

public class AtlantaFareServiceFactory extends DefaultFareServiceFactory {

  protected Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();

  @Override
  public FareService makeFareService() {
    return new AtlantaFareService(regularFareRules.values());
  }

  /**
   * This step ensures that the fares in the source GTFS data are accounted for correctly.
   */
  @Override
  public void processGtfs(FareRulesData fareRuleService) {
    fillFareRules(fareRuleService.fareAttributes(), fareRuleService.fareRules(), regularFareRules);
  }

  /**
   * There is no configuration code in DefaultFareServiceFactory. We override the super class's method just in case it changes.
   */
  @Override
  public void configure(JsonNode config) {
    // No configuration at the moment.
  }
}
