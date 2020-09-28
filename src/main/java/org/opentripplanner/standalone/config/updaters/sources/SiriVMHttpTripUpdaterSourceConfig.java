package org.opentripplanner.standalone.config.updaters.sources;

import org.opentripplanner.ext.siri.updater.SiriVMHttpTripUpdateSource;
import org.opentripplanner.standalone.config.NodeAdapter;

public class SiriVMHttpTripUpdaterSourceConfig extends UpdaterSourceConfig implements
    SiriVMHttpTripUpdateSource.Parameters {

  private final String requestorRef;

  private final String feedId;

  private final int timeoutSec;

  public SiriVMHttpTripUpdaterSourceConfig(NodeAdapter c) {
    super(c);
    requestorRef = c.asText("requestorRef", null);
    feedId = c.asText("feedId", null);
    timeoutSec = c.asInt("timeoutSec", -1);
  }

  public String getRequestorRef() { return requestorRef; }

  public String getFeedId() { return feedId; }

  public int getTimeoutSec() { return timeoutSec; }
}
