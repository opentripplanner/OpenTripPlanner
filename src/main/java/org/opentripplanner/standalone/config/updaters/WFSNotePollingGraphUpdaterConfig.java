package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.street_notes.WFSNotePollingGraphUpdater;

public class WFSNotePollingGraphUpdaterConfig extends PollingGraphUpdaterConfig
    implements WFSNotePollingGraphUpdater.Parameters {

  private final String featureType;

  public WFSNotePollingGraphUpdaterConfig(NodeAdapter c) {
    super(c);
    featureType = c.asText("featureType", null);
  }

  public String getFeatureType() { return featureType; }
}
