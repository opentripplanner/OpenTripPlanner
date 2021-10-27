package org.opentripplanner.standalone.config;

public class FlexConfig {
  public static final int DEFAULT_MAX_TRANSFER_METERS = 1000;
  public final int maxTransferMeters;

  public FlexConfig(NodeAdapter json) {
    maxTransferMeters = json.asInt("maxTransferMeters", DEFAULT_MAX_TRANSFER_METERS);
  }
}
