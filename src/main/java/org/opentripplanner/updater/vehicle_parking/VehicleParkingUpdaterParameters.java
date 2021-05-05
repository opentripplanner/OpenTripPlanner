package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class VehicleParkingUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;
  private final String url;
  private final String feedId;
  private final int frequencySec;
  private final String namePrefix;
  private final boolean zip;


  public VehicleParkingUpdaterParameters(
      String configRef,
      String feedId,
      String url,
      String namePrefix,
      int frequencySec,
      boolean zip
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.url = url;
    this.frequencySec = frequencySec;
    this.namePrefix = namePrefix;
    this.zip = zip;
  }

  @Override
  public int getFrequencySec() { return frequencySec; }

  /**
   * The config name/type for the updater. Used to reference the configuration element.
   */
  @Override
  public String getConfigRef() {
    return configRef;
  }

  KmlBikeParkDataSource.Parameters sourceParameters() {
    return new KmlBikeParkDataSource.Parameters() {
      @Override public String getUrl() { return url; }
      @Override public String getFeedId() { return feedId; }
      @Override public String getNamePrefix() { return namePrefix; }
      @Override public boolean zip() { return zip; }
    };
  }
}
