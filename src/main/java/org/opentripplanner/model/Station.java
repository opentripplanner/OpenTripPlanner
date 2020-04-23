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
  public static final TransferPriority DEFAULT_COST_PRIORITY = TransferPriority.ALLOWED;

  private final FeedScopedId id;

  private final String name;

  private final String code;

  private final String description;

  private final WgsCoordinate coordinate;

  private final TransferPriority costPriority;

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
      TimeZone timezone,
      TransferPriority costPriority
  ) {
    this.id = id;
    this.name = name;
    this.coordinate = coordinate;
    this.code = code;
    this.description = description;
    this.url = url;
    this.timezone = timezone;
    this.costPriority = costPriority == null ? DEFAULT_COST_PRIORITY : costPriority;
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

  /** @throws UnsupportedOperationException */
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

  /** Public facing station code (short text or number) */
  public String getCode() {
    return code;
  }

  /** Additional information about the station (if needed) */
  public String getDescription() {
    return description;
  }

  public String getUrl() {
    return url;
  }

  /**
   * The generalized cost priority associated with the stop independently of trips, routes
   * and/or other stops. This is supported in NeTEx, but not in GTFS. This should work by
   * adding adjusting the cost for all board-/alight- events in the routing search.
   * <p/>
   * To not interfere with request parameters this must be implemented in a neutral way. This mean
   * that the {@link TransferPriority#ALLOWED} (witch is default) should a nett-effect of
   * adding 0 - zero cost.
   */
  public TransferPriority getCostPriority() {
    return costPriority;
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
