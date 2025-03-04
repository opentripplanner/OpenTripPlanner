package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This fare service allows transfers between operators and, if the route transferred to is run by
 * an operator with a higher fare, the customer will be charged the higher fare. Also, the higher
 * fare is used up until the end of the free transfer window. The length of the free transfer window
 * is configurable, but defaults to 2.5 hours.
 * <p>
 * Additionally, there is an option to treat interlined transfers as actual transfers (with respect
 * to fares). This means that interlining between two routes with different fares will result in the
 * higher fare being charged. This is a work-around for transit agencies that choose to code their
 * fares in a route-based fashion instead of a zone-based fashion.
 * <p>
 * This calculator is maintained by IBI Group.
 */
public class HighestFareInFreeTransferWindowFareServiceFactory extends DefaultFareServiceFactory {

  protected Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();

  // default to 150 minutes to preserve compatibility with legacy pdx fares
  private Duration freeTransferWindow = Duration.ofMinutes(150);
  // If true, will also analyze more than just the first trip of an interlined transfer and will use the highest fare
  private boolean analyzeInterlinedTransfers = false;

  public FareService makeFareService() {
    return new HighestFareInFreeTransferWindowFareService(
      regularFareRules.values(),
      freeTransferWindow,
      analyzeInterlinedTransfers
    );
  }

  /**
   * This step ensures that the fares in the source GTFS data are accounted for correctly.
   */
  @Override
  public void processGtfs(FareRulesData fareRulesData, OtpTransitService transitService) {
    fillFareRules(fareRulesData.fareAttributes(), fareRulesData.fareRules(), regularFareRules);
  }

  @Override
  public void configure(JsonNode config) {
    var adapter = new NodeAdapter(config, null);
    freeTransferWindow = adapter
      .of("freeTransferWindow")
      .since(NA)
      .summary("TODO")
      .asDuration(freeTransferWindow);

    analyzeInterlinedTransfers = adapter
      .of("analyzeInterlinedTransfers")
      .since(NA)
      .summary("TODO")
      .asBoolean(analyzeInterlinedTransfers);
  }
}
