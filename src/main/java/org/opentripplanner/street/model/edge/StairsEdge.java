package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.state.State;

public class StairsEdge extends OsmEdge {
  private final I18NString name;
  private final LineString geometry;
  private final double distance;

  public StairsEdge(StreetVertex from, StreetVertex to, LineString geometry, I18NString name, double distance) {
    super(from, to);
    this.name = name;
    this.geometry = geometry;
    this.distance = distance;
  }

  @Override
  public State[] traverse(State s0) {

    // ban cars on stairs
    if(s0.getNonTransitMode().isInCar()) {
      return State.empty();
    }

    var prefs = s0.getPreferences();
    double speed = switch (s0.getNonTransitMode()){
      case WALK -> prefs.walk().speed();
      case BICYCLE, SCOOTER ->  prefs.bike().walkingSpeed();
      case CAR, FLEX -> 1;
    };

    var time = distance / speed;
    var weight =
      distance *
        prefs.walk().safetyFactor() +
        distance *
          (1 - prefs.walk().safetyFactor());
    weight /= speed;

    if(s0.getNonTransitMode().isCyclingIsh()) {
      weight *= prefs.bike().stairsReluctance();
    } else if(s0.getNonTransitMode().isWalking()) {
      weight *= prefs.walk().stairsReluctance();
    }

    var editor = s0.edit(this);
    editor.incrementWeight(weight);
    editor.incrementTimeInSeconds((int) time);

    return editor.makeStateArray();
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
  public void setBicycleSafetyFactor(float bicycleSafety) {

  }

  @Override
  public void setWalkSafetyFactor(float walkSafety) {

  }

  @Override
  public void setMotorVehicleNoThruTraffic(boolean motorVehicleNoThrough) {

  }

  @Override
  public void setBicycleNoThruTraffic(boolean bicycleNoThrough) {

  }

  @Override
  public void setWalkNoThruTraffic(boolean walkNoThrough) {

  }

  @Override
  public int getOutAngle() {
    return 0;
  }

  @Override
  public int getInAngle() {
    return 0;
  }

  @Override
  public void addTurnRestriction(TurnRestriction restriction) {

  }
}
