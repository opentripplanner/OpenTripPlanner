package org.opentripplanner.ext.flex.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
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
  public StopLocation s1;
  public StopLocation s2;
  public FlexAccessEgressTemplate flexTemplate;
  public FlexPath flexPath;

  public FlexTripEdge(
    Vertex v1,
    Vertex v2,
    StopLocation s1,
    StopLocation s2,
    FlexTrip trip,
    FlexAccessEgressTemplate flexTemplate,
    FlexPathCalculator calculator
  ) {
    // Why is this code so dirty? Because we don't want this edge to be added to the edge lists.
    // The first parameter in Vertex constructor is graph. If it is null, the vertex isn't added to it.
    super(new Vertex(null, null, 0.0, 0.0) {}, new Vertex(null, null, 0.0, 0.0) {});
    this.s1 = s1;
    this.s2 = s2;
    this.trip = trip;
    this.flexTemplate = flexTemplate;
    this.fromv = v1;
    this.tov = v2;
    this.flexPath =
      calculator.calculateFlexPath(
        fromv,
        tov,
        flexTemplate.fromStopIndex,
        flexTemplate.toStopIndex
      );
  }

  public int getTimeInSeconds() {
    return flexPath.durationSeconds;
  }

  @Override
  public State traverse(State s0) {
    if (this.flexPath == null) {
      // not routable
      return null;
    }
    StateEditor editor = s0.edit(this);
    editor.setBackMode(TraverseMode.FLEX);
    // TODO: decide good value
    editor.incrementWeight(10 * 60);
    int timeInSeconds = getTimeInSeconds();
    editor.incrementTimeInSeconds(timeInSeconds);
    editor.incrementWeight(timeInSeconds);
    editor.resetEnteredNoThroughTrafficArea();
    return editor.makeState();
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
