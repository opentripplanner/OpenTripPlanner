package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.vertex.StreetVertex;

public final class TemporaryPartialStreetEdge extends StreetEdge implements TemporaryEdge {

  /**
   * The edge on which this lies.
   */
  private final StreetEdge parentEdge;

  // An explicit geometry is stored, so that it may still be retrieved after this edge is removed
  // from the graph and the from/to vertices are set to null.
  private final LineString geometry;

  /**
   * Create a new partial street edge along the given 'parentEdge' from 'v1' to 'v2'. If the length
   * is negative, a new length is calculated from the geometry. The elevation data is calculated
   * using the 'parentEdge' and given 'length'.
   */
  public TemporaryPartialStreetEdge(
    StreetEdge parentEdge,
    StreetVertex v1,
    StreetVertex v2,
    LineString geometry,
    I18NString name,
    double length
  ) {
    super(v1, v2, geometry, name, length, parentEdge.getPermission(), false);
    this.parentEdge = parentEdge;
    this.geometry = super.getGeometry();
  }

  /**
   * Create a new partial street edge along the given 'parentEdge' from 'v1' to 'v2'. The length is
   * calculated using the provided geometry. The elevation data is calculated using the 'parentEdge'
   * and the calculated 'length'.
   */
  TemporaryPartialStreetEdge(
    StreetEdge parentEdge,
    StreetVertex v1,
    StreetVertex v2,
    LineString geometry,
    I18NString name,
    boolean back
  ) {
    super(v1, v2, geometry, name, parentEdge.getPermission(), back);
    this.parentEdge = parentEdge;
    this.geometry = super.getGeometry();
  }

  /**
   * This implementation makes it so that TurnRestrictions on the parent edge are applied to this
   * edge as well.
   */
  @Override
  public boolean isEquivalentTo(Edge e) {
    return (e == this || e == parentEdge);
  }

  @Override
  public boolean isReverseOf(Edge e) {
    Edge other = e;
    if (e instanceof TemporaryPartialStreetEdge) {
      other = ((TemporaryPartialStreetEdge) e).parentEdge;
    }

    // TODO(flamholz): is there a case where a partial edge has a reverse of its own?
    return parentEdge.isReverseOf(other);
  }

  /**
   * Returns true if this edge is trivial - beginning and ending at the same point.
   */
  public boolean isTrivial() {
    Coordinate fromCoord = this.getFromVertex().getCoordinate();
    Coordinate toCoord = this.getToVertex().getCoordinate();
    return fromCoord.equals(toCoord);
  }

  public StreetEdge getParentEdge() {
    return parentEdge;
  }

  @Override
  public String toString() {
    return (
      "TemporaryPartialStreetEdge(" +
      this.getDefaultName() +
      ", " +
      this.getFromVertex() +
      " -> " +
      this.getToVertex() +
      " length=" +
      this.getDistanceMeters() +
      " carSpeed=" +
      this.getCarSpeed() +
      " parentEdge=" +
      parentEdge +
      ")"
    );
  }

  @Override
  public boolean isRoundabout() {
    return parentEdge.isRoundabout();
  }

  @Override
  public LineString getGeometry() {
    return geometry;
  }

  /**
   * Have the inbound angle of  their parent.
   */
  @Override
  public int getInAngle() {
    return parentEdge.getInAngle();
  }

  /**
   * Have the outbound angle of  their parent.
   */
  @Override
  public int getOutAngle() {
    return parentEdge.getInAngle();
  }
}
