package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.street_notes.WFSNotePollingGraphUpdaterParameters;

public class WFSNotePollingGraphUpdaterConfig {

  public static WFSNotePollingGraphUpdaterParameters create(String configRef, NodeAdapter c) {
    return new WFSNotePollingGraphUpdaterParameters(
      configRef,
      c.asText("url"),
      c.asText("featureType"),
      c.of("frequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60)
    );
  }
}
