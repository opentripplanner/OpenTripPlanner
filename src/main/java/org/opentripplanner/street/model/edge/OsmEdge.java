package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Abstract base class for edges derived from OSM data.
 */
public abstract class OsmEdge extends Edge {

  protected OsmEdge(Vertex v1, Vertex v2) {
    super(v1, v2);
  }

  /**
   * Set the bicycle safety factor.
   */
  public abstract void setBicycleSafetyFactor(float bicycleSafety);

  /**
   * Set the walk safety factor.
   */
  public abstract void setWalkSafetyFactor(float walkSafety);

  /**
   * Set if this edge is no-thru for motor vehicles.
   */
  public abstract void setMotorVehicleNoThruTraffic(boolean motorVehicleNoThrough);

  /**
   * Set if this edge is no-thru for bicycles.
   */
  public abstract void setBicycleNoThruTraffic(boolean bicycleNoThrough);

  public abstract void setWalkNoThruTraffic(boolean walkNoThrough);

  /**
   * Return the azimuth of the last segment in this edge in integer degrees clockwise from South.
   */
  public abstract int getOutAngle();

  /**
   * Return the azimuth of the first segment in this edge in integer degrees clockwise from South.
   * TODO change everything to clockwise from North
   */
  public abstract int getInAngle();

  /**
   * Add a turn restriction to this edge.
   */
  public abstract void addTurnRestriction(TurnRestriction restriction);

  /**
   * Deduplicate data of two edges (possibly there and back) in memory.
   */
  public void shareData(Edge reversedEdge) {}
}
