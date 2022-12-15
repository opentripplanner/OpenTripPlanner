package org.opentripplanner.street.model.edge;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.framework.geometry.CompactLineStringUtils;
import org.opentripplanner.framework.geometry.DirectionUtils;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.SplitLineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.lang.BitSetUtils;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.LinkingDirection;
import org.opentripplanner.routing.util.ElevationUtils;
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

  private static final double GREENWAY_SAFETY_FACTOR = 0.1;
  // TODO(flamholz): do something smarter with the car speed here.
  public static final float DEFAULT_CAR_SPEED = 11.2f;
  /** If you have more than 16 flags, increase flags to short or int */
  private static final int BACK_FLAG_INDEX = 0;
  private static final int ROUNDABOUT_FLAG_INDEX = 1;
  private static final int HASBOGUSNAME_FLAG_INDEX = 2;
  private static final int MOTOR_VEHICLE_NOTHRUTRAFFIC = 3;
  private static final int STAIRS_FLAG_INDEX = 4;
  private static final int SLOPEOVERRIDE_FLAG_INDEX = 5;
  private static final int WHEELCHAIR_ACCESSIBLE_FLAG_INDEX = 6;
  private static final int BICYCLE_NOTHRUTRAFFIC = 7;
  private static final int WALK_NOTHRUTRAFFIC = 8;
  private static final int CLASS_LINK = 9;
  private StreetEdgeCostExtension costExtension;
  /** back, roundabout, stairs, ... */
  private short flags;

  /**
   * Length is stored internally as 32-bit fixed-point (millimeters). This allows edges of up to
   * ~2100km. Distances used in calculations and exposed outside this class are still in
   * double-precision floating point meters. Someday we might want to convert everything to fixed
   * point representations.
   */
  private int length_mm;

  /**
   * bicycleSafetyWeight = length * bicycleSafetyFactor. For example, a 100m street with a safety
   * factor of 2.0 will be considered in term of safety cost as the same as a 200m street with a
   * safety factor of 1.0.
   */
  protected float bicycleSafetyFactor;

  /**
   * walkSafetyFactor = length * walkSafetyFactor. For example, a 100m street with a safety
   * factor of 2.0 will be considered in term of safety cost as the same as a 200m street with a
   * safety factor of 1.0.
   */
  protected float walkSafetyFactor;

  private byte[] compactGeometry;

  private I18NString name;

  private StreetTraversalPermission permission;

  /**
   * The speed (meters / sec) at which an automobile can traverse this street segment.
   */
  private float carSpeed;

  /**
   * The angle at the start of the edge geometry. Internal representation is -180 to +179 integer
   * degrees mapped to -128 to +127 (brads)
   */
  private byte inAngle;

  /** The angle at the start of the edge geometry. Internal representation like that of inAngle. */
  private byte outAngle;

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

  public StreetEdge(
    StreetVertex v1,
    StreetVertex v2,
    LineString geometry,
    I18NString name,
    double length,
    StreetTraversalPermission permission,
    boolean back
  ) {
    super(v1, v2);
    this.setBack(back);
    this.setGeometry(geometry);
    this.length_mm = (int) (length * 1000); // CONVERT FROM FLOAT METERS TO FIXED MILLIMETERS
    if (this.length_mm == 0) {
      LOG.warn(
        "StreetEdge {} from {} to {} has length of 0. This is usually an error.",
        name,
        v1,
        v2
      );
    }
    this.bicycleSafetyFactor = 1.0f;
    this.walkSafetyFactor = 1.0f;
    this.name = name;
    this.setPermission(permission);
    this.setCarSpeed(DEFAULT_CAR_SPEED);
    this.setWheelchairAccessible(true); // accessible by default
    if (geometry != null) {
      try {
        for (Coordinate c : geometry.getCoordinates()) {
          if (Double.isNaN(c.x)) {
            System.out.println("X DOOM");
          }
          if (Double.isNaN(c.y)) {
            System.out.println("Y DOOM");
          }
        }
        // Conversion from radians to internal representation as a single signed byte.
        // We also reorient the angles since OTP seems to use South as a reference
        // while the azimuth functions use North.
        // FIXME Use only North as a reference, not a mix of North and South!
        // Range restriction happens automatically due to Java signed overflow behavior.
        // 180 degrees exists as a negative rather than a positive due to the integer range.
        double angleRadians = DirectionUtils.getLastAngle(geometry);
        outAngle = (byte) Math.round(angleRadians * 128 / Math.PI + 128);
        angleRadians = DirectionUtils.getFirstAngle(geometry);
        inAngle = (byte) Math.round(angleRadians * 128 / Math.PI + 128);
      } catch (IllegalArgumentException iae) {
        LOG.error(
          "exception while determining street edge angles. setting to zero. there is probably something wrong with this street segment's geometry."
        );
        inAngle = 0;
        outAngle = 0;
      }
    }
  }

  //For testing only
  public StreetEdge(
    StreetVertex v1,
    StreetVertex v2,
    LineString geometry,
    String name,
    double length,
    StreetTraversalPermission permission,
    boolean back
  ) {
    this(v1, v2, geometry, new NonLocalizedString(name), length, permission, back);
  }

  public StreetEdge(
    StreetVertex v1,
    StreetVertex v2,
    LineString geometry,
    I18NString name,
    StreetTraversalPermission permission,
    boolean back
  ) {
    this(v1, v2, geometry, name, SphericalDistanceLibrary.length(geometry), permission, back);
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
        case WALK -> walkingBike ? preferences.bike().walkingSpeed() : preferences.walk().speed();
        case BICYCLE -> preferences.bike().speed();
        case CAR -> getCarSpeed();
        default -> throw new IllegalArgumentException("getSpeed(): Invalid mode " + traverseMode);
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
    return (
      "StreetEdge(" +
      name +
      ", " +
      fromv +
      " -> " +
      tov +
      " length=" +
      this.getDistanceMeters() +
      " carSpeed=" +
      this.getCarSpeed() +
      " permission=" +
      this.getPermission() +
      ")"
    );
  }

  public boolean isRoundabout() {
    return BitSetUtils.get(flags, ROUNDABOUT_FLAG_INDEX);
  }

  @Override
  public State traverse(State s0) {
    final StateEditor editor;

    // If we are biking, or walking with a bike check if we may continue by biking or by walking
    if (s0.getNonTransitMode() == TraverseMode.BICYCLE) {
      if (canTraverse(TraverseMode.BICYCLE)) {
        editor = doTraverse(s0, TraverseMode.BICYCLE, false);
      } else if (canTraverse(TraverseMode.WALK)) {
        editor = doTraverse(s0, TraverseMode.WALK, true);
      } else {
        return null;
      }
    } else if (canTraverse(s0.getNonTransitMode())) {
      editor = doTraverse(s0, s0.getNonTransitMode(), false);
    } else {
      editor = null;
    }

    State state = editor != null ? editor.makeState() : null;

    if (canPickupAndDrive(s0) && canTraverse(TraverseMode.CAR)) {
      StateEditor inCar = doTraverse(s0, TraverseMode.CAR, false);
      if (inCar != null) {
        driveAfterPickup(s0, inCar);
        State forkState = inCar.makeState();
        if (forkState != null) {
          // Return both the original WALK state, along with the new IN_CAR state
          forkState.addToExistingResultChain(state);
          return forkState;
        }
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
        return dropOff.makeState();
      }
    }

    return state;
  }

  /**
   * Gets non-localized I18NString (Used when splitting edges)
   *
   * @return non-localized Name
   */
  public I18NString getName() {
    return this.name;
  }

  public void setName(I18NString name) {
    this.name = name;
  }

  public boolean hasBogusName() {
    return BitSetUtils.get(flags, HASBOGUSNAME_FLAG_INDEX);
  }

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

  private void setGeometry(LineString geometry) {
    this.compactGeometry =
      CompactLineStringUtils.compactLineString(
        fromv.getLon(),
        fromv.getLat(),
        tov.getLon(),
        tov.getLat(),
        isBack() ? geometry.reverse() : geometry,
        isBack()
      );
  }

  public void setRoundabout(boolean roundabout) {
    flags = BitSetUtils.set(flags, ROUNDABOUT_FLAG_INDEX, roundabout);
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

  public void setWheelchairAccessible(boolean wheelchairAccessible) {
    flags = BitSetUtils.set(flags, WHEELCHAIR_ACCESSIBLE_FLAG_INDEX, wheelchairAccessible);
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  public void setPermission(StreetTraversalPermission permission) {
    this.permission = permission;
  }

  /**
   * Marks that this edge is the reverse of the one defined in the source data. Does NOT mean
   * fromv/tov are reversed.
   */
  public boolean isBack() {
    return BitSetUtils.get(flags, BACK_FLAG_INDEX);
  }

  public void setBack(boolean back) {
    flags = BitSetUtils.set(flags, BACK_FLAG_INDEX, back);
  }

  public void setHasBogusName(boolean hasBogusName) {
    flags = BitSetUtils.set(flags, HASBOGUSNAME_FLAG_INDEX, hasBogusName);
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

  public void setStairs(boolean stairs) {
    flags = BitSetUtils.set(flags, STAIRS_FLAG_INDEX, stairs);
  }

  /**
   * The edge is part of an osm way, which is of type link
   */
  public boolean isLink() {
    return BitSetUtils.get(flags, CLASS_LINK);
  }

  public void setLink(boolean link) {
    flags = BitSetUtils.set(flags, CLASS_LINK, link);
  }

  public float getCarSpeed() {
    return carSpeed;
  }

  public void setCarSpeed(float carSpeed) {
    this.carSpeed = carSpeed;
  }

  public boolean isSlopeOverride() {
    return BitSetUtils.get(flags, SLOPEOVERRIDE_FLAG_INDEX);
  }

  public void setSlopeOverride(boolean slopeOverride) {
    flags = BitSetUtils.set(flags, SLOPEOVERRIDE_FLAG_INDEX, slopeOverride);
  }

  /**
   * Return the azimuth of the first segment in this edge in integer degrees clockwise from South.
   * TODO change everything to clockwise from North
   */
  public int getInAngle() {
    return (int) Math.round(this.inAngle * 180 / 128.0);
  }

  /** Return the azimuth of the last segment in this edge in integer degrees clockwise from South. */
  public int getOutAngle() {
    return (int) Math.round(this.outAngle * 180 / 128.0);
  }

  public void setCostExtension(StreetEdgeCostExtension costExtension) {
    this.costExtension = costExtension;
  }

  /**
   * Split this street edge and return the resulting street edges. After splitting, the original
   * edge will be removed from the graph.
   */
  public SplitStreetEdge splitDestructively(SplitterVertex v) {
    SplitLineString geoms = GeometryUtils.splitGeometryAtPoint(getGeometry(), v.getCoordinate());

    StreetEdge e1 = new StreetEdge(
      (StreetVertex) fromv,
      v,
      geoms.beginning(),
      name,
      permission,
      this.isBack()
    );
    StreetEdge e2 = new StreetEdge(
      v,
      (StreetVertex) tov,
      geoms.ending(),
      name,
      permission,
      this.isBack()
    );

    // we have this code implemented in both directions, because splits are fudged half a millimeter
    // when the length of this is odd. We want to make sure the lengths of the split streets end up
    // exactly the same as their backStreets so that if they are split again the error does not accumulate
    // and so that the order in which they are split does not matter.
    if (!isBack()) {
      // cast before the divide so that the sum is promoted
      double frac = (double) e1.length_mm / (e1.length_mm + e2.length_mm);
      e1.length_mm = (int) (length_mm * frac);
      e2.length_mm = length_mm - e1.length_mm;
    } else {
      // cast before the divide so that the sum is promoted
      double frac = (double) e2.length_mm / (e1.length_mm + e2.length_mm);
      e2.length_mm = (int) (length_mm * frac);
      e1.length_mm = length_mm - e2.length_mm;
    }

    // TODO: better handle this temporary fix to handle bad edge distance calculation
    if (e1.length_mm <= 0) {
      LOG.error(
        "Edge 1 ({}) split at vertex at {},{} has length {} mm. Setting to 1 mm.",
        e1.getName(),
        v.getLat(),
        v.getLon(),
        e1.length_mm
      );
      e1.length_mm = 1;
    }
    if (e2.length_mm <= 0) {
      LOG.error(
        "Edge 2 ({}) split at vertex at {},{}  has length {} mm. Setting to 1 mm.",
        e2.getName(),
        v.getLat(),
        v.getLon(),
        e2.length_mm
      );
      e2.length_mm = 1;
    }

    if (e1.length_mm < 0 || e2.length_mm < 0) {
      e1.tov.removeIncoming(e1);
      e1.fromv.removeOutgoing(e1);
      e2.tov.removeIncoming(e2);
      e2.fromv.removeOutgoing(e2);
      throw new IllegalStateException("Split street is longer than original street!");
    }

    copyPropertiesToSplitEdge(e1, 0, e1.getDistanceMeters());
    copyPropertiesToSplitEdge(e2, e1.getDistanceMeters(), getDistanceMeters());

    var splitEdges = new SplitStreetEdge(e1, e2);
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

    if (direction == LinkingDirection.OUTGOING || direction == LinkingDirection.BOTH_WAYS) {
      e1 =
        new TemporaryPartialStreetEdge(
          this,
          (StreetVertex) fromv,
          v,
          geoms.beginning(),
          name,
          this.isBack()
        );
      copyPropertiesToSplitEdge(e1, 0, e1.getDistanceMeters());
      tempEdges.addEdge(e1);
    }
    if (direction == LinkingDirection.INCOMING || direction == LinkingDirection.BOTH_WAYS) {
      e2 =
        new TemporaryPartialStreetEdge(
          this,
          v,
          (StreetVertex) tov,
          geoms.ending(),
          name,
          this.isBack()
        );
      copyPropertiesToSplitEdge(
        e2,
        getDistanceMeters() - e2.getDistanceMeters(),
        getDistanceMeters()
      );
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

      var tempEdge = new TemporaryPartialStreetEdge(this, from, to, partial, getName(), length);
      copyPropertiesToSplitEdge(tempEdge, start, start + length);
      return Optional.of(tempEdge);
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
  @Nonnull
  public List<TurnRestriction> getTurnRestrictions() {
    // this can be safely returned as it's unmodifiable
    return turnRestrictions;
  }

  @Override
  public void remove() {
    removeAllTurnRestrictions();

    super.remove();
  }

  protected void copyPropertiesToSplitEdge(
    StreetEdge splitEdge,
    double fromDistance,
    double toDistance
  ) {
    splitEdge.flags = this.flags;
    splitEdge.setBicycleSafetyFactor(bicycleSafetyFactor);
    splitEdge.setWalkSafetyFactor(walkSafetyFactor);
    splitEdge.setLink(isLink());
    splitEdge.setCarSpeed(getCarSpeed());
    splitEdge.setElevationExtensionUsingParent(this, fromDistance, toDistance);
  }

  protected void setElevationExtensionUsingParent(
    StreetEdge parentEdge,
    double fromDistance,
    double toDistance
  ) {
    var profile = ElevationUtils.getPartialElevationProfile(
      parentEdge.getElevationProfile(),
      fromDistance,
      toDistance
    );
    StreetElevationExtension.addToEdge(this, profile, true);
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

    final RoutingPreferences preferences = s0.getPreferences();

    // Automobiles have variable speeds depending on the edge type
    double speed = calculateSpeed(preferences, traverseMode, walkingBike);

    var traversalCosts =
      switch (traverseMode) {
        case BICYCLE, SCOOTER -> bicycleTraversalCost(preferences, speed);
        case WALK -> walkingTraversalCosts(
          preferences,
          traverseMode,
          speed,
          walkingBike,
          s0.getRequest().wheelchair()
        );
        default -> otherTraversalCosts(preferences, traverseMode, walkingBike, speed);
      };

    int time = (int) Math.ceil(traversalCosts.time());
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
        turnDuration =
          s0
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
        turnDuration =
          s0
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

      if (!traverseMode.isDriving()) {
        s1.incrementWalkDistance(turnDuration / 100); // just a tie-breaker
      }

      time += (int) Math.ceil(turnDuration);
      weight += preferences.street().turnReluctance() * turnDuration;
    }

    if (!traverseMode.isDriving()) {
      s1.incrementWalkDistance(getDistanceWithElevation());
    }

    if (costExtension != null) {
      weight += costExtension.calculateExtraCost(s0, length_mm, traverseMode);
    }

    s1.incrementTimeInSeconds(time);

    s1.incrementWeight(weight);

    return s1;
  }

  @Nonnull
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

  @Nonnull
  private TraversalCosts bicycleTraversalCost(RoutingPreferences pref, double speed) {
    double time = getEffectiveBikeDistance() / speed;
    double weight;
    switch (pref.bike().optimizeType()) {
      case GREENWAYS -> {
        weight = bicycleSafetyFactor * getDistanceMeters() / speed;
        if (bicycleSafetyFactor <= GREENWAY_SAFETY_FACTOR) {
          // greenways are treated as even safer than they really are
          weight *= 0.66;
        }
      }
      case SAFE -> weight = getEffectiveBicycleSafetyDistance() / speed;
      case FLAT -> /* see notes in StreetVertex on speed overhead */weight =
        getEffectiveBikeDistanceForWorkCost() / speed;
      case QUICK -> weight = getEffectiveBikeDistance() / speed;
      case TRIANGLE -> {
        double quick = getEffectiveBikeDistance();
        double safety = getEffectiveBicycleSafetyDistance();
        double slope = getEffectiveBikeDistanceForWorkCost();
        weight =
          quick *
          pref.bike().optimizeTriangle().time() +
          slope *
          pref.bike().optimizeTriangle().slope() +
          safety *
          pref.bike().optimizeTriangle().safety();
        weight /= speed;
      }
      default -> weight = getDistanceMeters() / speed;
    }
    var reluctance = StreetEdgeReluctanceCalculator.computeReluctance(
      pref,
      TraverseMode.BICYCLE,
      false,
      isStairs()
    );
    weight *= reluctance;
    return new TraversalCosts(time, weight);
  }

  @Nonnull
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
          weight *= preferences.bike().stairsReluctance();
        }
      } else {
        // take slopes into account when walking
        time = getEffectiveWalkDistance() / speed;
        weight =
          getEffectiveWalkSafetyDistance() *
          preferences.walk().safetyFactor() +
          getEffectiveWalkDistance() *
          (1 - preferences.walk().safetyFactor());
        weight /= speed;
      }
      weight *=
        StreetEdgeReluctanceCalculator.computeReluctance(
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
}
