package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * Parses the {@code transfers.via} block of the build config, the configuration of the transfers
 * between the via coordinate and the stops.
 */
public class ViaTransferConfig {

  private static final Duration DEFAULT_MAX_DURATION = Duration.ofMinutes(30);

  /** The longest transfer between a via coordinate and a stop. */
  public static Duration mapMaxDuration(NodeAdapter transfers) {
    return transfers
      .of("via")
      .since(V2_10)
      .summary("The configuration of the transfers between a via coordinate and the stops.")
      .asObject()
      .of("maxDuration")
      .since(V2_10)
      .summary("The longest transfer between a via coordinate and a stop.")
      .asDuration(DEFAULT_MAX_DURATION);
  }
}
