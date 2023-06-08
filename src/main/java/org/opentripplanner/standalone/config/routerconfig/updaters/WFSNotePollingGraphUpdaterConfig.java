package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.street_note.WFSNotePollingGraphUpdaterParameters;

public class WFSNotePollingGraphUpdaterConfig {

  public static WFSNotePollingGraphUpdaterParameters create(String configRef, NodeAdapter c) {
    return new WFSNotePollingGraphUpdaterParameters(
      configRef,
      c.of("url").since(NA).summary("TODO").asString(),
      c.of("featureType").since(NA).summary("TODO").asString(),
      c.of("frequency").since(NA).summary("TODO").asDuration(Duration.ofMinutes(1))
    );
  }
}
