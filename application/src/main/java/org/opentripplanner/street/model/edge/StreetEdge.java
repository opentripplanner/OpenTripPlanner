package org.opentripplanner.street.model.edge;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.framework.geometry.CompactLineStringUtils;
import org.opentripplanner.framework.geometry.DirectionUtils;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.SplitLineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.TurnRestrictionType;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.street.search.state.VehicleRentalState;
import org.opentripplanner.utils.lang.BitSetUtils;
import org.opentripplanner.utils.lang.IntUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This represents a street segment.
 *
 * @author novalis
 */
public class StreetEdge
  extends Edge
  implements BikeWalkableEdge, Cloneable, CarPickupableEdge, WheelchairTraversalInformation {

  private static final Logger LOG = LoggerFactory.getLogger(StreetEdge.class);

  private static final double SAFEST_STREETS_SAFETY_FACTOR = 0.1;

  /** If you have more than 16 flags, increase flags to short or int */
  static final int BACK_FLAG_INDEX = 0;
  static final int ROUNDABOUT_FLAG_INDEX = 1;
  /**
   * @see Edge#nameIsDerived()
   */
  static final int NAME_IS_DERIVED_FLAG_INDEX = 2;
  static final int MOTOR_VEHICLE_NOTHRUTRAFFIC = 3;
  static final int STAIRS_FLAG_INDEX = 4;
  static final int SLOPEOVERRIDE_FLAG_INDEX = 5;
  static final int WHEELCHAIR_ACCESSIBLE_FLAG_INDEX = 6;
  static final int BICYCLE_NOTHRUTRAFFIC = 7;
  static final int WALK_NOTHRUTRAFFIC = 8;
  static final int CLASS_LINK = 9;

  private StreetEdgeCostExtension costExtension;

  /** back, roundabout, stairs, ... */
  private short flags;

  /**
   * Length is stored internally as 32-bit fixed-point (millimeters). This allows edges of up to
   * ~2100km. Distances used in calculations and exposed outside this class are still in
   * double-precision floating point meters. Someday we might want to convert everything to fixed
   * point representations.
   */
  private final int length_mm;

  /**
   * bicycleSafetyWeight = length * bicycleSafetyFactor. For example, a 100m street with a safety
   * factor of 2.0 will be considered in terms of safety cost as the same as a 200m street with a
   * safety factor of 1.0.
   */
  private float bicycleSafetyFactor;

  /**
   * walkSafetyFactor = length * walkSafetyFactor. For example, a 100m street with a safety
   * factor of 2.0 will be considered in terms of safety cost as the same as a 200m street with a
   * safety factor of 1.0.
   */
  private float walkSafetyFactor;

  private byte[] compactGeometry;

  private I18NString name;

  private StreetTraversalPermission permission;

  /**
   * The speed (meters / sec) at which an automobile can traverse this street segment.
   */
  private final float carSpeed;

  /**
   * The angle at the start of the edge geometry. Internal representation is -180 to +179 integer
   * degrees mapped to -128 to +127 (brads)
   */
  private final byte inAngle;

  /** The angle at the start of the edge geometry. Internal representation like that of inAngle. */
  private final byte outAngle;

  private StreetElevationExtension elevationExtension;

  /**
   * The set of turn restrictions of this edge. Since most instances don't have any, we reuse a
   * global instance in order to conserve memory.
   * <p>
   * This field is optimized for low memory consumption and fast access, but modification is
   * synchronized since it can happen concurrently.
   * <p>
   * Why not use null to represent no turn restrictions? This would mean that the access would also
   * need to be synchronized but since that is a very hot code path, it needs to be fast.
   * <p>
   * Why not use a concurrent collection? That would mean that every StreetEdge has its own empty
   * instance which would increase memory significantly.
   * <p>
   * We use specifically an EmptyList here, in order to get very fast iteration, since it has a
   * static iterator instance, which always returns false in hasNext
   */
  private List<TurnRestriction> turnRestrictions = Collections.emptyList();

  protected StreetEdge(StreetEdgeBuilder<?> builder) {
    super(builder.fromVertex(), builder.toVertex());
    this.flags = builder.getFlags();
    this.setGeometry(builder.geometry());
    this.length_mm = computeLength(builder);
    this.setBicycleSafetyFactor(builder.bicycleSafetyFactor());
    this.setWalkSafetyFactor(builder.walkSafetyFactor());
    this.name = builder.name();
    this.setPermission(builder.permission());
    this.carSpeed = builder.carSpeed();
    LineStringInOutAngles lineStringInOutAngles = LineStringInOutAngles.of(builder.geometry());
    inAngle = lineStringInOutAngles.inAngle();
    outAngle = lineStringInOutAngles.outAngle();
    elevationExtension = builder.streetElevationExtension();
  }

  public StreetEdgeBuilder<?> toBuilder() {
    return new StreetEdgeBuilder<>(this);
  }

  /**
   * Checks permissions of the street edge if specified modes are allowed to travel.
   * <p>
   * Barriers aren't taken into account. So it can happen that canTraverse returns True. But
   * doTraverse returns false. Since there are barriers on a street.
   * <p>
   * This is because this function is used also on street when searching for start/stop. Those
   * streets are then split. On splitted streets can be possible to drive with a CAR because it is
   * only blocked from one way.
   */
  public boolean canTraverse(TraverseModeSet modes) {
    return getPermission().allows(modes);
  }

  /**
   * This checks if start or end vertex is bollard If it is it creates intersection of street edge
   * permissions and from/to barriers. Then it checks if mode is allowed to traverse the edge.
   * <p>
   * By default CAR isn't allowed to traverse barrier but foot and bicycle are. This can be changed
   * with different tags
   * <p>
   * If start/end isn't bollard it just checks the street permissions.
   * <p>
   * It is used in {@link #canTraverse(TraverseMode)}
   */
  public boolean canTraverse(TraverseMode mode) {
    StreetTraversalPermission permission = getPermission();
    if (fromv instanceof BarrierVertex) {
      permission = permission.intersection(((BarrierVertex) fromv).getBarrierPermissions());
    }
    if (tov instanceof BarrierVertex) {
      permission = permission.intersection(((BarrierVertex) tov).getBarrierPermissions());
    }

    return permission.allows(mode);
  }

  public void setElevationExtension(StreetElevationExtension streetElevationExtension) {
    this.elevationExtension = streetElevationExtension;
  }

  public boolean hasElevationExtension() {
    return elevationExtension != null;
  }

  public PackedCoordinateSequence getElevationProfile() {
    return hasElevationExtension() ? elevationExtension.getElevationProfile() : null;
  }

  public boolean isElevationFlattened() {
    return hasElevationExtension() && elevationExtension.isFlattened();
  }

  public double getMaxSlope() {
    return hasElevationExtension() ? elevationExtension.getMaxSlope() : 0.0d;
  }

  public boolean isNoThruTraffic(TraverseMode traverseMode) {
    return switch (traverseMode) {
      case WALK -> isWalkNoThruTraffic();
      case BICYCLE, SCOOTER -> isBicycleNoThruTraffic();
      case CAR, FLEX -> isMotorVehicleNoThruTraffic();
    };
  }

  /**
   * Calculate the speed appropriately given the RouteRequest and traverseMode.
   */
  public double calculateSpeed(
    RoutingPreferences preferences,
    TraverseMode traverseMode,
    boolean walkingBike
  ) {
    if (traverseMode == null) {
      return Double.NaN;
    }

    final double speed =
      switch (traverseMode) {
        case WALK -> walkingBike
          ? preferences.bike().walking().speed()
          : preferences.walk().speed();
        case BICYCLE -> preferences.bike().speed();
        case CAR -> getCarSpeed();
        case SCOOTER -> preferences.scooter().speed();
        case FLEX -> throw new IllegalArgumentException("getSpeed(): Invalid mode " + traverseMode);
      };

    return isStairs() ? (speed / preferences.walk().stairsTimeFactor()) : speed;
  }

  /**
   * This gets the effective length for bikes and wheelchairs, taking slopes into account. This can
   * be divided by the speed on a flat surface to get the duration.
   */
  public double getEffectiveBikeDistance() {
    return hasElevationExtension()
      ? elevationExtension.getEffectiveBikeDistance()
      : getDistanceMeters();
  }

  /**
   * This gets the effective work amount for bikes, taking the effort required to traverse the
   * slopes into account.
   */
  public double getEffectiveBikeDistanceForWorkCost() {
    return hasElevationExtension()
      ? elevationExtension.getEffectiveBikeDistanceForWorkCost()
      : getDistanceMeters();
  }

  public float getBicycleSafetyFactor() {
    return bicycleSafetyFactor;
  }

  public void setBicycleSafetyFactor(float bicycleSafetyFactor) {
    if (hasElevationExtension()) {
      throw new IllegalStateException(
        "A bicycle safety factor may not be set if an elevation extension is set."
      );
    }
    if (!Float.isFinite(bicycleSafetyFactor) || bicycleSafetyFactor <= 0) {
      throw new IllegalArgumentException("Invalid bicycleSafetyFactor: " + bicycleSafetyFactor);
    }
    this.bicycleSafetyFactor = bicycleSafetyFactor;
  }

  public double getEffectiveBicycleSafetyDistance() {
    return elevationExtension != null
      ? elevationExtension.getEffectiveBicycleSafetyDistance()
      : bicycleSafetyFactor * getDistanceMeters();
  }

  public float getWalkSafetyFactor() {
    return walkSafetyFactor;
  }

  public void setWalkSafetyFactor(float walkSafetyFactor) {
    if (hasElevationExtension()) {
      throw new IllegalStateException(
        "A walk safety factor may not be set if an elevation extension is set."
      );
    }
    if (!Float.isFinite(walkSafetyFactor) || walkSafetyFactor <= 0) {
      throw new IllegalArgumentException("Invalid walkSafetyFactor: " + walkSafetyFactor);
    }
    this.walkSafetyFactor = walkSafetyFactor;
  }

  public double getEffectiveWalkSafetyDistance() {
    return elevationExtension != null
      ? elevationExtension.getEffectiveWalkSafetyDistance()
      : walkSafetyFactor * getDistanceMeters();
  }

  public String toString() {
    var nameString = name != null ? name.toString() : null;
    return buildToString(nameString, b ->
      b
        .append(", length=")
        .append(this.getDistanceMeters())
        .append(", carSpeed=")
        .append(this.getCarSpeed())
        .append(", permission=")
        .append(this.getPermission())
    );
  }

  public boolean isRoundabout() {
    return BitSetUtils.get(flags, ROUNDABOUT_FLAG_INDEX);
  }

  @Override
  public State[] traverse(State s0) {
    final StateEditor editor;

    final boolean arriveByRental =
      s0.getRequest().mode().includesRenting() && s0.getRequest().arriveBy();
    if (arriveByRental && tov.rentalTraversalBanned(s0)) {
      return State.empty();
    } else if (arriveByRental && hasStartedWalkingInNoDropOffZoneAndIsExitingIt(s0)) {
      return splitStatesAfterHavingExitedNoDropOffZoneWhenReverseSearching(s0);
    }
    // if the traversal is banned for the current state because of a GBFS geofencing zone
    // we drop the vehicle and continue walking
    else if (s0.getRequest().mode().includesRenting() && tov.rentalTraversalBanned(s0)) {
      editor = doTraverse(s0, TraverseMode.WALK, false);
      if (editor != null) {
        editor.dropFloatingVehicle(
          s0.vehicleRentalFormFactor(),
          s0.getVehicleRentalNetwork(),
          s0.getRequest().arriveBy()
        );
      }
      // when we start the reverse search of a rental request there are three cases when we need
      // to stop walking and pick up a vehicle:
      //  - crossing the border of a business zone
      //  - leaving a no-drop-off zone
      //  - leaving a no-traversal zone
      // remember that this is a reverse search so calling dropFloatingVehicle actually transitions
      // from walking to using the vehicle.
    } else if (arriveByRental && leavesZoneWithRentalRestrictionsWhenHavingRented(s0)) {
      editor = doTraverse(s0, TraverseMode.WALK, false);
      if (editor != null) {
        editor.dropFloatingVehicle(
          s0.vehicleRentalFormFactor(),
          s0.getVehicleRentalNetwork(),
          s0.getRequest().arriveBy()
        );
      }
    }
    // If we are biking, or walking with a bike check if we may continue by biking or by walking
    else if (s0.currentMode() == TraverseMode.BICYCLE) {
      if (canTraverse(TraverseMode.BICYCLE)) {
        editor = doTraverse(s0, TraverseMode.BICYCLE, false);
      } else if (canTraverse(TraverseMode.WALK)) {
        editor = doTraverse(s0, TraverseMode.WALK, true);
      } else {
        return State.empty();
      }
    } else if (canTraverse(s0.currentMode())) {
      editor = doTraverse(s0, s0.currentMode(), false);
    } else {
      editor = null;
    }

    State state = editor != null ? editor.makeState() : null;

    // we are transitioning into a no-drop-off zone therefore we add a second state for dropping
    // off the vehicle and walking
    if (state != null && !fromv.rentalDropOffBanned(s0) && tov.rentalDropOffBanned(s0)) {
      StateEditor afterTraversal = doTraverse(s0, TraverseMode.WALK, false);
      if (afterTraversal != null) {
        afterTraversal.dropFloatingVehicle(
          state.vehicleRentalFormFactor(),
          state.getVehicleRentalNetwork(),
          state.getRequest().arriveBy()
        );
        afterTraversal.leaveNoRentalDropOffArea();
        var forkState = afterTraversal.makeState();
        return State.ofNullable(forkState, state);
      }
    }

    // when we leave a geofencing zone in reverse search we want to speculatively pick up a rental
    // vehicle, however, we _also_ want to keep on walking in case the renting state doesn't lead
    // anywhere due to these cases:
    //  - no rental vehicle available
    //  - not being able to continue renting due to traversal restrictions or geofencing zones
    if (state != null && arriveByRental && leavesZoneWithRentalRestrictionsWhenHavingRented(s0)) {
      StateEditor walking = doTraverse(s0, TraverseMode.WALK, false);
      var forkState = walking.makeState();
      return State.ofNullable(forkState, state);
    }

    if (canPickupAndDrive(s0) && canTraverse(TraverseMode.CAR)) {
      StateEditor inCar = doTraverse(s0, TraverseMode.CAR, false);
      if (inCar != null) {
        driveAfterPickup(s0, inCar);
        State forkState = inCar.makeState();
        // Return both the original WALK state, along with the new IN_CAR state
        return State.ofNullable(forkState, state);
      }
    }

    if (
      canDropOffAfterDriving(s0) &&
      !getPermission().allows(TraverseMode.CAR) &&
      canTraverse(TraverseMode.WALK)
    ) {
      StateEditor dropOff = doTraverse(s0, TraverseMode.WALK, false);
      if (dropOff != null) {
        dropOffAfterDriving(s0, dropOff);
        // Only the walk state is returned, since traversing by car was not possible
        return dropOff.makeStateArray();
      }
    }

    return State.ofNullable(state);
  }

  /**
   * Gets non-localized I18NString (Used when splitting edges)
   *
   * @return non-localized Name
   */
  public I18NString getName() {
    return this.name;
  }

  /**
   * Update the name of the edge after it has been constructed. This method also sets the nameIsDerived
   * property to false, indicating to the code that maps from edges to steps that this is a real
   * street name.
   * @see Edge#nameIsDerived()
   */
  public void setName(I18NString name) {
    this.name = name;
    this.flags = BitSetUtils.set(flags, NAME_IS_DERIVED_FLAG_INDEX, false);
  }

  @Override
  public boolean nameIsDerived() {
    return BitSetUtils.get(flags, NAME_IS_DERIVED_FLAG_INDEX);
  }

  @Override
  public LineString getGeometry() {
    return CompactLineStringUtils.uncompactLineString(
      fromv.getLon(),
      fromv.getLat(),
      tov.getLon(),
      tov.getLat(),
      compactGeometry,
      isBack()
    );
  }

  @Override
  public double getDistanceMeters() {
    return length_mm / 1000.0;
  }

  @Override
  public double getEffectiveWalkDistance() {
    return hasElevationExtension()
      ? elevationExtension.getEffectiveWalkDistance()
      : getDistanceMeters();
  }

  /**
   * This method is not thread-safe.
   */
  public void removeRentalExtension(RentalRestrictionExtension ext) {
    fromv.removeRentalRestriction(ext);
    tov.removeRentalRestriction(ext);
  }

  @Override
  public StreetEdge clone() {
    try {
      return (StreetEdge) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean canTurnOnto(Edge e, State state, TraverseMode mode) {
    for (TurnRestriction turnRestriction : turnRestrictions) {
      /* FIXME: This is wrong for trips that end in the middle of turnRestriction.to
       */

      // NOTE(flamholz): edge to be traversed decides equivalence. This is important since
      // it might be a temporary edge that is equivalent to some graph edge.
      if (turnRestriction.type == TurnRestrictionType.ONLY_TURN) {
        if (
          !e.isEquivalentTo(turnRestriction.to) &&
          turnRestriction.modes.contains(mode) &&
          turnRestriction.active(state.getTimeSeconds())
        ) {
          return false;
        }
      } else {
        if (
          e.isEquivalentTo(turnRestriction.to) &&
          turnRestriction.modes.contains(mode) &&
          turnRestriction.active(state.getTimeSeconds())
        ) {
          return false;
        }
      }
    }
    return true;
  }

  public void shareData(StreetEdge reversedEdge) {
    if (Arrays.equals(compactGeometry, reversedEdge.compactGeometry)) {
      compactGeometry = reversedEdge.compactGeometry;
    } else {
      LOG.warn("Can't share geometry between {} and {}", this, reversedEdge);
    }
  }

  @Override
  public boolean isWheelchairAccessible() {
    return BitSetUtils.get(flags, WHEELCHAIR_ACCESSIBLE_FLAG_INDEX);
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  public void setPermission(StreetTraversalPermission permission) {
    this.permission = Objects.requireNonNull(permission);
  }

  /**
   * Marks that this edge is the reverse of the one defined in the source data. Does NOT mean
   * fromv/tov are reversed.
   */
  public boolean isBack() {
    return BitSetUtils.get(flags, BACK_FLAG_INDEX);
  }

  public boolean isWalkNoThruTraffic() {
    return BitSetUtils.get(flags, WALK_NOTHRUTRAFFIC);
  }

  public void setWalkNoThruTraffic(boolean noThruTraffic) {
    flags = BitSetUtils.set(flags, WALK_NOTHRUTRAFFIC, noThruTraffic);
  }

  public boolean isMotorVehicleNoThruTraffic() {
    return BitSetUtils.get(flags, MOTOR_VEHICLE_NOTHRUTRAFFIC);
  }

  public void setMotorVehicleNoThruTraffic(boolean noThruTraffic) {
    flags = BitSetUtils.set(flags, MOTOR_VEHICLE_NOTHRUTRAFFIC, noThruTraffic);
  }

  public boolean isBicycleNoThruTraffic() {
    return BitSetUtils.get(flags, BICYCLE_NOTHRUTRAFFIC);
  }

  public void setBicycleNoThruTraffic(boolean noThruTraffic) {
    flags = BitSetUtils.set(flags, BICYCLE_NOTHRUTRAFFIC, noThruTraffic);
  }

  /**
   * This street is a staircase
   */
  public boolean isStairs() {
    return BitSetUtils.get(flags, STAIRS_FLAG_INDEX);
  }

  /**
   * The edge is part of an osm way, which is of type link
   */
  public boolean isLink() {
    return BitSetUtils.get(flags, CLASS_LINK);
  }

  public float getCarSpeed() {
    return carSpeed;
  }

  public boolean isSlopeOverride() {
    return BitSetUtils.get(flags, SLOPEOVERRIDE_FLAG_INDEX);
  }

  /**
   * Return the azimuth of the first segment in this edge in integer degrees clockwise from South.
   * TODO change everything to clockwise from North
   */
  public int getInAngle() {
    return IntUtils.round((this.inAngle * 180) / 128.0);
  }

  /** Return the azimuth of the last segment in this edge in integer degrees clockwise from South. */
  public int getOutAngle() {
    return IntUtils.round((this.outAngle * 180) / 128.0);
  }

  public void setCostExtension(StreetEdgeCostExtension costExtension) {
    this.costExtension = costExtension;
  }

  /**
   * This method is not thread-safe!
   */
  public void addRentalRestriction(RentalRestrictionExtension ext) {
    fromv.addRentalRestriction(ext);
  }

  /**
   * Split this street edge and return the resulting street edges. After splitting, the original
   * edge will be removed from the graph.
   */
  public SplitStreetEdge splitDestructively(SplitterVertex v) {
    SplitLineString geoms = GeometryUtils.splitGeometryAtPoint(getGeometry(), v.getCoordinate());

    StreetEdgeBuilder<?> seb1 = new StreetEdgeBuilder<>()
      .withFromVertex((StreetVertex) fromv)
      .withToVertex(v)
      .withGeometry(geoms.beginning())
      .withName(name)
      .withPermission(permission)
      .withBack(isBack());

    StreetEdgeBuilder<?> seb2 = new StreetEdgeBuilder<>()
      .withFromVertex(v)
      .withToVertex((StreetVertex) tov)
      .withGeometry(geoms.ending())
      .withName(name)
      .withPermission(permission)
      .withBack(isBack());

    // we have this code implemented in both directions, because splits are fudged half a millimeter
    // when the length of this is odd. We want to make sure the lengths of the split streets end up
    // exactly the same as their backStreets so that if they are split again the error does not accumulate
    // and so that the order in which they are split does not matter.
    int l1 = defaultMillimeterLength(geoms.beginning());
    int l2 = defaultMillimeterLength(geoms.ending());
    if (!isBack()) {
      // cast before the divide so that the sum is promoted
      double frac = (double) l1 / (l1 + l2);
      l1 = (int) (length_mm * frac);
      l2 = length_mm - l1;
    } else {
      // cast before the divide so that the sum is promoted
      double frac = (double) l2 / (l1 + l2);
      l2 = (int) (length_mm * frac);
      l1 = length_mm - l2;
    }

    // TODO: better handle this temporary fix to handle bad edge distance calculation
    if (l1 <= 0) {
      LOG.error(
        "Edge 1 ({}) split at vertex at {},{} has length {} mm. Setting to 1 mm.",
        name,
        v.getLat(),
        v.getLon(),
        l1
      );
      l1 = 1;
    }
    if (l2 <= 0) {
      LOG.error(
        "Edge 2 ({}) split at vertex at {},{}  has length {} mm. Setting to 1 mm.",
        name,
        v.getLat(),
        v.getLon(),
        l2
      );
      l2 = 1;
    }

    seb1.withMilliMeterLength(l1);
    seb2.withMilliMeterLength(l2);

    copyPropertiesToSplitEdge(seb1, 0, l1 / 1000.0);
    copyPropertiesToSplitEdge(seb2, l1 / 1000.0, getDistanceMeters());

    StreetEdge se1 = seb1.buildAndConnect();
    StreetEdge se2 = seb2.buildAndConnect();

    copyRentalRestrictionsToSplitEdge(se1);
    copyRentalRestrictionsToSplitEdge(se2);

    var splitEdges = new SplitStreetEdge(se1, se2);
    copyRestrictionsToSplitEdges(this, splitEdges);
    return splitEdges;
  }

  /** Split this street edge and return the resulting street edges. The original edge is kept. */
  public SplitStreetEdge splitNonDestructively(
    SplitterVertex v,
    DisposableEdgeCollection tempEdges,
    LinkingDirection direction
  ) {
    SplitLineString geoms = GeometryUtils.splitGeometryAtPoint(getGeometry(), v.getCoordinate());

    StreetEdge e1 = null;
    StreetEdge e2 = null;

    if (direction == LinkingDirection.OUTGOING || direction == LinkingDirection.BIDIRECTIONAL) {
      var seb1 = new TemporaryPartialStreetEdgeBuilder()
        .withParentEdge(this)
        .withFromVertex((StreetVertex) fromv)
        .withToVertex(v)
        .withGeometry(geoms.beginning())
        .withName(name)
        .withBack(isBack());
      copyPropertiesToSplitEdge(seb1, 0, defaultMillimeterLength(geoms.beginning()) / 1000.0);
      e1 = seb1.buildAndConnect();
      copyRentalRestrictionsToSplitEdge(e1);
      tempEdges.addEdge(e1);
    }
    if (direction == LinkingDirection.INCOMING || direction == LinkingDirection.BIDIRECTIONAL) {
      var seb2 = new TemporaryPartialStreetEdgeBuilder()
        .withParentEdge(this)
        .withFromVertex(v)
        .withToVertex((StreetVertex) tov)
        .withGeometry(geoms.ending())
        .withName(name)
        .withBack(isBack());
      copyPropertiesToSplitEdge(
        seb2,
        getDistanceMeters() - defaultMillimeterLength(geoms.ending()) / 1000.0,
        getDistanceMeters()
      );
      e2 = seb2.buildAndConnect();
      copyRentalRestrictionsToSplitEdge(e2);
      tempEdges.addEdge(e2);
    }

    var splitEdges = new SplitStreetEdge(e1, e2);
    copyRestrictionsToSplitEdges(this, splitEdges);
    return splitEdges;
  }

  public Optional<Edge> createPartialEdge(StreetVertex from, StreetVertex to) {
    LineString parent = getGeometry();
    LineString head = GeometryUtils.getInteriorSegment(
      parent,
      getFromVertex().getCoordinate(),
      from.getCoordinate()
    );
    LineString tail = GeometryUtils.getInteriorSegment(
      parent,
      to.getCoordinate(),
      getToVertex().getCoordinate()
    );

    if (parent.getLength() > head.getLength() + tail.getLength()) {
      LineString partial = GeometryUtils.getInteriorSegment(
        parent,
        from.getCoordinate(),
        to.getCoordinate()
      );

      double startRatio = head.getLength() / parent.getLength();
      double start = getDistanceMeters() * startRatio;
      double lengthRatio = partial.getLength() / parent.getLength();
      double length = getDistanceMeters() * lengthRatio;

      var tpseb = new TemporaryPartialStreetEdgeBuilder()
        .withParentEdge(this)
        .withFromVertex(from)
        .withToVertex(to)
        .withGeometry(partial)
        .withName(getName())
        .withMeterLength(length);
      copyPropertiesToSplitEdge(tpseb, start, start + length);
      TemporaryPartialStreetEdge se = tpseb.buildAndConnect();
      copyRentalRestrictionsToSplitEdge(se);
      return Optional.of(se);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Add a {@link TurnRestriction} to this edge.
   * <p>
   * This method is thread-safe as modifying the underlying set is synchronized.
   */
  public void addTurnRestriction(TurnRestriction turnRestriction) {
    if (turnRestriction == null) {
      return;
    }
    synchronized (this) {
      // in order to guarantee fast access without extra allocations
      // we make the turn restrictions unmodifiable after a copy-on-write modification
      var temp = new HashSet<>(turnRestrictions);
      temp.add(turnRestriction);
      turnRestrictions = List.copyOf(temp);
    }
  }

  /**
   * Remove a {@link TurnRestriction} from this edge.
   * <p>
   * This method is thread-safe as modifying the underlying set is synchronized.
   */
  public void removeTurnRestriction(TurnRestriction turnRestriction) {
    if (turnRestriction == null) {
      return;
    }
    synchronized (this) {
      if (turnRestrictions.contains(turnRestriction)) {
        if (turnRestrictions.size() == 1) {
          turnRestrictions = Collections.emptyList();
        } else {
          // in order to guarantee fast access without extra allocations
          // we make the turn restrictions unmodifiable after a copy-on-write modification
          var withRemoved = new HashSet<>(turnRestrictions);
          withRemoved.remove(turnRestriction);
          turnRestrictions = List.copyOf(withRemoved);
        }
      }
    }
  }

  public void removeAllTurnRestrictions() {
    if (turnRestrictions == null) {
      return;
    }
    synchronized (this) {
      turnRestrictions = Collections.emptyList();
    }
  }

  @Override
  public void removeTurnRestrictionsTo(Edge other) {
    for (TurnRestriction turnRestriction : this.getTurnRestrictions()) {
      if (turnRestriction.to == other) {
        this.removeTurnRestriction(turnRestriction);
      }
    }
  }

  /**
   * Get the immutable {@link List} of {@link TurnRestriction}s that belongs to this
   * {@link StreetEdge}.
   * <p>
   * This method is thread-safe, even if {@link StreetEdge#addTurnRestriction} or
   * {@link StreetEdge#removeTurnRestriction} is called concurrently.
   */
  public List<TurnRestriction> getTurnRestrictions() {
    // this can be safely returned as it's unmodifiable
    return turnRestrictions;
  }

  @Override
  public void remove() {
    removeAllTurnRestrictions();

    super.remove();
  }

  /**
   * Copy inherited properties from a parent edge to a split edge.
   */
  protected void copyPropertiesToSplitEdge(
    StreetEdgeBuilder<?> seb,
    double fromDistance,
    double toDistance
  ) {
    seb.withFlags(flags);
    seb.withBicycleSafetyFactor(bicycleSafetyFactor);
    seb.withWalkSafetyFactor(walkSafetyFactor);
    seb.withCarSpeed(carSpeed);

    var partialElevationProfileFromParent = ElevationUtils.getPartialElevationProfile(
      getElevationProfile(),
      fromDistance,
      toDistance
    );

    StreetElevationExtensionBuilder.of(seb)
      .withDistanceInMeters(defaultMillimeterLength(seb.geometry()) / 1000.)
      .withElevationProfile(partialElevationProfileFromParent)
      .build()
      .ifPresent(seb::withElevationExtension);
  }

  /**
   * Copy inherited rental restrictions from a parent edge to a split edge
   */
  protected void copyRentalRestrictionsToSplitEdge(StreetEdge splitEdge) {
    splitEdge.addRentalRestriction(fromv.rentalRestrictions());
  }

  short getFlags() {
    return flags;
  }

  int getMillimeterLength() {
    return length_mm;
  }

  /**
   * Copy restrictions having former edge as from to appropriate split edge, as well as restrictions
   * on incoming edges.
   */
  private static void copyRestrictionsToSplitEdges(StreetEdge edge, SplitStreetEdge splitEdges) {
    // Copy turn restriction which have a .to of this edge (present on the incoming edges of fromv)
    if (splitEdges.head() != null) {
      edge
        .getFromVertex()
        .getIncoming()
        .stream()
        .filter(StreetEdge.class::isInstance)
        .map(StreetEdge.class::cast)
        .flatMap(originatingEdge -> originatingEdge.getTurnRestrictions().stream())
        .filter(restriction -> restriction.to == edge)
        .forEach(restriction ->
          applyRestrictionsToNewEdge(restriction.from, splitEdges.head(), restriction)
        );
    }

    // Copy turn restriction which have a .from of this edge (present on the original street edge)
    if (splitEdges.tail() != null) {
      edge
        .getTurnRestrictions()
        .forEach(existingTurnRestriction ->
          applyRestrictionsToNewEdge(
            splitEdges.tail(),
            existingTurnRestriction.to,
            existingTurnRestriction
          )
        );
    }
  }

  private static void applyRestrictionsToNewEdge(
    StreetEdge fromEdge,
    StreetEdge toEdge,
    TurnRestriction restriction
  ) {
    TurnRestriction splitTurnRestriction = new TurnRestriction(
      fromEdge,
      toEdge,
      restriction.type,
      restriction.modes,
      restriction.time
    );
    LOG.debug("Created new restriction for split edges: {}", splitTurnRestriction);
    fromEdge.addTurnRestriction(splitTurnRestriction);
  }

  private int computeLength(StreetEdgeBuilder<?> builder) {
    int lengthInMillimeter = builder.hasDefaultLength()
      ? defaultMillimeterLength(builder.geometry())
      : builder.millimeterLength();
    if (lengthInMillimeter == 0) {
      LOG.warn(
        "StreetEdge {} from {} to {} has length of 0. This is usually an error.",
        name,
        builder.fromVertex(),
        builder.toVertex()
      );
    }
    return lengthInMillimeter;
  }

  static int defaultMillimeterLength(LineString geometry) {
    return (int) (SphericalDistanceLibrary.length(geometry) * 1000);
  }

  /**
   * Helper method for {@link #splitStatesAfterHavingExitedNoDropOffZoneWhenReverseSearching}.
   * Create a single new state, exiting a no-drop-off zone, in reverse, and continuing
   * on a rental vehicle in the known network, or an unknown network if network is null,
   * unless the known network is not accepted by the provided {@link RoutingPreferences}.
   * @param s0 The parent state (i.e. the following state, as we are in reverse)
   * @param network Network id, or null if unknown
   * @param preferences Active {@link RoutingPreferences}
   * @return Newly generated {@link State}, or null if the state would have been forbidden.
   */
  private State createStateAfterHavingExitedNoDropOffZoneWhenReverseSearching(
    State s0,
    String network,
    RoutingPreferences preferences
  ) {
    var edit = doTraverse(s0, TraverseMode.WALK, false);
    if (edit != null) {
      edit.dropFloatingVehicle(s0.vehicleRentalFormFactor(), network, s0.getRequest().arriveBy());
      if (network != null) {
        edit.resetStartedInNoDropOffZone();
      }
      State state = edit.makeState();
      if (state != null && network != null) {
        var rentalPreferences = preferences.rental(state.currentMode());
        var allowedNetworks = rentalPreferences.allowedNetworks();
        var bannedNetworks = rentalPreferences.bannedNetworks();
        if (allowedNetworks.isEmpty()) {
          if (bannedNetworks.contains(network)) {
            return null;
          }
        } else {
          if (!allowedNetworks.contains(network)) {
            return null;
          }
        }
      }
      return state;
    }
    return null;
  }

  /**
   * A very special case: an arriveBy rental search has started in a no-drop-off zone
   * we don't know yet which rental network we will end up using.
   * <p>
   * So we speculatively assume that we can rent any by setting the network in the state data
   * to null.
   * <p>
   * When we then leave the no drop off zone on foot we generate a state for each network that the
   * zone applies to where we pick up a vehicle with a specific network.
   */
  private State[] splitStatesAfterHavingExitedNoDropOffZoneWhenReverseSearching(State s0) {
    var preferences = s0.getRequest().preferences();
    var states = new ArrayList<State>();

    // Also include a state which continues walking, because the vehicle rental states are
    // speculation. It is possible that the rental states don't end up at the target at all
    // due to mode limitations or not finding a place to pick up the rental vehicle, or that
    // the rental possibility is simply more expensive than walking.
    StateEditor walking = doTraverse(s0, TraverseMode.WALK, false);
    if (walking != null) {
      states.add(walking.makeState());
    }

    boolean hasNetworkStates = false;
    for (var network : tov.rentalRestrictions().noDropOffNetworks()) {
      var state = createStateAfterHavingExitedNoDropOffZoneWhenReverseSearching(
        s0,
        network,
        preferences
      );
      if (state != null) {
        states.add(state);
        hasNetworkStates = true;
      }
    }
    if (hasNetworkStates) {
      // null is a special rental network that speculatively assumes that you can take any vehicle
      // you have to check in the rental edge if this has search has been started in a no-drop off zone
      states.add(
        createStateAfterHavingExitedNoDropOffZoneWhenReverseSearching(s0, null, preferences)
      );
    }
    return states.toArray(State[]::new);
  }

  /**
   * This is the state that starts a backwards search inside a restricted zone
   * (no drop off, no traversal or outside business area) and is walking towards finding a rental
   * vehicle. Once we are leaving a geofencing zone or are entering a business area we want to
   * speculatively pick up a vehicle a ride towards an edge where there is one parked.
   */
  private boolean leavesZoneWithRentalRestrictionsWhenHavingRented(State s0) {
    return (
      s0.getVehicleRentalState() == VehicleRentalState.HAVE_RENTED &&
      !fromv.rentalRestrictions().hasRestrictions() &&
      tov.rentalRestrictions().hasRestrictions()
    );
  }

  /**
   * If the reverse search has started in a no-drop off rental zone and you are exiting
   * it .
   */
  private boolean hasStartedWalkingInNoDropOffZoneAndIsExitingIt(State s0) {
    return (
      s0.currentMode() == TraverseMode.WALK &&
      !s0.stateData.noRentalDropOffZonesAtStartOfReverseSearch.isEmpty() &&
      fromv.rentalRestrictions().noDropOffNetworks().isEmpty() &&
      !tov.rentalRestrictions().noDropOffNetworks().isEmpty()
    );
  }

  private void setGeometry(LineString geometry) {
    this.compactGeometry = CompactLineStringUtils.compactLineString(
      fromv.getLon(),
      fromv.getLat(),
      tov.getLon(),
      tov.getLat(),
      isBack() ? geometry.reverse() : geometry,
      isBack()
    );
  }

  private double getDistanceWithElevation() {
    return hasElevationExtension()
      ? elevationExtension.getDistanceWithElevation()
      : getDistanceMeters();
  }

  /**
   * return a StateEditor rather than a State so that we can make parking/mode switch modifications
   * for kiss-and-ride.
   */
  private StateEditor doTraverse(State s0, TraverseMode traverseMode, boolean walkingBike) {
    Edge backEdge = s0.getBackEdge();
    if (backEdge != null) {
      // No illegal U-turns.
      // NOTE(flamholz): we check both directions because both edges get a chance to decide
      // if they are the reverse of the other. Also, because it doesn't matter which direction
      // we are searching in - these traversals are always disallowed (they are U-turns in one direction
      // or the other).
      // TODO profiling indicates that this is a hot spot.
      if (this.isReverseOf(backEdge) || backEdge.isReverseOf(this)) {
        return null;
      }
    }

    var s1 = createEditor(s0, this, traverseMode, walkingBike);

    if (isTraversalBlockedByNoThruTraffic(traverseMode, backEdge, s0, s1)) {
      return null;
    }

    if (s0.getRequest().mode().includesRenting()) {
      if (tov.rentalDropOffBanned(s0)) {
        s1.enterNoRentalDropOffArea();
      } else if (s0.isInsideNoRentalDropOffArea() && !tov.rentalDropOffBanned(s0)) {
        s1.leaveNoRentalDropOffArea();
      }
    }

    final RoutingPreferences preferences = s0.getPreferences();

    // Automobiles have variable speeds depending on the edge type
    double speed = calculateSpeed(preferences, traverseMode, walkingBike);

    var traversalCosts =
      switch (traverseMode) {
        case BICYCLE, SCOOTER -> bicycleOrScooterTraversalCost(preferences, traverseMode, speed);
        case WALK -> walkingTraversalCosts(
          preferences,
          traverseMode,
          speed,
          walkingBike,
          s0.getRequest().wheelchair()
        );
        default -> otherTraversalCosts(preferences, traverseMode, walkingBike, speed);
      };

    long time_ms = (long) Math.ceil(1000.0 * traversalCosts.time());
    var weight = traversalCosts.weight();

    /* Compute turn cost. */
    if (backEdge instanceof StreetEdge backPSE) {
      TraverseMode backMode = s0.getBackMode();
      final boolean arriveBy = s0.getRequest().arriveBy();

      // Apply turn restrictions
      if (
        arriveBy
          ? !canTurnOnto(backPSE, s0, backMode)
          : !backPSE.canTurnOnto(this, s0, traverseMode)
      ) {
        return null;
      }

      double backSpeed = backPSE.calculateSpeed(preferences, backMode, s0.isBackWalkingBike());
      final double turnDuration; // Units are seconds.

      /*
       * This is a subtle piece of code. Turn costs are evaluated differently during
       * forward and reverse traversal. During forward traversal of an edge, the turn
       * *into* that edge is used, while during reverse traversal, the turn *out of*
       * the edge is used.
       *
       * However, over a set of edges, the turn costs must add up the same (for
       * general correctness and specifically for reverse optimization). This means
       * that during reverse traversal, we must also use the speed for the mode of
       * the backEdge, rather than of the current edge.
       */
      if (arriveBy && tov instanceof IntersectionVertex traversedVertex) { // arrive-by search
        turnDuration = s0
          .intersectionTraversalCalculator()
          .computeTraversalDuration(
            traversedVertex,
            this,
            backPSE,
            backMode,
            (float) speed,
            (float) backSpeed
          );
      } else if (!arriveBy && fromv instanceof IntersectionVertex traversedVertex) { // depart-after search
        turnDuration = s0
          .intersectionTraversalCalculator()
          .computeTraversalDuration(
            traversedVertex,
            backPSE,
            this,
            traverseMode,
            (float) backSpeed,
            (float) speed
          );
      } else {
        // In case this is a temporary edge not connected to an IntersectionVertex
        LOG.debug("Not computing turn duration for edge {}", this);
        turnDuration = 0;
      }

      if (!traverseMode.isInCar()) {
        s1.incrementWalkDistance(turnDuration / 100); // just a tie-breaker
      }

      time_ms += (long) Math.ceil(1000.0 * turnDuration);
      weight += preferences.street().turnReluctance() * turnDuration;
    }

    if (!traverseMode.isInCar()) {
      s1.incrementWalkDistance(getDistanceWithElevation());
    }

    if (costExtension != null) {
      weight += costExtension.calculateExtraCost(s0, length_mm, traverseMode);
    }

    s1.incrementTimeInMilliseconds(time_ms);

    s1.incrementWeight(weight);

    return s1;
  }

  private TraversalCosts otherTraversalCosts(
    RoutingPreferences preferences,
    TraverseMode traverseMode,
    boolean walkingBike,
    double speed
  ) {
    var time = getDistanceMeters() / speed;
    var weight =
      time *
      StreetEdgeReluctanceCalculator.computeReluctance(
        preferences,
        traverseMode,
        walkingBike,
        isStairs()
      );
    return new TraversalCosts(time, weight);
  }

  private TraversalCosts bicycleOrScooterTraversalCost(
    RoutingPreferences pref,
    TraverseMode mode,
    double speed
  ) {
    double time = getEffectiveBikeDistance() / speed;
    double weight;
    var optimizeType = mode == TraverseMode.BICYCLE
      ? pref.bike().optimizeType()
      : pref.scooter().optimizeType();
    switch (optimizeType) {
      case SAFEST_STREETS -> {
        weight = (bicycleSafetyFactor * getDistanceMeters()) / speed;
        if (bicycleSafetyFactor <= SAFEST_STREETS_SAFETY_FACTOR) {
          // safest streets are treated as even safer than they really are
          weight *= 0.66;
        }
      }
      case SAFE_STREETS -> weight = getEffectiveBicycleSafetyDistance() / speed;
      case FLAT_STREETS -> /* see notes in StreetVertex on speed overhead */weight =
        getEffectiveBikeDistanceForWorkCost() / speed;
      case SHORTEST_DURATION -> weight = getEffectiveBikeDistance() / speed;
      case TRIANGLE -> {
        double quick = getEffectiveBikeDistance();
        double safety = getEffectiveBicycleSafetyDistance();
        double slope = getEffectiveBikeDistanceForWorkCost();
        var triangle = mode == TraverseMode.BICYCLE
          ? pref.bike().optimizeTriangle()
          : pref.scooter().optimizeTriangle();
        weight = quick * triangle.time() + slope * triangle.slope() + safety * triangle.safety();
        weight /= speed;
      }
      default -> weight = getDistanceMeters() / speed;
    }
    var reluctance = StreetEdgeReluctanceCalculator.computeReluctance(
      pref,
      mode,
      false,
      isStairs()
    );
    weight *= reluctance;
    return new TraversalCosts(time, weight);
  }

  private TraversalCosts walkingTraversalCosts(
    RoutingPreferences preferences,
    TraverseMode traverseMode,
    double speed,
    boolean walkingBike,
    boolean wheelchair
  ) {
    double time, weight;
    if (wheelchair) {
      time = getEffectiveWalkDistance() / speed;
      weight =
        (getEffectiveBikeDistance() / speed) *
        StreetEdgeReluctanceCalculator.computeWheelchairReluctance(
          preferences,
          getMaxSlope(),
          isWheelchairAccessible(),
          isStairs()
        );
    } else {
      if (walkingBike) {
        // take slopes into account when walking bikes
        time = weight = (getEffectiveBikeDistance() / speed);
        if (isStairs()) {
          // we do allow walking the bike across a stairs but there is a very high default penalty
          weight *= preferences.bike().walking().stairsReluctance();
        }
      } else {
        // take slopes into account when walking
        time = getEffectiveWalkDistance() / speed;
        weight =
          getEffectiveWalkSafetyDistance() * preferences.walk().safetyFactor() +
          getEffectiveWalkDistance() * (1 - preferences.walk().safetyFactor());
        weight /= speed;
      }

      weight *= StreetEdgeReluctanceCalculator.computeReluctance(
        preferences,
        traverseMode,
        walkingBike,
        isStairs()
      );
    }

    return new TraversalCosts(time, weight);
  }

  /* The no-thru traffic support works by not allowing a transition from a no-thru area out of it.
   * It allows starting in a no-thru area by checking for a transition from a "normal"
   * (thru-traffic allowed) edge to a no-thru edge. Once a transition is recorded
   * (State#hasEnteredNoThruTrafficArea), traverseing "normal" edges is blocked.
   *
   * Since a Vertex may be arrived at with and without a no-thru restriction, the logic in
   * DominanceFunction#betterOrEqualAndComparable treats the two cases as separate.
   */
  private boolean isTraversalBlockedByNoThruTraffic(
    TraverseMode traverseMode,
    Edge backEdge,
    State s0,
    StateEditor s1
  ) {
    if (isNoThruTraffic(traverseMode)) {
      // Record transition into no-through-traffic area.
      if (backEdge instanceof StreetEdge sbe && !sbe.isNoThruTraffic(traverseMode)) {
        s1.setEnteredNoThroughTrafficArea();
      }
    } else if (s0.hasEnteredNoThruTrafficArea()) {
      // If we transitioned into a no-through-traffic area at some point, check if we are exiting it.
      return true;
    }

    return false;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  /** Tuple to return time and weight from calculation */
  private record TraversalCosts(double time, double weight) {}

  /**
   * The angles of the first (in) segment and last (out) segment of a LineString, encoded in one
   * byte.
   */
  private record LineStringInOutAngles(byte inAngle, byte outAngle) {
    private static final LineStringInOutAngles DEFAULT = new LineStringInOutAngles(
      (byte) 0,
      (byte) 0
    );

    public static LineStringInOutAngles of(LineString geometry) {
      if (geometry == null) {
        return LineStringInOutAngles.DEFAULT;
      }

      try {
        byte in = convertRadianToByte(DirectionUtils.getFirstAngle(geometry));
        byte out = convertRadianToByte(DirectionUtils.getLastAngle(geometry));
        return new LineStringInOutAngles(in, out);
      } catch (Exception e) {
        LOG.info(
          "Exception while determining LineString angles. setting to zero. There is probably something wrong with this segment's geometry."
        );
        return LineStringInOutAngles.DEFAULT;
      }
    }

    /**
     * Conversion from radians to internal representation as a single signed byte.
     * We also reorient the angles since OTP seems to use South as a reference
     * while the azimuth functions use North.
     * FIXME Use only North as a reference, not a mix of North and South!
     * Range restriction happens automatically due to Java signed overflow behavior.
     * 180 degrees exists as a negative rather than a positive due to the integer range.
     */
    private static byte convertRadianToByte(double angleRadians) {
      return (byte) Math.round((angleRadians * 128) / Math.PI + 128);
    }
  }
}
