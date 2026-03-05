package org.opentripplanner.ext.ojp.parameters;

import javax.annotation.Nullable;

public class OjpApiParameters {

  private final boolean hideFeedId;
  private final String hardcodedInputFeedId;

  public OjpApiParameters(boolean hideFeedId, String hardcodedInputFeedId) {
    if (hideFeedId && hardcodedInputFeedId == null) {
      throw new IllegalArgumentException(
        "If `hideFeedId` is set to `true`, `hardcodedInputFeedId` must also be set."
      );
    }
    this.hideFeedId = hideFeedId;
    this.hardcodedInputFeedId = hardcodedInputFeedId;
  }

  public boolean hideFeedId() {
    return hideFeedId;
  }

  @Nullable
  public String hardcodedInputFeedId() {
    return hardcodedInputFeedId;
  }
}
