package org.opentripplanner.graph_builder.module.osm;

import java.util.function.Function;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * Builder for {@link WayProperties}. Default value for bicycle and walk safety is set when
 * {@link WayProperties} is built.
 */
public class WayPropertiesBuilder {

  private final StreetTraversalPermission permission;
  private SafetyFeatures bicycleSafetyFeatures = null;
  private SafetyFeatures walkSafetyFeatures = null;

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
    return this;
  }

  /**
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public WayPropertiesBuilder bicycleSafety(double bicycleSafety, double bicycleSafetyBack) {
    this.bicycleSafetyFeatures = new SafetyFeatures(bicycleSafety, bicycleSafetyBack);
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
    return this;
  }

  /**
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public WayPropertiesBuilder walkSafety(double walkSafety, double walkSafetyBack) {
    this.walkSafetyFeatures = new SafetyFeatures(walkSafety, walkSafetyBack);
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

  public WayProperties build(
    Function<StreetTraversalPermission, Double> defaultBicycleSafetyForPermission,
    Function<StreetTraversalPermission, Double> defaultWalkSafetyForPermission
  ) {
    if (bicycleSafetyFeatures == null) {
      bicycleSafety(defaultBicycleSafetyForPermission.apply(permission));
    }
    if (walkSafetyFeatures == null) {
      walkSafety(defaultWalkSafetyForPermission.apply(permission));
    }
    return new WayProperties(this);
  }

  public static WayPropertiesBuilder withModes(StreetTraversalPermission p) {
    return new WayPropertiesBuilder(p);
  }
}
