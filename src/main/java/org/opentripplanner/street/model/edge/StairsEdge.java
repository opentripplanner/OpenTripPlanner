package org.opentripplanner.street.model.edge;

import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.CompactLineStringUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.state.State;

/**
 * Represents a flight of stairs that are derived from OSM data.
 */
public class StairsEdge extends Edge {

  // stairs are only allowed for walking and cycling/scootering
  private static final StreetTraversalPermission DEFAULT_PERMISSIONS =
    StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
  private static final LocalizedString NAME = new LocalizedString("name.stairs");

  private final LineString compactGeometry;
  private final double distance;

  public StairsEdge(StreetVertex from, StreetVertex to, LineString geometry, double distance) {
    super(from, to);
    this.compactGeometry =geometry;
    this.distance = distance;
  }

  @Override
  @Nonnull
  public State[] traverse(State s0) {
    if (blockedByBarriers() || s0.currentMode().isInCar()) {
      return State.empty();
    }

    var prefs = s0.getPreferences();
    double speed =
      switch (s0.currentMode()) {
        case WALK -> prefs.walk().speed();
        case BICYCLE, SCOOTER -> prefs.bike().walkingSpeed();
        case CAR, FLEX -> throw new IllegalStateException();
      };
    // slow down by stairsTimeFactor
    speed = speed / prefs.walk().stairsTimeFactor();

    double time = distance / speed;
    double weight =
      getDistanceMeters() *
      prefs.walk().safetyFactor() +
      getDistanceMeters() *
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
    return NAME;
  }

  @Override
  public LineString getGeometry() {
      return compactGeometry;
  }

  @Override
  public double getDistanceMeters() {
    return distance;
  }

  private static double reluctance(State s0, RoutingPreferences prefs) {
    if (s0.getRequest().wheelchair()) {
      return StreetEdgeReluctanceCalculator.computeWheelchairReluctance(prefs, 0, true, true);
    } else {
      return StreetEdgeReluctanceCalculator.computeReluctance(prefs, s0.currentMode(), false, true);
    }
  }

  private boolean blockedByBarriers() {
    var permission = BarrierCalculator.reducePermissions(DEFAULT_PERMISSIONS, fromv, tov);
    return permission.allowsNothing();
  }
}
