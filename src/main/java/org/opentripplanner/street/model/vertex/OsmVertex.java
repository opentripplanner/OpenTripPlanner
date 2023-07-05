package org.opentripplanner.street.model.vertex;

import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * A vertex coming from OpenStreetMap.
 * <p>
 * This class marks something that comes from the street network itself. It is used for linking
 * origins in Analyst to ensure that they are linked to the same locations regardless of changes in
 * the transit network between (or eventually within) graphs.
 */
public class OsmVertex extends IntersectionVertex {

  /** The OSM node ID from whence this came */
  public final long nodeId;

  public OsmVertex(double x, double y, long nodeId) {
    super(x, y);
    this.nodeId = nodeId;
  }

  public OsmVertex(
    double x,
    double y,
    long nodeId,
    @Nullable I18NString name,
    boolean hasHighwayTrafficLight,
    boolean hasCrossingTrafficLight
  ) {
    super(x, y, name, hasHighwayTrafficLight, hasCrossingTrafficLight);
    this.nodeId = nodeId;
  }

  @Override
  public VertexLabel getLabel() {
    return new VertexLabel.OsmNodeLabel(nodeId);
  }
}
