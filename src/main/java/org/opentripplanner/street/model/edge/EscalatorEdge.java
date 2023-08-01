package org.opentripplanner.street.model.edge;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;

/** Represents an escalator. An escalator edge can only be traversed by walking */
public class EscalatorEdge extends Edge {

  /* A quick internet search gives escalator speed range of 0.3-0.6 m/s and angle of 30 degrees.
   * Using the angle of 30 degrees and a speed of 0.5 m/s gives a horizontal component
   * of approx. 0.43 m/s */
  private static final double HORIZONTAL_SPEED = 0.45;
  private static final LocalizedString NAME = new LocalizedString("name.escalator");
  private final double length;

  private EscalatorEdge(Vertex v1, Vertex v2, double length) {
    super(v1, v2);
    this.length = length;
  }

  @Nonnull
  @Override
  public State[] traverse(State s0) {
    // Only allow traversal by walking
    if (s0.currentMode() == TraverseMode.WALK && !s0.getRequest().wheelchair()) {
      var s1 = s0.edit(this);
      var time = getDistanceMeters() / HORIZONTAL_SPEED;
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
    return NAME;
  }

  public static EscalatorEdge createEscalatorEdge(Vertex from, Vertex to, double length) {
    return connectToGraph(new EscalatorEdge(from, to, length));
  }
}
