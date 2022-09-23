package org.opentripplanner.ext.vehicleparking.hslpark;

import java.time.ZoneId;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by {@link
 * HslParkUpdater}.
 */
public class HslParkUpdaterParameters extends VehicleParkingUpdaterParameters {

  private final int facilitiesFrequencySec;
  private final String facilitiesUrl;
  private final String feedId;
  private final String utilizationsUrl;
  private final ZoneId timeZone;

  public HslParkUpdaterParameters(
    String configRef,
    int facilitiesFrequencySec,
    String facilitiesUrl,
    String feedId,
    DataSourceType sourceType,
    int utilizationsFrequencySec,
    String utilizationsUrl,
    ZoneId timeZone
  ) {
    super(configRef, utilizationsFrequencySec, sourceType);
    this.facilitiesFrequencySec = facilitiesFrequencySec;
    this.facilitiesUrl = facilitiesUrl;
    this.feedId = feedId;
    this.utilizationsUrl = utilizationsUrl;
    this.timeZone = timeZone;
  }

  public int getFacilitiesFrequencySec() {
    return facilitiesFrequencySec;
  }

  public String getFeedId() {
    return feedId;
  }

  public String getFacilitiesUrl() {
    return facilitiesUrl;
  }

  public String getUtilizationsUrl() {
    return utilizationsUrl;
  }

  public ZoneId getTimeZone() {
    return timeZone;
  }
}
