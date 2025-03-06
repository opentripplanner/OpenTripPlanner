package org.opentripplanner.transit.model.site;

import static org.opentripplanner.framework.geometry.GeometryUtils.getGeometryFactory;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;

/**
 * A grouping of stops in GTFS or the lowest level grouping in NeTEx. It can be a train station, a
 * bus terminal, or a bus station (with a bus stop at each side of the road). Equivalent to GTFS
 * stop location type 1 or NeTEx monomodal StopPlace.
 */
public class Station
  extends AbstractTransitEntity<Station, StationBuilder>
  implements StopLocationsGroup, LogInfo {

  private final I18NString name;
  private final String code;
  private final I18NString description;
  private final WgsCoordinate coordinate;
  private final boolean shouldRouteToCentroid;
  private final StopTransferPriority priority;
  private final I18NString url;
  private final ZoneId timezone;
  private final boolean transfersNotAllowed;

  // We serialize this class to json only for snapshot tests, and this creates cyclical structures
  @JsonBackReference
  private final Set<StopLocation> childStops = new HashSet<>();

  private GeometryCollection geometry;

  Station(StationBuilder builder) {
    super(builder.getId());
    // Required fields
    this.name = Objects.requireNonNull(builder.getName());
    this.coordinate = Objects.requireNonNull(builder.getCoordinate());
    this.shouldRouteToCentroid = builder.shouldRouteToCentroid();
    this.priority = Objects.requireNonNullElse(
      builder.getPriority(),
      StopTransferPriority.defaultValue()
    );
    this.transfersNotAllowed = builder.isTransfersNotAllowed();

    // Optional fields
    this.code = builder.getCode();
    this.description = builder.getDescription();
    this.url = builder.getUrl();
    this.timezone = builder.getTimezone();

    // Initialize the geometry with an empty set of children
    this.geometry = computeGeometry(coordinate, Set.of());
  }

  public static StationBuilder of(FeedScopedId id) {
    return new StationBuilder(id);
  }

  void addChildStop(RegularStop stop) {
    this.childStops.add(stop);
    this.geometry = computeGeometry(coordinate, childStops);
  }

  public boolean includes(StopLocation stop) {
    return childStops.contains(stop);
  }

  public I18NString getName() {
    return name;
  }

  public Collection<StopLocation> getChildStops() {
    return childStops;
  }

  @Override
  public double getLat() {
    return coordinate.latitude();
  }

  @Override
  public double getLon() {
    return coordinate.longitude();
  }

  public WgsCoordinate getCoordinate() {
    return coordinate;
  }

  /**
   * When doing a street search from/to the station, we can either route to the centroid of the station
   * or from/to any child stop. This feature is inactive unless configured.
   */
  public boolean shouldRouteToCentroid() {
    return shouldRouteToCentroid;
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

  /**
   * URL to a web page containing information about this particular station
   */
  @Nullable
  public I18NString getUrl() {
    return url;
  }

  /**
   * The generalized cost priority associated with the stop independently of trips, routes and/or
   * other stops. This is supported in NeTEx, but not in GTFS. However, it can be configured for
   * GTFS feeds. This should work by adding adjusting the cost for all board-/alight- events in the
   * routing search.
   * <p/>
   * To not interfere with request parameters this must be implemented in a neutral way. This mean
   * that the {@link StopTransferPriority#ALLOWED} (which is default) should a nett-effect of adding
   * 0 - zero cost.
   */
  public StopTransferPriority getPriority() {
    return priority;
  }

  @Nullable
  public ZoneId getTimezone() {
    return timezone;
  }

  /**
   * If true do not allow any transfers to or from any stop within station
   */
  public boolean isTransfersNotAllowed() {
    return transfersNotAllowed;
  }

  /**
   * A geometry collection that contains the center point and the convex hull of all the child
   * stops.
   */
  public GeometryCollection getGeometry() {
    return geometry;
  }

  @Override
  @Nullable
  public String logName() {
    return name == null ? null : name.toString();
  }

  @Override
  public StationBuilder copy() {
    return new StationBuilder(this);
  }

  @Override
  public boolean sameAs(Station other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.name) &&
      Objects.equals(code, other.code) &&
      Objects.equals(description, other.description) &&
      Objects.equals(coordinate, other.coordinate) &&
      Objects.equals(shouldRouteToCentroid, other.shouldRouteToCentroid) &&
      Objects.equals(priority, other.priority) &&
      Objects.equals(url, other.url) &&
      Objects.equals(timezone, other.timezone)
    );
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
}
