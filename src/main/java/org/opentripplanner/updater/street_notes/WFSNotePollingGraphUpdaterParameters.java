package org.opentripplanner.updater.street_notes;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class WFSNotePollingGraphUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;

  private final String url;

  private final String featureType;

  private final int frequencySec;

  public WFSNotePollingGraphUpdaterParameters(
      String configRef, String url, String featureType, int frequencySec
  ) {
    this.configRef = configRef;
    this.url = url;
    this.featureType = featureType;
    this.frequencySec = frequencySec;
  }

  public String getUrl() { return url; }

  @Override
  public int getFrequencySec() { return frequencySec; }

  public String getFeatureType() { return featureType; }

  public String getConfigRef() { return configRef; }

}
