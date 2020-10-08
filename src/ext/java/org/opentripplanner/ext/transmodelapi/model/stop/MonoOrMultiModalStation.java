package org.opentripplanner.ext.transmodelapi.model.stop;

import org.opentripplanner.model.*;

import java.util.Collection;
import java.util.TimeZone;

public class MonoOrMultiModalStation extends TransitEntity {

  private final String name;

  private final double lat;

  private final double lon;

  /**
   * Public facing station code (short text or number)
   */
  private final String code;

  /**
   * Additional information about the station (if needed)
   */
  private final String description;

  /**
   * URL to a web page containing information about this particular station
   */
  private final String url;

  private final TimeZone timezone;

  private final Collection<Stop> childStops;

  private final MonoOrMultiModalStation parentStation;

  public MonoOrMultiModalStation(Station station, MultiModalStation parentStation) {
      super(station.getId());

      this.name = station.getName();
      this.lat = station.getLat();
      this.lon = station.getLon();
      this.code = station.getCode();
      this.description = station.getDescription();
      this.url = station.getUrl();
      this.timezone = station.getTimezone();
      this.childStops = station.getChildStops();
      this.parentStation =
          parentStation != null
              ? new MonoOrMultiModalStation(parentStation)
              : null;
  }

  public MonoOrMultiModalStation(MultiModalStation multiModalStation) {
    super(multiModalStation.getId());
    this.name = multiModalStation.getName();
    this.lat = multiModalStation.getLat();
    this.lon = multiModalStation.getLon();
    this.code = multiModalStation.getCode();
    this.description = multiModalStation.getDescription();
    this.url = multiModalStation.getUrl();
    this.timezone = null;
    this.childStops = multiModalStation.getChildStops();
    this.parentStation = null;
  }

  public String getName() {
    return name;
  }

  public double getLat() {
    return lat;
  }

  public double getLon() {
    return lon;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public String getUrl() {
    return url;
  }

  public TimeZone getTimezone() {
    return timezone;
  }

  public Collection<Stop> getChildStops() {
    return childStops;
  }

  public MonoOrMultiModalStation getParentStation() {
    return parentStation;
  }

  @Override
  public String toString() {
    return "<MonoOrMultiModalStation " + getId() + ">";
  }
}
