package org.opentripplanner.apis.transmodel.model.stop;

import java.time.ZoneId;
import java.util.Collection;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;

public class MonoOrMultiModalStation {

  private final FeedScopedId id;

  private final I18NString name;

  private final double lat;

  private final double lon;

  /**
   * Public facing station code (short text or number)
   */
  private final String code;

  /**
   * Additional information about the station (if needed)
   */
  private final I18NString description;

  /**
   * URL to a web page containing information about this particular station
   */
  private final I18NString url;

  private final ZoneId timezone;

  private final Collection<StopLocation> childStops;

  private final MonoOrMultiModalStation parentStation;

  public MonoOrMultiModalStation(Station station, MultiModalStation parentStation) {
    this.id = station.getId();
    this.name = station.getName();
    this.lat = station.getLat();
    this.lon = station.getLon();
    this.code = station.getCode();
    this.description = station.getDescription();
    this.url = station.getUrl();
    this.timezone = station.getTimezone();
    this.childStops = station.getChildStops();
    this.parentStation = parentStation != null ? new MonoOrMultiModalStation(parentStation) : null;
  }

  public MonoOrMultiModalStation(MultiModalStation multiModalStation) {
    this.id = multiModalStation.getId();
    this.name = multiModalStation.getName();
    this.lat = multiModalStation.getLat();
    this.lon = multiModalStation.getLon();
    this.code = multiModalStation.getCode();
    this.description = NonLocalizedString.ofNullable(multiModalStation.getDescription());
    this.url = multiModalStation.getUrl();
    this.timezone = null;
    this.childStops = multiModalStation.getChildStops();
    this.parentStation = null;
  }

  public final FeedScopedId getId() {
    return id;
  }

  public I18NString getName() {
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

  public I18NString getDescription() {
    return description;
  }

  public I18NString getUrl() {
    return url;
  }

  public ZoneId getTimezone() {
    return timezone;
  }

  public Collection<StopLocation> getChildStops() {
    return childStops;
  }

  public MonoOrMultiModalStation getParentStation() {
    return parentStation;
  }

  @Override
  public final int hashCode() {
    return getId().hashCode();
  }

  /**
   * Uses the  {@code id} for identity. We could use the {@link Object#equals(Object)} method, but
   * this causes the equals to fail in cases were the same entity is created twice - for example
   * after reloading a serialized instance.
   */
  @Override
  public final boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MonoOrMultiModalStation other = (MonoOrMultiModalStation) obj;
    return getId().equals(other.getId());
  }

  /**
   * Provide a default toString implementation with class name and id.
   */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + '{' + getId() + '}';
  }
}
