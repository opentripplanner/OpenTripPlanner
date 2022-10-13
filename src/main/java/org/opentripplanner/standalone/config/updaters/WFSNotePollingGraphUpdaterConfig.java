package org.opentripplanner.standalone.config.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.street_note.WFSNotePollingGraphUpdaterParameters;

public class WFSNotePollingGraphUpdaterConfig {

  public static WFSNotePollingGraphUpdaterParameters create(String configRef, NodeAdapter c) {
    return new WFSNotePollingGraphUpdaterParameters(
      configRef,
      c.of("url").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString(),
      c
        .of("featureType")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(),
      c.of("frequencySec").withDoc(NA, /*TODO DOC*/"TODO").asInt(60)
    );
  }
}
