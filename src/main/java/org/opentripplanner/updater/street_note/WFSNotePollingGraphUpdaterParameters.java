package org.opentripplanner.updater.street_note;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class WFSNotePollingGraphUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;

  private final String url;

  private final String featureType;

  private final int frequencySec;

  public WFSNotePollingGraphUpdaterParameters(
    String configRef,
    String url,
    String featureType,
    int frequencySec
  ) {
    this.configRef = configRef;
    this.url = url;
    this.featureType = featureType;
    this.frequencySec = frequencySec;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public int frequencySec() {
    return frequencySec;
  }

  public String configRef() {
    return configRef;
  }

  public String getFeatureType() {
    return featureType;
  }
}
