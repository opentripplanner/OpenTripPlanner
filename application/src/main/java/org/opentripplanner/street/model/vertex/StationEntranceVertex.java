package org.opentripplanner.street.model.vertex;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class StationEntranceVertex extends OsmVertex {

  private final String code;
  private final boolean accessible;

  public StationEntranceVertex(double x, double y, long nodeId, String code, boolean accessible) {
    super(x, y, nodeId);
    this.code = code;
    this.accessible = accessible;
  }

  public FeedScopedId id() {
    return new FeedScopedId("osm", String.valueOf(nodeId));
  }

  @Nullable
  public String code() {
    return code;
  }

  public Accessibility wheelchairAccessibility() {
    return accessible ? Accessibility.POSSIBLE : Accessibility.NOT_POSSIBLE;
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
