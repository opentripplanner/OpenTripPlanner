package org.opentripplanner.street.model.vertex;

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

  public OsmVertex(String label, double x, double y, long nodeId) {
    super(label, x, y);
    this.nodeId = nodeId;
  }

  public OsmVertex(
    String label,
    double x,
    double y,
    long nodeId,
    I18NString name,
    boolean hasHighwayTrafficLight,
    boolean hasCrossingTrafficLight
  ) {
    super(label, x, y, name, hasHighwayTrafficLight, hasCrossingTrafficLight);
    this.nodeId = nodeId;
  }
}
