package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class FlexConfig {

  public static FlexConfig DEFAULT = new FlexConfig();
  private static final int DEFAULT_MAX_TRANSFER_SECONDS = 60 * 5; // 5 minutes

  public final int maxTransferSeconds;

  private FlexConfig() {
    maxTransferSeconds = DEFAULT_MAX_TRANSFER_SECONDS;
  }

  public FlexConfig(NodeAdapter json) {
    maxTransferSeconds =
      json
        .of("maxTransferDurationSeconds")
        .since(V2_1)
        .summary("How long should you be allowed to walk from a flex vehicle to a transit one.")
        .description(
          "How long should a passenger be allowed to walk after getting out of a flex vehicle and transferring to a flex or transit one. " +
          "This was mainly introduced to improve performance which is also the reason for not using the existing value with the same name: fixed schedule transfers are computed during the graph build but flex ones are calculated at request time and are more sensitive to slowdown. " +
          "A lower value means that the routing is faster."
        )
        .asInt(DEFAULT_MAX_TRANSFER_SECONDS);
  }
}
