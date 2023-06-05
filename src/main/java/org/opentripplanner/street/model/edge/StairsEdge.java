package org.opentripplanner.street.model.edge;

import static org.opentripplanner.framework.geometry.AngleUtils.calculateAngle;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.DirectionUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.state.State;

public class StairsEdge extends OsmEdge {

  private final I18NString name;
  private final LineString geometry;
  private final double distance;
  private final byte outAngle;
  private final byte inAngle;

  public StairsEdge(
    StreetVertex from,
    StreetVertex to,
    LineString geometry,
    I18NString name,
    double distance
  ) {
    super(from, to);
    this.name = name;
    this.geometry = geometry;
    this.distance = distance;
    this.outAngle = calculateAngle(DirectionUtils.getLastAngle(geometry));
    this.inAngle = calculateAngle(DirectionUtils.getFirstAngle(geometry));
  }

  @Override
  public State[] traverse(State s0) {
    // no cars on stairs
    if (s0.getNonTransitMode().isInCar()) {
      return State.empty();
    }

    var prefs = s0.getPreferences();
    double speed =
      switch (s0.getNonTransitMode()) {
        case WALK -> prefs.walk().speed();
        case BICYCLE, SCOOTER -> prefs.bike().walkingSpeed();
        case CAR, FLEX -> 0;
      };
    // slow down by stairsTimeFactor
    speed = speed / prefs.walk().stairsTimeFactor();

    double time = distance / speed;
    double weight =
      distance * prefs.walk().safetyFactor() + distance * (1 - prefs.walk().safetyFactor());
    weight /= speed;

    weight *= reluctance(s0, prefs);

    if (s0.getNonTransitMode().isCyclingIsh()) {
      weight = weight * prefs.bike().stairsReluctance();
    }

    var editor = s0.edit(this);
    editor.incrementWeight(weight);
    editor.incrementTimeInSeconds((int) time);

    return editor.makeStateArray();
  }

  private static double reluctance(State s0, RoutingPreferences prefs) {
    if (s0.getRequest().wheelchair()) {
      return StreetEdgeReluctanceCalculator.computeWheelchairReluctance(prefs, 0, true, true);
    } else {
      return StreetEdgeReluctanceCalculator.computeReluctance(
        prefs,
        s0.getNonTransitMode(),
        false,
        true
      );
    }
  }

  @Override
  public double getDistanceMeters() {
    return distance;
  }

  @Override
  public LineString getGeometry() {
    return geometry;
  }

  @Override
  public I18NString getName() {
    return this.name;
  }

  @Override
  public void setBicycleSafetyFactor(float bicycleSafety) {}

  @Override
  public void setWalkSafetyFactor(float walkSafety) {}

  @Override
  public void setMotorVehicleNoThruTraffic(boolean motorVehicleNoThrough) {}

  @Override
  public void setBicycleNoThruTraffic(boolean bicycleNoThrough) {}

  @Override
  public void setWalkNoThruTraffic(boolean walkNoThrough) {}

  @Override
  public int getOutAngle() {
    return outAngle;
  }

  @Override
  public int getInAngle() {
    return inAngle;
  }

  @Override
  public void addTurnRestriction(TurnRestriction restriction) {}
}
