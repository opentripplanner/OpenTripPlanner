package org.opentripplanner.ext.fares.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.fares.FaresConfiguration;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MultipleFareServiceFactory implements FareServiceFactory {

  private static final Logger log = LoggerFactory.getLogger(MultipleFareServiceFactory.class);

  private List<FareServiceFactory> subFactories;

  @Override
  public FareService makeFareService() {
    List<FareService> subServices = new ArrayList<>();
    for (FareServiceFactory subFactory : subFactories) subServices.add(
      subFactory.makeFareService()
    );
    return makeMultipleFareService(subServices);
  }

  @Override
  public void processGtfs(FareRulesData fareRuleService, OtpTransitService transitService) {
    for (FareServiceFactory subFactory : subFactories) subFactory.processGtfs(
      fareRuleService,
      transitService
    );
  }

  /**
   * Accept several ways to define fares to compose. Examples:
   *
   * <pre>
   * { combinationStrategy : "additive",
   *   // An array of 'fares'
   *   fares : [ "seattle", { ... } ]
   * }
   * --------------------------
   * { combinationStrategy : "additive",
   *   // All properties starting with 'fare'
   *   fare1 : "seattle",
   *   fare2 : { type: "vehicle-rental-time-based",
   *             prices : { ... }
   * } }
   * </pre>
   */
  @Override
  public void configure(JsonNode config) {
    subFactories = new ArrayList<>();
    for (JsonNode pConfig : config.path("fares")) {
      subFactories.add(FaresConfiguration.fromConfig(pConfig));
    }
    for (Iterator<Map.Entry<String, JsonNode>> i = config.fields(); i.hasNext();) {
      Map.Entry<String, JsonNode> kv = i.next();
      String key = kv.getKey();
      if (key.startsWith("fare") && !key.equals("fares")) {
        JsonNode node = kv.getValue();
        FareServiceFactory fareFactory = FaresConfiguration.fromConfig(node);
        if (fareFactory != null) subFactories.add(fareFactory);
      }
    }
    if (subFactories.isEmpty()) throw new IllegalArgumentException(
      "Empty fare composite. Please specify either a 'fares' array or a list of 'fareXxx' properties"
    );
    if (subFactories.size() == 1) {
      // Legal, but suspicious.
      log.warn(
        "Fare composite has only ONE fare to combine. This is allowed, but useless. Did you forgot to define a second fare to combine?"
      );
    }
  }

  protected abstract FareService makeMultipleFareService(List<FareService> subServices);

  public static class AddingMultipleFareServiceFactory extends MultipleFareServiceFactory {

    @Override
    protected FareService makeMultipleFareService(List<FareService> subServices) {
      return new AddingMultipleFareService(subServices);
    }
  }
}
