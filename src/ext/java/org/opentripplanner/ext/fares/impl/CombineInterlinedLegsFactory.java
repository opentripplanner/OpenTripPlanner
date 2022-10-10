package org.opentripplanner.ext.fares.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.fares.impl.CombinedInterlinedLegsFareService.CombinationMode;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.standalone.config.NodeAdapter;

public class CombineInterlinedLegsFactory extends DefaultFareServiceFactory {

  private CombinationMode mode = CombinationMode.ALWAYS;

  @Override
  public FareService makeFareService() {
    return new CombinedInterlinedLegsFareService(mode);
  }

  @Override
  public void configure(JsonNode config) {
    var adapter = new NodeAdapter(config, null);
    mode = adapter.asEnum("mode", CombinationMode.ALWAYS);
  }
}
