package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.vertex.Vertex;

public abstract class OsmEdge extends Edge {

  protected OsmEdge(Vertex v1, Vertex v2) {
    super(v1, v2);
  }

  public abstract void setBicycleSafetyFactor(float bicycleSafety);

  public abstract void setWalkSafetyFactor(float walkSafety);

  public abstract void setMotorVehicleNoThruTraffic(boolean motorVehicleNoThrough);

  public abstract void setBicycleNoThruTraffic(boolean bicycleNoThrough);

  public abstract void setWalkNoThruTraffic(boolean walkNoThrough);

  public abstract int getOutAngle();

  public abstract int getInAngle();

  public abstract void addTurnRestriction(TurnRestriction restriction);

  public void shareData(Edge reversedEdge) {}
}
