package org.opentripplanner.street.model.edge;

import static org.opentripplanner.framework.geometry.AngleUtils.calculateAngle;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.CompactLineStringUtils;
import org.opentripplanner.framework.geometry.DirectionUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.state.State;

/**
 * Represents a flight of stairs that are derived from OSM data.
 */
public class StairsEdge extends OsmEdge {

  // stairs are only allowed for walking and cycling/scootering
  private static final StreetTraversalPermission DEFAULT_PERMISSIONS =
    StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

  private final I18NString name;
  private final byte[] compactGeometry;
  private final double distance;
  private final byte outAngle;
  private final byte inAngle;
  private float walkSafetyFactor = 1f;

  public StairsEdge(
    StreetVertex from,
    StreetVertex to,
    LineString geometry,
    I18NString name,
    double distance
  ) {
    super(from, to);
    this.name = name;
    this.compactGeometry =
      CompactLineStringUtils.compactLineString(
        from.getX(),
        from.getY(),
        to.getX(),
        to.getY(),
        geometry,
        false
      );
    this.distance = distance;
    this.outAngle = calculateAngle(DirectionUtils.getLastAngle(geometry));
    this.inAngle = calculateAngle(DirectionUtils.getFirstAngle(geometry));
  }

  @Override
  public State[] traverse(State s0) {
    if (blockedByBarriers() || s0.getNonTransitMode().isInCar()) {
      return State.empty();
    }

    var prefs = s0.getPreferences();
    double speed =
      switch (s0.getNonTransitMode()) {
        case WALK -> prefs.walk().speed();
        case BICYCLE, SCOOTER -> prefs.bike().walkingSpeed();
        case CAR, FLEX -> throw new IllegalStateException();
      };
    // slow down by stairsTimeFactor
    speed = speed / prefs.walk().stairsTimeFactor();

    double time = distance / speed;
    double weight =
      getEffectiveWalkSafetyDistance() *
      prefs.walk().safetyFactor() +
      getEffectiveWalkSafetyDistance() *
      (1 - prefs.walk().safetyFactor());
    weight /= speed;

    weight *= reluctance(s0, prefs);

    var editor = s0.edit(this);
    editor.incrementWeight(weight);
    editor.incrementTimeInSeconds((int) time);

    return editor.makeStateArray();
  }

  @Override
  public I18NString getName() {
    return this.name;
  }

  @Override
  public LineString getGeometry() {
    return CompactLineStringUtils.uncompactLineString(
      fromv.getX(),
      fromv.getY(),
      tov.getX(),
      tov.getY(),
      compactGeometry,
      false
    );
  }

  @Override
  public double getDistanceMeters() {
    return distance;
  }

  @Override
  public void setBicycleSafetyFactor(float bicycleSafety) {
    // we ignore the bicycle safety factor, stairs are inherently unsafe and therefore have a separate
    // reluctance
  }

  @Override
  public void setWalkSafetyFactor(float walkSafety) {
    this.walkSafetyFactor = walkSafety;
  }

  @Override
  public void setMotorVehicleNoThruTraffic(boolean motorVehicleNoThrough) {
    // we are ignoring no-through on stairs
  }

  @Override
  public void setBicycleNoThruTraffic(boolean bicycleNoThrough) {
    // we are ignoring no-through on stairs
  }

  @Override
  public void setWalkNoThruTraffic(boolean walkNoThrough) {
    // we are ignoring no-through on stairs
  }

  @Override
  public int getOutAngle() {
    return outAngle;
  }

  @Override
  public int getInAngle() {
    return inAngle;
  }

  @Override
  public void addTurnRestriction(TurnRestriction restriction) {
    // we are ignoring turn restrictions on stairs
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

  private boolean blockedByBarriers() {
    var permission = BarrierCalculator.reducePermissions(DEFAULT_PERMISSIONS, fromv, tov);
    return permission.allowsNothing();
  }

  private double getEffectiveWalkSafetyDistance() {
    return walkSafetyFactor * getDistanceMeters();
  }
}
