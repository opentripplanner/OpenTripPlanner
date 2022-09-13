package org.opentripplanner.graph_builder.module.osm;

import java.util.function.Function;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * Builder for {@link WayProperties}. 1.0 is used as the default value for bicycle and walk safety
 * if they are unset.
 */
public class WayPropertiesBuilder {

  private static final SafetyFeatures defaultSafetyFeatures = new SafetyFeatures(1.0, 1.0);
  private final StreetTraversalPermission permission;
  private SafetyFeatures bicycleSafetyFeatures = defaultSafetyFeatures;
  private SafetyFeatures walkSafetyFeatures = defaultSafetyFeatures;
  private boolean hasCustomBicycleSafetyFeatures = false;
  private boolean hasCustomWalkSafetyFeatures = false;

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
    this.bicycleSafetyFeatures = new SafetyFeatures(bicycleSafety, bicycleSafety);
    this.hasCustomBicycleSafetyFeatures = true;
    return this;
  }

  /**
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public WayPropertiesBuilder bicycleSafety(double bicycleSafety, double bicycleSafetyBack) {
    this.bicycleSafetyFeatures = new SafetyFeatures(bicycleSafety, bicycleSafetyBack);
    this.hasCustomBicycleSafetyFeatures = true;
    return this;
  }

  /**
   * Sets the same safety value for normal and back edge.
   *
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public WayPropertiesBuilder walkSafety(double walkSafety) {
    this.walkSafetyFeatures = new SafetyFeatures(walkSafety, walkSafety);
    this.hasCustomWalkSafetyFeatures = true;
    return this;
  }

  /**
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public WayPropertiesBuilder walkSafety(double walkSafety, double walkSafetyBack) {
    this.walkSafetyFeatures = new SafetyFeatures(walkSafety, walkSafetyBack);
    this.hasCustomWalkSafetyFeatures = true;
    return this;
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  public SafetyFeatures getBicycleSafetyFeatures() {
    return bicycleSafetyFeatures;
  }

  public SafetyFeatures getWalkSafetyFeatures() {
    return walkSafetyFeatures;
  }

  public WayProperties build() {
    return new WayProperties(this);
  }

  public WayProperties build(
    Function<StreetTraversalPermission, Double> defaultBicycleSafetyForPermission,
    Function<StreetTraversalPermission, Double> defaultWalkSafetyForPermission
  ) {
    if (!hasCustomBicycleSafetyFeatures) {
      bicycleSafety(defaultBicycleSafetyForPermission.apply(permission));
    }
    if (!hasCustomWalkSafetyFeatures) {
      walkSafety(defaultWalkSafetyForPermission.apply(permission));
    }
    return new WayProperties(this);
  }

  public static WayPropertiesBuilder withModes(StreetTraversalPermission p) {
    return new WayPropertiesBuilder(p);
  }
}
