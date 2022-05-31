package org.opentripplanner.transit.model.site;

import static org.opentripplanner.common.geometry.GeometryUtils.getGeometryFactory;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.transit.model.base.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.transit.model.framework.TransitEntity;
import org.opentripplanner.util.I18NString;

/**
 * A grouping of stops in GTFS or the lowest level grouping in NeTEx. It can be a train station, a
 * bus terminal, or a bus station (with a bus stop at each side of the road). Equivalent to GTFS
 * stop location type 1 or NeTEx monomodal StopPlace.
 */
public class Station extends TransitEntity implements StopCollection, LogInfo {

  public static final StopTransferPriority DEFAULT_PRIORITY = StopTransferPriority.ALLOWED;

  private final I18NString name;

  private final String code;

  private final I18NString description;

  private final WgsCoordinate coordinate;

  private final StopTransferPriority priority;
  /**
   * URL to a web page containing information about this particular station
   */
  private final I18NString url;
  private final TimeZone timezone;

  // We serialize this class to json only for snapshot tests, and this creates cyclical structures
  @JsonBackReference
  private final Set<StopLocation> childStops = new HashSet<>();

  private GeometryCollection geometry;

  public Station(
    FeedScopedId id,
    I18NString name,
    WgsCoordinate coordinate,
    String code,
    I18NString description,
    I18NString url,
    TimeZone timezone,
    StopTransferPriority priority
  ) {
    super(id);
    this.name = name;
    this.coordinate = coordinate;
    this.code = code;
    this.description = description;
    this.url = url;
    this.timezone = timezone;
    this.priority = priority == null ? DEFAULT_PRIORITY : priority;
    // Initialize the geometry with an empty set of children
    this.geometry = computeGeometry(coordinate, Set.of());
  }

  public void addChildStop(Stop stop) {
    this.childStops.add(stop);
    this.geometry = computeGeometry(coordinate, childStops);
  }

  public boolean includes(StopLocation stop) {
    return childStops.contains(stop);
  }

  @Nonnull
  public I18NString getName() {
    return name;
  }

  @Nonnull
  public Collection<StopLocation> getChildStops() {
    return childStops;
  }

  public double getLat() {
    return coordinate.latitude();
  }

  public double getLon() {
    return coordinate.longitude();
  }

  @Nonnull
  public WgsCoordinate getCoordinate() {
    return coordinate;
  }

  /** Public facing station code (short text or number) */
  @Nullable
  public String getCode() {
    return code;
  }

  /** Additional information about the station (if needed) */
  @Nullable
  public I18NString getDescription() {
    return description;
  }

  @Nullable
  public I18NString getUrl() {
    return url;
  }

  /**
   * The generalized cost priority associated with the stop independently of trips, routes and/or
   * other stops. This is supported in NeTEx, but not in GTFS. This should work by adding adjusting
   * the cost for all board-/alight- events in the routing search.
   * <p/>
   * To not interfere with request parameters this must be implemented in a neutral way. This mean
   * that the {@link StopTransferPriority#ALLOWED} (which is default) should a nett-effect of adding
   * 0 - zero cost.
   */
  @Nonnull
  public StopTransferPriority getPriority() {
    return priority;
  }

  @Nullable
  public TimeZone getTimezone() {
    return timezone;
  }

  /**
   * A geometry collection that contains the center point and the convex hull of all the child
   * stops.
   */
  @Nonnull
  public GeometryCollection getGeometry() {
    return geometry;
  }

  private static GeometryCollection computeGeometry(
    WgsCoordinate coordinate,
    Set<StopLocation> childStops
  ) {
    Point stationPoint = null;
    var childGeometries = childStops
      .stream()
      .map(StopLocation::getGeometry)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    if (coordinate != null) {
      stationPoint = getGeometryFactory().createPoint(coordinate.asJtsCoordinate());
      childGeometries.add(stationPoint);
    }
    var geometryCollection = getGeometryFactory()
      .createGeometryCollection(childGeometries.toArray(new Geometry[] {}));
    var convexHull = new ConvexHull(geometryCollection).getConvexHull();

    var geometries = stationPoint != null
      ? new Geometry[] { stationPoint, convexHull }
      : new Geometry[] { convexHull };
    return getGeometryFactory().createGeometryCollection(geometries);
  }

  @Override
  @Nullable
  public String logName() {
    return name == null ? null : name.toString();
  }
}
