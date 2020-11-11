package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.street_notes.WFSNotePollingGraphUpdaterParameters;

public class WFSNotePollingGraphUpdaterConfig {

  public static WFSNotePollingGraphUpdaterParameters create(String configRef, NodeAdapter c) {
    return new WFSNotePollingGraphUpdaterParameters(
        configRef,
        c.asText("url"),
        c.asText("featureType"),
        c.asInt("frequencySec", 60)
    );
  }
}
