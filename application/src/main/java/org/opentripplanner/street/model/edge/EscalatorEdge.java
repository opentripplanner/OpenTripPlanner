package org.opentripplanner.street.model.edge;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;

/** Represents an escalator. An escalator edge can only be traversed by walking */
public class EscalatorEdge extends Edge {

  private static final LocalizedString NAME = new LocalizedString("name.escalator");
  private final double length;
  private final Duration duration;

  private EscalatorEdge(Vertex v1, Vertex v2, double length, Duration duration) {
    super(v1, v2);
    this.length = length;
    this.duration = duration;
  }

  @Override
  public State[] traverse(State s0) {
    // Only allow traversal by walking
    if (s0.currentMode() == TraverseMode.WALK && !s0.getRequest().wheelchair()) {
      var s1 = s0.edit(this);
      double time;
      if (duration == null) {
        time = getDistanceMeters() / s0.getPreferences().walk().escalator().speed();
      } else {
        time = duration.toSeconds();
      }
      s1.incrementWeight(s0.getPreferences().walk().escalator().reluctance() * time);
      s1.incrementTimeInSeconds((long) time);
      s1.incrementWalkDistance(getDistanceMeters());
      return s1.makeStateArray();
    } else return State.empty();
  }

  @Override
  public LineString getGeometry() {
    return GeometryUtils.makeLineString(fromv.getCoordinate(), tov.getCoordinate());
  }

  @Override
  public double getDistanceMeters() {
    return length;
  }

  /**
   * Parsed content of duration tag in OSM, if any. Not a calculated value.
   * @return Duration, or empty
   */
  public Optional<Duration> getDuration() {
    return Optional.ofNullable(duration);
  }

  @Override
  public I18NString getName() {
    return NAME;
  }

  public static EscalatorEdge createEscalatorEdge(
    Vertex from,
    Vertex to,
    double length,
    @Nullable Duration duration
  ) {
    return connectToGraph(new EscalatorEdge(from, to, length, duration));
  }
}
