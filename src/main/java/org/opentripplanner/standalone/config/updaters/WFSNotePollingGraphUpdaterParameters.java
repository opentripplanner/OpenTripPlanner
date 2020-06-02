package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.street_notes.WFSNotePollingGraphUpdater;

public class WFSNotePollingGraphUpdaterParameters extends PollingGraphUpdaterParameters
    implements WFSNotePollingGraphUpdater.Parameters {

  private final String featureType;

  public WFSNotePollingGraphUpdaterParameters(NodeAdapter c) {
    super(c);
    featureType = c.asText("featureType", null);
  }

  public String getFeatureType() { return featureType; }
}
