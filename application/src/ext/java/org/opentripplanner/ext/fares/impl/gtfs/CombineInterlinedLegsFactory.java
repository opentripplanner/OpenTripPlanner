package org.opentripplanner.ext.fares.impl.gtfs;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.fares.impl.gtfs.CombinedInterlinedLegsFareService.CombinationMode;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class CombineInterlinedLegsFactory extends DefaultFareServiceFactory {

  private CombinationMode mode = CombinationMode.ALWAYS;

  @Override
  public FareService makeFareService() {
    var service = new CombinedInterlinedLegsFareService(mode);
    service.addFareRules(FareType.regular, regularFareRules.values());
    return service;
  }

  @Override
  public void configure(JsonNode config) {
    var adapter = new NodeAdapter(config, null);
    mode = adapter.of("mode").asEnum(CombinationMode.ALWAYS);
  }
}
