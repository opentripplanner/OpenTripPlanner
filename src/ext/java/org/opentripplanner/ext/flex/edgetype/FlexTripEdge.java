package org.opentripplanner.ext.flex.edgetype;

import java.time.LocalDate;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Flex trips edges are not connected to the graph.
 */
public class FlexTripEdge extends Edge {

  private final StopLocation s1;
  private final StopLocation s2;
  private final FlexTrip<?, ?> trip;
  private final int boardStopPosInPattern;
  private final int alightStopPosInPattern;
  private final LocalDate serviceDate;
  private final FlexPath flexPath;

  public FlexTripEdge(
    Vertex v1,
    Vertex v2,
    StopLocation s1,
    StopLocation s2,
    FlexTrip<?, ?> trip,
    int boardStopPosInPattern,
    int alightStopPosInPattern,
    LocalDate serviceDate,
    FlexPath flexPath
  ) {
    super(v1, v2);
    this.s1 = s1;
    this.s2 = s2;
    this.trip = trip;
    this.boardStopPosInPattern = boardStopPosInPattern;
    this.alightStopPosInPattern = alightStopPosInPattern;
    this.serviceDate = serviceDate;
    this.flexPath = Objects.requireNonNull(flexPath);
  }

  public StopLocation s1() {
    return s1;
  }

  public StopLocation s2() {
    return s2;
  }

  public int boardStopPosInPattern() {
    return boardStopPosInPattern;
  }

  public int alightStopPosInPattern() {
    return alightStopPosInPattern;
  }

  public LocalDate serviceDate() {
    return serviceDate;
  }

  public int getTimeInSeconds() {
    return flexPath.durationSeconds;
  }

  public FlexTrip<?, ?> getFlexTrip() {
    return trip;
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
}
