package org.opentripplanner.osm.model;

import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.osm.OsmProvider;

public class OsmNode extends OsmEntity {

  private static final Set<String> RAILWAY_STATION_ENTRANCE_TAGS = Set.of(
    "subway_entrance",
    "train_station_entrance"
  );

  public final double lat;
  public final double lon;

  OsmNode(long id, double lat, double lon, Map<String, String> tags, OsmProvider osmProvider) {
    super(id, tags, osmProvider);
    this.lat = lat;
    this.lon = lon;
  }

  public static OsmNodeBuilder of() {
    return new OsmNodeBuilder();
  }

  public OsmNodeBuilder copy() {
    return new OsmNodeBuilder(this);
  }

  public Coordinate getCoordinate() {
    return new Coordinate(this.lon, this.lat);
  }

  public boolean hasHighwayTrafficLight() {
    return "traffic_signals".equals(getTag("highway"));
  }

  public boolean hasCrossingTrafficLight() {
    return "traffic_signals".equals(getTag("crossing"));
  }

  /**
   * Checks if this node blocks traversal in any way
   *
   * @return true if it does
   */
  public boolean isBarrier() {
    // the majority of nodes have no tags at all, so this yields a good speed-up
    if (this.isTagless()) {
      return false;
    }
    return overridePermissions(ALL) != ALL;
  }

  /**
   * Checks if this node is a station entrance.
   *
   * @return true if it is
   */
  public boolean isStationEntrance() {
    return (
      isOneOfTags("railway", RAILWAY_STATION_ENTRANCE_TAGS) || isTag("public_transport", "entrance")
    );
  }

  /**
   * @return True if this entity provides an entrance to a platform or similar entity
   */
  public boolean isEntrance() {
    // the majority of nodes have no tags at all, so this yields a good speed-up
    if (this.isTagless()) {
      return false;
    }
    return (
      (isStationEntrance() || isTag("entrance", "yes") || isTag("entrance", "main")) &&
      !isTag("access", "private") &&
      !isTag("access", "no")
    );
  }

  /** checks for units (m/ft) in an OSM ele tag value, and returns the value in meters */
  public OptionalDouble parseEleTag() {
    var ele = getTag("ele");
    if (ele == null) {
      return OptionalDouble.empty();
    }
    ele = ele.toLowerCase();
    double unit = 1;
    if (ele.endsWith("m")) {
      ele = ele.replaceFirst("\\s*m", "");
    } else if (ele.endsWith("ft")) {
      ele = ele.replaceFirst("\\s*ft", "");
      unit = 0.3048;
    }
    try {
      return OptionalDouble.of(Double.parseDouble(ele) * unit);
    } catch (NumberFormatException e) {
      return OptionalDouble.empty();
    }
  }

  @Override
  public String url() {
    return String.format("https://www.openstreetmap.org/node/%d", getId());
  }

  /**
   * Check if this node represents a tagged barrier crossing if placed on an intersection of a
   * highway and a barrier way.
   *
   * @return true if it has a barrier tag, or if it explicitly overrides permissions.
   */
  public boolean isTaggedBarrierCrossing() {
    return (
      hasTag("barrier") ||
      hasTag("access") ||
      hasTag("entrance") ||
      overridePermissions(ALL) != ALL ||
      overridePermissions(NONE) != NONE
    );
  }

  /**
   * Check if this node represents access to a platform.
   * <p>
   * If this node appears inside a platform area and belongs to the same public transport relation,
   * the platform will be kept even if it isn't physically linked to this node so that
   * {@link org.opentripplanner.graph_builder.module.OsmBoardingLocationsModule} can associate the
   * transit stop with the physical platform.
   */
  public boolean isPlatformAccess() {
    return isEntrance() || isBoardingLocation() || isTag("highway", "elevator");
  }
}
