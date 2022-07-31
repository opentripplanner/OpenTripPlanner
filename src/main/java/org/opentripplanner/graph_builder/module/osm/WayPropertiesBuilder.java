package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * Builder for {@link WayProperties}. 1.0 is used as the default value for bicycle and walk safety
 * if they are unset.
 */
public class WayPropertiesBuilder {

  private static final P2<Double> defaultSafetyFeatures = new P2<>(1.0, 1.0);
  private final StreetTraversalPermission permission;
  private P2<Double> bicycleSafetyFeatures = defaultSafetyFeatures;
  private P2<Double> walkSafetyFeatures = defaultSafetyFeatures;

  public WayPropertiesBuilder(StreetTraversalPermission permission) {
    this.permission = permission;
  }

  public WayPropertiesBuilder(WayProperties defaultProperties) {
    this.permission = defaultProperties.getPermission();
    this.bicycleSafetyFeatures = defaultProperties.getBicycleSafetyFeatures();
    this.walkSafetyFeatures = defaultProperties.getWalkSafetyFeatures();
  }

  /**
   * Sets the same safety value for normal and back edge.
   *
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public WayPropertiesBuilder bicycleSafety(double bicycleSafety) {
    this.bicycleSafetyFeatures = new P2<>(bicycleSafety, bicycleSafety);
    return this;
  }

  /**
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public WayPropertiesBuilder bicycleSafety(double bicycleSafety, double bicycleSafetyBack) {
    this.bicycleSafetyFeatures = new P2<>(bicycleSafety, bicycleSafetyBack);
    return this;
  }

  /**
   * Sets the same safety value for normal and back edge.
   *
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public WayPropertiesBuilder walkSafety(double walkSafety) {
    this.walkSafetyFeatures = new P2<>(walkSafety, walkSafety);
    return this;
  }

  /**
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public WayPropertiesBuilder walkSafety(double walkSafety, double walkSafetyBack) {
    this.walkSafetyFeatures = new P2<>(walkSafety, walkSafetyBack);
    return this;
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  public P2<Double> getBicycleSafetyFeatures() {
    return bicycleSafetyFeatures;
  }

  public P2<Double> getWalkSafetyFeatures() {
    return walkSafetyFeatures;
  }

  public WayProperties build() {
    return new WayProperties(this);
  }

  public static WayPropertiesBuilder of(StreetTraversalPermission p) {
    return new WayPropertiesBuilder(p);
  }
}
