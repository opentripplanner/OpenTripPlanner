package org.opentripplanner.ext.flex.edgetype;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.template.FlexAccessEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.site.StopLocation;

public class FlexTripEdge extends Edge {

  private final FlexTrip trip;
  public final StopLocation s1;
  public final StopLocation s2;
  public final FlexAccessEgressTemplate flexTemplate;
  public final FlexPath flexPath;

  private FlexTripEdge(
    Vertex v1,
    Vertex v2,
    StopLocation s1,
    StopLocation s2,
    FlexTrip trip,
    FlexAccessEgressTemplate flexTemplate,
    FlexPath flexPath
  ) {
    super(v1, v2);
    this.s1 = s1;
    this.s2 = s2;
    this.trip = trip;
    this.flexTemplate = flexTemplate;
    this.flexPath = Objects.requireNonNull(flexPath);
  }

  /**
   * Create a Flex Trip.
   * Flex trips are not connected to the graph.
   */
  public static FlexTripEdge createFlexTripEdge(
    Vertex v1,
    Vertex v2,
    StopLocation s1,
    StopLocation s2,
    FlexTrip trip,
    FlexAccessEgressTemplate flexTemplate,
    FlexPath flexPath
  ) {
    return new FlexTripEdge(v1, v2, s1, s2, trip, flexTemplate, flexPath);
  }

  public int getTimeInSeconds() {
    return flexPath.durationSeconds;
  }

  @Override
  @Nonnull
  public State[] traverse(State s0) {
    StateEditor editor = s0.edit(this);
    editor.setBackMode(TraverseMode.FLEX);
    // TODO: decide good value
    editor.incrementWeight(10 * 60);
    int timeInSeconds = getTimeInSeconds();
    editor.incrementTimeInSeconds(timeInSeconds);
    editor.incrementWeight(timeInSeconds);
    editor.resetEnteredNoThroughTrafficArea();
    return editor.makeStateArray();
  }

  @Override
  public I18NString getName() {
    return null;
  }

  @Override
  public LineString getGeometry() {
    return flexPath.getGeometry();
  }

  @Override
  public double getDistanceMeters() {
    return flexPath.distanceMeters;
  }

  public FlexTrip getFlexTrip() {
    return trip;
  }
}
