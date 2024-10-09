package org.opentripplanner.openstreetmap.wayproperty;

import javax.annotation.Nullable;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 * Builder for {@link WayProperties}. Bicycle and walk safety features are nullable, but they should
 * be set before building the final {@link WayProperties} for a way.
 */
public class WayPropertiesBuilder {

  private StreetTraversalPermission permission;
  private SafetyFeatures bicycleSafetyFeatures = null;
  private SafetyFeatures walkSafetyFeatures = null;

  public WayPropertiesBuilder(StreetTraversalPermission permission) {
    this.permission = permission;
  }

  WayPropertiesBuilder(WayProperties defaultProperties) {
    this.permission = defaultProperties.getPermission();
    this.bicycleSafetyFeatures = defaultProperties.bicycleSafety();
    this.walkSafetyFeatures = defaultProperties.walkSafety();
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

  public WayPropertiesBuilder withPermission(StreetTraversalPermission permission) {
    this.permission = permission;
    return this;
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  @Nullable
  protected SafetyFeatures bicycleSafety() {
    return bicycleSafetyFeatures;
  }

  @Nullable
  protected SafetyFeatures walkSafety() {
    return walkSafetyFeatures;
  }

  public WayProperties build() {
    return new WayProperties(this);
  }

  public static WayPropertiesBuilder withModes(StreetTraversalPermission p) {
    return new WayPropertiesBuilder(p);
  }
}
