package org.opentripplanner.street.model.edge;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;

/** Represents an escalator. An escalator edge can only be traversed by walking */
public class EscalatorEdge extends Edge {

  private double length;

  private double speed = 0.45;
  public EscalatorEdge(Vertex v1, Vertex v2, double length) {
    super(v1, v2);
    this.length = length;
  }

  @Override
  public State[] traverse(State s0) {
    // Only allow traversal by walking
    if (s0.getNonTransitMode() == TraverseMode.WALK && !s0.getRequest().wheelchair()) {
      var s1 = s0.edit(this);
      var time = getDistanceMeters() / speed;
      s1.incrementWeight(s0.getPreferences().walk().escalatorReluctance() * time);
      s1.incrementTimeInSeconds((int) Math.round(time));
      s1.incrementWalkDistance(getDistanceMeters());
      return s1.makeStateArray();
    } else return State.empty();
  }

  @Override
  public double getDistanceMeters() {
    return length;
  }

  @Override
  public I18NString getName() {
    return null;
  }
}
