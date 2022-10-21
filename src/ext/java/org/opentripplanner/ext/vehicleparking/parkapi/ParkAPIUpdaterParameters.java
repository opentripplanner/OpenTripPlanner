package org.opentripplanner.ext.vehicleparking.parkapi;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;

/**
 * Class that extends {@link VehicleParkingUpdaterParameters} with parameters required by {@link
 * ParkAPIUpdater}.
 */
public class ParkAPIUpdaterParameters extends VehicleParkingUpdaterParameters {

  private final String url;
  private final String feedId;
  private final Map<String, String> httpHeaders;
  private final List<String> tags;
  private final ZoneId timeZone;

  public ParkAPIUpdaterParameters(
    String configRef,
    String url,
    String feedId,
    int frequencySec,
    @Nonnull Map<String, String> httpHeaders,
    List<String> tags,
    DataSourceType sourceType,
    ZoneId timeZone
  ) {
    super(configRef, frequencySec, sourceType);
    this.url = url;
    this.feedId = feedId;
    this.httpHeaders = httpHeaders;
    this.tags = tags;
    this.timeZone = timeZone;
  }

  public String getFeedId() {
    return feedId;
  }

  public String getUrl() {
    return url;
  }

  public Map<String, String> getHttpHeaders() {
    return httpHeaders;
  }

  public Collection<String> getTags() {
    return tags;
  }

  public ZoneId getTimeZone() {
    return timeZone;
  }
}
