package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdater;

public class GtfsRealtimeAlertsUpdaterConfig
    extends PollingGraphUpdaterConfig
    implements GtfsRealtimeAlertsUpdater.Parameters {

  private final int earlyStartSec;

  private final String feedId;

  private final boolean fuzzyTripMatching;

  public GtfsRealtimeAlertsUpdaterConfig(NodeAdapter c)
  {
    super(c);
    earlyStartSec = c.asInt("earlyStartSec", 0);
    feedId = c.asText("feedId", null);
    fuzzyTripMatching = c.asBoolean("fuzzyTripMatching", false);
  }


  public int getEarlyStartSec() {
    return earlyStartSec; }

  public String getFeedId() {
    return feedId;
  }

  public boolean fuzzyTripMatching() {
    return fuzzyTripMatching;
  }

}
