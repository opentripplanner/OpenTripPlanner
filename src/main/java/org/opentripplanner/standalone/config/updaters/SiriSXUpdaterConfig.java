package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.ext.siri.updater.SiriSXUpdater;
import org.opentripplanner.standalone.config.NodeAdapter;

public class SiriSXUpdaterConfig extends PollingGraphUpdaterConfig
    implements SiriSXUpdater.Parameters {

  private final String url;
  private final String requestorRef;
  private final int earlyStartSec;
  private final String feedId;
  private final int timeoutSec;
  private final boolean blockReadinessUntilInitialized;

  public SiriSXUpdaterConfig(NodeAdapter c) {
    super(c);
    url = c.asText("url", null);
    requestorRef = c.asText("requestorRef", null);
    earlyStartSec = c.asInt("earlyStartSec", -1);
    feedId = c.asText("feedId", null);
    timeoutSec = c.asInt("timeoutSec", -1);
    blockReadinessUntilInitialized = c.asBoolean("blockReadinessUntilInitialized", false);
  }

  public String getUrl() { return url; }

  public String getRequestorRef() { return requestorRef; }

  public int getEarlyStartSec() { return earlyStartSec; }

  public String getFeedId() { return feedId; }

  public int getTimeoutSec() { return timeoutSec; }

  public boolean blockReadinessUntilInitialized() { return blockReadinessUntilInitialized; }
}
