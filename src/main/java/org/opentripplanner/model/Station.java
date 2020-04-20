package org.opentripplanner.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 * A grouping of stops in GTFS or the lowest level grouping in NeTEx. It can be a train station, a
 * bus terminal, or a bus station (with a bus stop at each side of the road). Equivalent to GTFS
 * stop location type 1 or NeTEx monomodal StopPlace.
 */
public class Station extends TransitEntity<FeedScopedId> implements StopCollection {

  private static final long serialVersionUID = 1L;

  private final FeedScopedId id;

  private final String name;

  /**
   * Public facing station code (short text or number)
   */
  private final String code;

  /**
   * Additional information about the station (if needed)
   */
  private final String description;

  private final WgsCoordinate coordinate;

  /**
   * URL to a web page containing information about this particular station
   */
  private final String url;

  private final TimeZone timezone;

  private final Set<Stop> childStops = new HashSet<>();

  public Station(
      FeedScopedId id,
      String name,
      WgsCoordinate coordinate,
      String code,
      String description,
      String url,
      TimeZone timezone
  ) {
    this.id = id;
    this.name = name;
    this.coordinate = coordinate;
    this.code = code;
    this.description = description;
    this.url = url;
    this.timezone = timezone;
  }

  public void addChildStop(Stop stop) {
    this.childStops.add(stop);
  }

  @Override
  public String toString() {
    return "<Station " + this.id + ">";
  }

  @Override
  public FeedScopedId getId() {
    return id;
  }

  @Override
  public final void setId(FeedScopedId id) {
    super.setId(id);
  }

  public String getName() {
    return name;
  }

  public WgsCoordinate getCoordinate() {
    return coordinate;
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

  public double getLat() {
    return coordinate.latitude();
  }

  public double getLon() {
    return coordinate.longitude();
  }
}
