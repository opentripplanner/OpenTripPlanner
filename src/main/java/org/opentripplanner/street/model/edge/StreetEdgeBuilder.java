package org.opentripplanner.street.model.edge;

import static org.opentripplanner.street.model.edge.StreetEdge.BACK_FLAG_INDEX;
import static org.opentripplanner.street.model.edge.StreetEdge.BICYCLE_NOTHRUTRAFFIC;
import static org.opentripplanner.street.model.edge.StreetEdge.CLASS_LINK;
import static org.opentripplanner.street.model.edge.StreetEdge.HASBOGUSNAME_FLAG_INDEX;
import static org.opentripplanner.street.model.edge.StreetEdge.MOTOR_VEHICLE_NOTHRUTRAFFIC;
import static org.opentripplanner.street.model.edge.StreetEdge.ROUNDABOUT_FLAG_INDEX;
import static org.opentripplanner.street.model.edge.StreetEdge.SLOPEOVERRIDE_FLAG_INDEX;
import static org.opentripplanner.street.model.edge.StreetEdge.STAIRS_FLAG_INDEX;
import static org.opentripplanner.street.model.edge.StreetEdge.WALK_NOTHRUTRAFFIC;
import static org.opentripplanner.street.model.edge.StreetEdge.WHEELCHAIR_ACCESSIBLE_FLAG_INDEX;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.lang.BitSetUtils;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.TraverseMode;

public class StreetEdgeBuilder<B extends StreetEdgeBuilder<B>> {

  // TODO(flamholz): do something smarter with the car speed here.
  public static final float DEFAULT_CAR_SPEED = 11.2f;
  public static final float DEFAULT_WALK_SAFETY_FACTOR = 1.0f;
  private static final float DEFAULT_BICYCLE_SAFETY_FACTOR = 1.0f;

  private StreetVertex from;
  private StreetVertex to;
  private LineString geometry;
  private I18NString name;
  private int millimeterLength;
  private StreetTraversalPermission permission;
  private boolean defaultLength;
  private float carSpeed;
  private float walkSafetyFactor;
  private float bicycleSafetyFactor;
  private short flags;
  private StreetElevationExtension streetElevationExtension;

  public StreetEdgeBuilder() {
    this.defaultLength = true;
    this.walkSafetyFactor = DEFAULT_WALK_SAFETY_FACTOR;
    this.bicycleSafetyFactor = DEFAULT_BICYCLE_SAFETY_FACTOR;
    this.carSpeed = DEFAULT_CAR_SPEED;
    withWheelchairAccessible(true);
  }

  public StreetEdgeBuilder(StreetEdge original) {
    this.from = (StreetVertex) original.getFromVertex();
    this.to = (StreetVertex) original.getToVertex();
    this.geometry = original.getGeometry();
    this.name = original.getName();
    this.millimeterLength = original.getMillimeterLength();
    this.permission = original.getPermission();
    this.defaultLength = false;
    this.carSpeed = original.getCarSpeed();
    this.walkSafetyFactor = original.getWalkSafetyFactor();
    this.bicycleSafetyFactor = original.getBicycleSafetyFactor();
    this.flags = original.getFlags();
  }

  public StreetEdge buildAndConnect() {
    return Edge.connectToGraph(new StreetEdge(this));
  }

  public StreetVertex fromVertex() {
    return from;
  }

  public B withFromVertex(StreetVertex from) {
    this.from = from;
    return instance();
  }

  public StreetVertex toVertex() {
    return to;
  }

  public B withToVertex(StreetVertex to) {
    this.to = to;
    return instance();
  }

  public LineString geometry() {
    return geometry;
  }

  public B withGeometry(LineString geometry) {
    this.geometry = geometry;
    return instance();
  }

  public I18NString name() {
    return name;
  }

  public B withName(I18NString name) {
    this.name = name;
    return instance();
  }

  public B withName(String name) {
    this.name = new NonLocalizedString(name);
    return instance();
  }

  public int millimeterLength() {
    return millimeterLength;
  }

  public B withMeterLength(double length) {
    return withMilliMeterLength((int) (length * 1000));
  }

  public B withMilliMeterLength(int length) {
    this.millimeterLength = length;
    this.defaultLength = false;
    return instance();
  }

  public boolean hasDefaultLength() {
    return defaultLength;
  }

  public StreetTraversalPermission permission() {
    return permission;
  }

  public B withPermission(StreetTraversalPermission permission) {
    this.permission = permission;
    return instance();
  }

  public float carSpeed() {
    return carSpeed;
  }

  public B withCarSpeed(float carSpeed) {
    this.carSpeed = carSpeed;
    return instance();
  }

  public float walkSafetyFactor() {
    return walkSafetyFactor;
  }

  public B withWalkSafetyFactor(float walkSafetyFactor) {
    this.walkSafetyFactor = walkSafetyFactor;
    return instance();
  }

  public float bicycleSafetyFactor() {
    return bicycleSafetyFactor;
  }

  public B withBicycleSafetyFactor(float bicycleSafetyFactor) {
    this.bicycleSafetyFactor = bicycleSafetyFactor;
    return instance();
  }

  public short getFlags() {
    return flags;
  }

  public B withBack(boolean back) {
    flags = BitSetUtils.set(flags, BACK_FLAG_INDEX, back);
    return instance();
  }

  public B withLink(boolean link) {
    flags = BitSetUtils.set(flags, CLASS_LINK, link);
    return instance();
  }

  public B withBogusName(boolean hasBogusName) {
    flags = BitSetUtils.set(flags, HASBOGUSNAME_FLAG_INDEX, hasBogusName);
    return instance();
  }

  public B withStairs(boolean stairs) {
    flags = BitSetUtils.set(flags, STAIRS_FLAG_INDEX, stairs);
    return instance();
  }

  public B withWheelchairAccessible(boolean wheelchairAccessible) {
    flags = BitSetUtils.set(flags, WHEELCHAIR_ACCESSIBLE_FLAG_INDEX, wheelchairAccessible);
    return instance();
  }

  public B withSlopeOverride(boolean slopeOverride) {
    flags = BitSetUtils.set(flags, SLOPEOVERRIDE_FLAG_INDEX, slopeOverride);
    return instance();
  }

  public B withRoundabout(boolean roundabout) {
    flags = BitSetUtils.set(flags, ROUNDABOUT_FLAG_INDEX, roundabout);
    return instance();
  }

  public B withMotorVehicleNoThruTraffic(boolean motorVehicleNoThruTraffic) {
    flags = BitSetUtils.set(flags, MOTOR_VEHICLE_NOTHRUTRAFFIC, motorVehicleNoThruTraffic);
    return instance();
  }

  public B withBicycleNoThruTraffic(boolean bicycleNoThruTraffic) {
    flags = BitSetUtils.set(flags, BICYCLE_NOTHRUTRAFFIC, bicycleNoThruTraffic);
    return instance();
  }

  public B withWalkNoThruTraffic(boolean walkNoThruTraffic) {
    flags = BitSetUtils.set(flags, WALK_NOTHRUTRAFFIC, walkNoThruTraffic);
    return instance();
  }

  public B withNoThruTrafficTraverseMode(TraverseMode noThruTrafficTraverseMode) {
    if (noThruTrafficTraverseMode == null) {
      return instance();
    }
    switch (noThruTrafficTraverseMode) {
      case WALK -> withWalkNoThruTraffic(true);
      case BICYCLE, SCOOTER -> withBicycleNoThruTraffic(true);
      case CAR, FLEX -> withMotorVehicleNoThruTraffic(true);
    }
    return instance();
  }

  public boolean slopeOverride() {
    return BitSetUtils.get(flags, SLOPEOVERRIDE_FLAG_INDEX);
  }

  public boolean stairs() {
    return BitSetUtils.get(flags, STAIRS_FLAG_INDEX);
  }

  public B withFlags(short flags) {
    this.flags = flags;
    return instance();
  }

  public B withElevationExtension(StreetElevationExtension streetElevationExtension) {
    this.streetElevationExtension = streetElevationExtension;
    return instance();
  }

  public StreetElevationExtension streetElevationExtension() {
    return streetElevationExtension;
  }

  @SuppressWarnings("unchecked")
  final B instance() {
    return (B) this;
  }
}
