package org.opentripplanner.ext.trias.parameters;

import java.time.ZoneId;
import java.util.Optional;

public class TriasApiParameters {

  private final boolean hideFeedId;
  private final String hardcodedInputFeedId;
  private final ZoneId timeZone;

  public TriasApiParameters(boolean hideFeedId, String hardcodedInputFeedId, ZoneId timeZone) {
    if (hideFeedId && hardcodedInputFeedId == null) {
      throw new IllegalArgumentException(
        "If `hideFeedId` is set to `true`, `hardcodedInputFeedId` must also be set."
      );
    }
    this.hideFeedId = hideFeedId;
    this.hardcodedInputFeedId = hardcodedInputFeedId;
    this.timeZone = timeZone;
  }

  public boolean hideFeedId() {
    return hideFeedId;
  }

  public String hardcodedInputFeedId() {
    return hardcodedInputFeedId;
  }

  public Optional<ZoneId> timeZone() {
    return Optional.ofNullable(timeZone);
  }
}
