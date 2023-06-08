package org.opentripplanner.updater.street_note;

import java.time.Duration;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

public class WFSNotePollingGraphUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;

  private final String url;

  private final String featureType;

  private final Duration frequency;

  public WFSNotePollingGraphUpdaterParameters(
    String configRef,
    String url,
    String featureType,
    Duration frequency
  ) {
    this.configRef = configRef;
    this.url = url;
    this.featureType = featureType;
    this.frequency = frequency;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public Duration frequency() {
    return frequency;
  }

  public String configRef() {
    return configRef;
  }

  public String getFeatureType() {
    return featureType;
  }
}
