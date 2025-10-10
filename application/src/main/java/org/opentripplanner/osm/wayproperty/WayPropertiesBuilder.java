package org.opentripplanner.osm.wayproperty;

import javax.annotation.Nullable;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 * Builder for {@link WayProperties}. Bicycle and walk safety features are nullable, but they should
 * be set before building the final {@link WayProperties} for a way.
 */
public class WayPropertiesBuilder {

  private StreetTraversalPermission permission;
  private Double bicycleSafetyFeatures = null;
  private Double walkSafetyFeatures = null;

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
   */
  public WayPropertiesBuilder bicycleSafety(double bicycleSafety) {
    this.bicycleSafetyFeatures = bicycleSafety;
    return this;
  }

  /**
   * Sets the same safety value for normal and back edge.
   */
  public WayPropertiesBuilder walkSafety(double walkSafety) {
    this.walkSafetyFeatures = walkSafety;
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
  protected Double bicycleSafety() {
    return bicycleSafetyFeatures;
  }

  @Nullable
  protected Double walkSafety() {
    return walkSafetyFeatures;
  }

  public WayProperties build() {
    return new WayProperties(this);
  }

  public static WayPropertiesBuilder withModes(StreetTraversalPermission p) {
    return new WayPropertiesBuilder(p);
  }
}
