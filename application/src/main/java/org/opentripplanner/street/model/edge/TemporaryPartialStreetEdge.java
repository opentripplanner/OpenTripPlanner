package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

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
  TemporaryPartialStreetEdge(TemporaryPartialStreetEdgeBuilder builder) {
    super(builder);
    builder
      .fromVertex()
      .addRentalRestriction(builder.parentEdge().getFromVertex().rentalRestrictions());
    builder
      .toVertex()
      .addRentalRestriction(builder.parentEdge().getToVertex().rentalRestrictions());
    this.parentEdge = builder.parentEdge();
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
    return buildToString(this.getDefaultName(), b ->
      b.append(", length=").append(this.getCarSpeed()).append(", parentEdge=").append(parentEdge)
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
