package org.opentripplanner.osm.wayproperty;

import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 * Builder for {@link MixinDirectionalProperties}. If you don't set the safety features they will have a default
 * value of 1, which means no change.
 */
public class MixinDirectionalPropertiesBuilder {

  private double walkSafety = 1;
  private double bicycleSafety = 1;
  private StreetTraversalPermission addedPermission = StreetTraversalPermission.NONE;
  private StreetTraversalPermission removedPermission = StreetTraversalPermission.NONE;

  public static MixinDirectionalPropertiesBuilder ofWalkSafety(double safety) {
    return new MixinDirectionalPropertiesBuilder().withWalkSafety(safety);
  }

  public static MixinDirectionalPropertiesBuilder ofBicycleSafety(double safety) {
    return new MixinDirectionalPropertiesBuilder().withBicycleSafety(safety);
  }

  /**
   * Sets the same safety value for normal and back edge.
   * <p>
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public MixinDirectionalPropertiesBuilder withBicycleSafety(double value) {
    this.bicycleSafety = value;
    return this;
  }

  /**
   * Sets the same safety value for normal and back edge.
   * <p>
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public MixinDirectionalPropertiesBuilder withWalkSafety(double walkSafety) {
    this.walkSafety = walkSafety;
    return this;
  }

  public MixinDirectionalPropertiesBuilder addPermission(StreetTraversalPermission permission) {
    removedPermission = removedPermission.remove(permission);
    addedPermission = addedPermission.add(permission);
    return this;
  }

  public MixinDirectionalPropertiesBuilder removePermission(StreetTraversalPermission permission) {
    removedPermission = removedPermission.add(permission);
    addedPermission = addedPermission.remove(permission);
    return this;
  }

  public MixinDirectionalProperties build() {
    return new MixinDirectionalProperties(
      addedPermission,
      removedPermission,
      walkSafety,
      bicycleSafety
    );
  }
}
