package org.opentripplanner.street.model.vertex;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A station entrance extracted from OSM and therefore not (yet) associated with the transit
 * entity {@link org.opentripplanner.transit.model.site.Station}.
 */
public class StationEntranceVertex extends OsmVertex {

  private static final String FEED_ID = "osm";
  private final String code;
  private final boolean wheelchairAccessible;

  public StationEntranceVertex(
    double lat,
    double lon,
    long nodeId,
    String code,
    boolean wheelchairAccessible
  ) {
    super(lat, lon, nodeId);
    this.code = code;
    this.wheelchairAccessible = wheelchairAccessible;
  }

  /**
   * The id of the entrance which may or may not be human-readable.
   */
  public FeedScopedId id() {
    return new FeedScopedId(FEED_ID, String.valueOf(nodeId));
  }

  /**
   * Short human-readable code of the exit, like A or H3.
   * If we need a proper name like "Oranienplatz" we have to add a name field.
   */
  @Nullable
  public String code() {
    return code;
  }

  public Accessibility wheelchairAccessibility() {
    return wheelchairAccessible ? Accessibility.POSSIBLE : Accessibility.NOT_POSSIBLE;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(StationEntranceVertex.class)
      .addNum("nodeId", nodeId)
      .addStr("code", code)
      .toString();
  }
}
